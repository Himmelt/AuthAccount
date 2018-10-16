package org.soraworld.account.manager;

import org.soraworld.account.data.Account;
import org.soraworld.account.data.Database;
import org.soraworld.account.hasher.BCryptHasher;
import org.soraworld.hocon.node.Setting;
import org.soraworld.violet.manager.SpongeManager;
import org.soraworld.violet.plugin.SpongePlugin;
import org.soraworld.violet.util.ChatColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;

import java.nio.file.Path;

public class AccountManager extends SpongeManager {

    @Setting(comment = "comment.general")
    private GeneralSetting general = new GeneralSetting();
    @Setting(comment = "comment.spawn")
    public SpawnSetting spawn = new SpawnSetting();
    @Setting(path = "database", comment = "comment.database")
    public DatabaseSetting databaseSetting = new DatabaseSetting();
    @Setting(comment = "comment.email")
    private EmailSetting email = new EmailSetting();

    private final Database database;
    private final ProtectionManager protectionManager = new ProtectionManager();
    private static final BCryptHasher hasher = new BCryptHasher();

    public AccountManager(SpongePlugin plugin, Path path) {
        super(plugin, path);
        database = new Database(this, path);
    }

    public ChatColor defChatColor() {
        return ChatColor.AQUA;
    }

    public void beforeLoad() {
        //run this task sync in order let it finish before the process ends
        Database database = new Database(this, path);
        database.close();
        Sponge.getServer().getOnlinePlayers().forEach(protectionManager::unprotect);
    }

    public void afterLoad() {
        database.createTable();
        Sponge.getServer().getOnlinePlayers().forEach(player -> {
            protectionManager.protect(player);
            database.loadAccount(player);
        });
    }

    public void closeDatabase() {

    }

    public void unProtectAll() {

    }

    public void logout(Player player) {
        Account account = database.remove(player);

        protectionManager.unprotect(player);

/*        if (account != null) {
            plugin.getAttempts().remove(player.getName());
            //account is loaded -> mark the player as logout as it could remain in the cache
            account.setOnline(false);

            if (plugin.loader().config().isUpdateLoginStatus()) {
                Sponge.getScheduler().createTaskBuilder()
                        .async().execute(() -> plugin.getDatabase().flushLoginStatus(account, false))
                        .submit(plugin);
            }
        }*/
    }

    public void login(Player player) {
        protectionManager.protect(player);

/*        Sponge.getScheduler().createTaskBuilder()
                .async()
                .execute(() -> onAccountLoaded(player))
                .submit(plugin);*/
    }

    public Database getDatabase() {
        return database;
    }
}
