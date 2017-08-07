package org.soraworld.authme.listener;

import org.soraworld.authme.Account;
import org.soraworld.authme.Authme;
import org.soraworld.authme.config.Config;
import org.soraworld.authme.tasks.LoginMessageTask;
import org.soraworld.authme.util.IPUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ConnectionListener {

    private static final String VALID_USERNAME = "^\\w{2,16}$";

    private final Authme plugin = Authme.getInstance();

    @Listener
    public void onPlayerQuit(ClientConnectionEvent.Disconnect playerQuitEvent) {
        Player player = playerQuitEvent.getTargetEntity();
        Account account = plugin.getDatabase().remove(player);

        plugin.getProtectionManager().unprotect(player);

        if (account != null) {
            plugin.getAttempts().remove(player.getName());
            //account is loaded -> mark the player as logout as it could remain in the cache
            account.setOnline(false);

            if (plugin.loader().config().isUpdateLoginStatus()) {
                Sponge.getScheduler().createTaskBuilder()
                        .async().execute(() -> plugin.getDatabase().flushLoginStatus(account, false))
                        .submit(plugin);
            }
        }
    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join playerJoinEvent) {
        Player player = playerJoinEvent.getTargetEntity();
        plugin.getProtectionManager().protect(player);

        Sponge.getScheduler().createTaskBuilder()
                .async()
                .execute(() -> onAccountLoaded(player))
                .submit(plugin);
    }

    @Listener
    public void onPlayerAuth(ClientConnectionEvent.Auth playerAuthEvent) {
        String playerName = playerAuthEvent.getProfile().getName().get();
        if (!playerName.matches(VALID_USERNAME)) {
            //validate invalid characters
            playerAuthEvent.setMessage(plugin.loader().getTextConfig().getInvalidUsername());
            playerAuthEvent.setCancelled(true);
        } else {
            Optional<Player> player = Sponge.getServer().getPlayer(playerName);
            if (player.isPresent() && player.get().getName().equals(playerName)) {
                playerAuthEvent.setMessage(plugin.loader().getTextConfig().getAlreadyOnlineMessage());
                playerAuthEvent.setCancelled(true);
            }
        }
    }

    public void onAccountLoaded(Player player) {
        Account account = plugin.getDatabase().loadAccount(player);

        Config config = plugin.loader().config();
        if (account == null) {
            if (config.isCommandOnlyProtection()) {
                if (player.hasPermission(plugin.plugin().getId() + ".registerRequired")) {
                    //command only protection but have to register
                    sendNotLoggedInMessage(player);
                }
            } else {
                //no account
                sendNotLoggedInMessage(player);
            }
        } else {
            long lastLogin = account.getTimestamp().getTime();
            if (config.isIpAutoLogin() && account.ip().equals(IPUtil.getPlayerIP(player))
                    && System.currentTimeMillis() < lastLogin + 12 * 60 * 60 * 1000
                    && !player.hasPermission(plugin.plugin().getId() + ".no_auto_login")) {
                //user will be auto logged in
                player.sendMessage(plugin.loader().getTextConfig().getIpAutoLogin());
                account.setOnline(true);
            } else {
                //user has an account but isn't logged in
                sendNotLoggedInMessage(player);
            }
        }

        scheduleTimeoutTask(player);
    }

    private void sendNotLoggedInMessage(Player player) {
        //send the message if the player only needs to login
        if (!plugin.loader().config().bypass()
                || !player.hasPermission(plugin.plugin().getId() + ".bypass")) {
            Sponge.getScheduler().createTaskBuilder()
                    .execute(new LoginMessageTask(player))
                    .interval(plugin.loader().config().getMessageInterval(), TimeUnit.SECONDS)
                    .submit(plugin);
        }
    }

    private void scheduleTimeoutTask(Player player) {
        Config config = plugin.loader().config();
        if (plugin.loader().config().bypass()
                && player.hasPermission(plugin.plugin().getId() + ".bypass")) {
            return;
        }

        if (!config.isCommandOnlyProtection() && config.getTimeoutLogin() != -1) {
            Sponge.getScheduler().createTaskBuilder()
                    .execute(() -> {
                        Account account = plugin.getDatabase().getAccountIfPresent(player);
                        if (account == null || !account.isOnline()) {
                            player.kick(plugin.loader().getTextConfig().getTimeoutReason());
                        }
                    })
                    .delay(config.getTimeoutLogin(), TimeUnit.SECONDS)
                    .submit(plugin);
        }
    }
}
