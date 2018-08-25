package org.soraworld.authme;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.soraworld.authme.command.CommandAuthme;
import org.soraworld.authme.commands.*;
import org.soraworld.authme.config.Settings;
import org.soraworld.authme.constant.Constant;
import org.soraworld.authme.hasher.BcryptHasher;
import org.soraworld.authme.hasher.Hasher;
import org.soraworld.authme.hasher.TOTP;
import org.soraworld.authme.listener.ConnectionListener;
import org.soraworld.authme.listener.PreventListener;
import org.soraworld.authme.manager.AuthmeManager;
import org.soraworld.violet.command.SpongeCommand;
import org.soraworld.violet.manager.SpongeManager;
import org.soraworld.violet.plugin.SpongePlugin;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Plugin(
        id = Constant.MODID,
        name = Constant.NAME,
        version = Constant.VERSION
)
public class Authme extends SpongePlugin {

    private final Game game;
    @Inject
    private final Logger logger;
    @Inject
    private final PluginContainer plugin;
    private static Authme instance;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private Path cfgFile;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> cfgLoader;

    private Map<String, Integer> attempts = Maps.newConcurrentMap();

    private Settings configuration;
    private Database database;
    private final ProtectionManager protectionManager;

    private Hasher hasher;

    @Inject
    public Authme(Logger logger, PluginContainer plugin, Game game) {
        instance = this;

        this.logger = logger;
        this.plugin = plugin;
        this.game = game;

        this.protectionManager = new ProtectionManager();
    }

    public static Authme getInstance() {
        return instance;
    }

    @Listener
    public void onPreInit(GamePreInitializationEvent event) {
        configuration = new Settings(cfgLoader, cfgFile);
        configuration.load();

        database = new Database();
        database.createTable();

        if ("totp".equalsIgnoreCase(configuration.config().getHashAlgo())) {
            hasher = new TOTP();
        } else {
            //use bcrypt as fallback for now
            hasher = new BcryptHasher();
        }
    }

    @Listener //Commands register + events
    public void onInit(GameInitializationEvent initEvent) {
        //register commands
        CommandManager commandDispatcher = game.getCommandManager();

        commandDispatcher.register(this, CommandSpec.builder()
                .executor(new LoginCommand())
                .arguments(GenericArguments.onlyOne(GenericArguments.string(Text.of("password"))))
                .build(), "login", "log");

        commandDispatcher.register(this, CommandSpec.builder()
                .executor(new RegisterCommand())
                .arguments(GenericArguments
                        .optional(GenericArguments
                                .repeated(GenericArguments
                                        .string(Text.of("password")), 2)))
                .build(), "register", "reg");

        commandDispatcher.register(this, CommandSpec.builder()
                .executor(new ChangePasswordCommand())
                .arguments(GenericArguments
                        .repeated(GenericArguments
                                .string(Text.of("password")), 2))
                .build(), "changepassword", "changepw");

        commandDispatcher.register(this, CommandSpec.builder()
                .executor(new SetEmailCommand())
                .arguments(GenericArguments.onlyOne(GenericArguments.string(Text.of("email"))))
                .build(), "setemail", "email");

        commandDispatcher.register(this, CommandSpec.builder()
                .executor(new ForgotPasswordCommand())
                .build(), "forgotpassword", "forgot");

        commandDispatcher.register(this, CommandSpec.builder()
                .executor(new LogoutCommand())
                .build(), "logout");

        //admin commands
        commandDispatcher.register(this, CommandSpec.builder()
                .permission(plugin.getName() + ".admin")
                .child(CommandSpec.builder()
                        .executor(new ReloadCommand())
                        .build(), "reload", "rl")
                .child(CommandSpec.builder()
                        .executor(new UnregisterCommand())
                        .arguments(GenericArguments.onlyOne(GenericArguments.string(Text.of("account"))))
                        .build(), "unregister", "unreg")
                .child(CommandSpec.builder()
                        .executor(new ForceRegisterCommand())
                        .arguments(
                                GenericArguments.onlyOne(GenericArguments
                                        .string(Text.of("account"))), GenericArguments.string(Text.of("password")))
                        .build(), "register", "reg")
                .child(CommandSpec.builder()
                        .executor(new ResetPasswordCommand())
                        .arguments(
                                GenericArguments.onlyOne(GenericArguments
                                        .string(Text.of("account"))), GenericArguments.string(Text.of("password")))
                        .build(), "resetpw", "resetpassword")
                .build(), plugin.getName());

        //register events
        game.getEventManager().registerListeners(this, new ConnectionListener());
        game.getEventManager().registerListeners(this, new PreventListener());
    }

    @Nonnull
    protected SpongeManager registerManager(Path path) {
        return new AuthmeManager(this, path);
    }

    @Nonnull
    protected SpongeCommand registerCommand() {
        return new CommandAuthme(null, false, manager, "authme");
    }

    @Nullable
    protected List<Object> registerListeners() {
        return null;
    }

    @Listener
    public void onDisable(GameStoppedServerEvent gameStoppedEvent) {
        //run this task sync in order let it finish before the process ends
        database.close();

        game.getServer().getOnlinePlayers().forEach(protectionManager::unprotect);
    }

    public void onReload() {
        //run this task sync in order let it finish before the process ends
        database.close();

        game.getServer().getOnlinePlayers().forEach(protectionManager::unprotect);

        configuration.load();
        database = new Database();
        database.createTable();

        if ("totp".equalsIgnoreCase(configuration.config().getHashAlgo())) {
            hasher = new TOTP();
        } else {
            //use bcrypt as fallback for now
            hasher = new BcryptHasher();
        }

        game.getServer().getOnlinePlayers().forEach(protectionManager::protect);
        game.getServer().getOnlinePlayers().forEach(database::loadAccount);
    }

    public Settings loader() {
        return configuration;
    }

    public PluginContainer plugin() {
        return plugin;
    }

    public Logger getLogger() {
        return logger;
    }

    public Game getGame() {
        return game;
    }

    public Database getDatabase() {
        return database;
    }

    public Map<String, Integer> getAttempts() {
        return attempts;
    }

    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }

    public Hasher getHasher() {
        //this is thread-safe because it won't change after config load
        return hasher;
    }

    @Nonnull
    public String assetsId() {
        return getId();
    }
}
