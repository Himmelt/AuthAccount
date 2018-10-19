package org.soraworld.account;

import org.soraworld.account.command.CommandAccount;
import org.soraworld.account.command.CommandLogin;
import org.soraworld.account.data.Account;
import org.soraworld.account.listener.EventListener;
import org.soraworld.account.manager.AccountManager;
import org.soraworld.violet.Violet;
import org.soraworld.violet.command.SpongeBaseSubs;
import org.soraworld.violet.command.SpongeCommand;
import org.soraworld.violet.manager.SpongeManager;
import org.soraworld.violet.plugin.SpongePlugin;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.user.UserStorageService;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Plugin(
        id = AuthAccount.PLUGIN_ID,
        name = AuthAccount.PLUGIN_NAME,
        version = AuthAccount.PLUGIN_VERSION,
        description = "AuthAccount Plugin for sponge.",
        dependencies = {
                @Dependency(
                        id = Violet.PLUGIN_ID,
                        version = Violet.PLUGIN_VERSION
                )
        }
)
public class AuthAccount extends SpongePlugin {

    public static final String PLUGIN_ID = "authaccount";
    public static final String PLUGIN_NAME = "AuthAccount";
    public static final String PLUGIN_VERSION = "1.0.0";

    @Listener
    public void onInit(GameInitializationEvent event) {
        super.onInit(event);
        DataRegistration.builder()
                .dataClass(Account.class)
                .immutableClass(Account.Immutable.class)
                .builder(new Account.Builder())
                .dataName("Account Data")
                .manipulatorId("account")
                .buildAndRegister(container);
    }

    protected SpongeManager registerManager(Path path) {
        return new AccountManager(this, path);
    }

    protected List<Object> registerListeners() {
        UserStorageService service = Sponge.getServiceManager().provide(UserStorageService.class).orElse(null);
        if (service != null) return Collections.singletonList(new EventListener((AccountManager) manager, service));
        else {
            manager.consoleKey("invalidStorageService");
            return Collections.emptyList();
        }
    }

    protected void registerCommands() {
        SpongeCommand command = new SpongeCommand(getId(), null, false, manager, "auth", "account", "acc");
        command.extractSub(SpongeBaseSubs.class);
        command.extractSub(CommandAccount.class);
        register(this, command);
        register(this, new CommandLogin("login", null, (AccountManager) manager, "log", "l"));
    }

    public void beforeDisable() {
        ((AccountManager) manager).closeDatabase();
        ((AccountManager) manager).unProtectAll();
    }
}
