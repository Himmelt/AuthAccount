package org.soraworld.account.listener;

import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;
import org.soraworld.account.tasks.LoginMessageTask;
import org.soraworld.account.util.IPUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ConnectionListener {

    private final AccountManager manager;
    private static final String VALID_USERNAME = "[A-Za-z0-9_]{1,15}";
    private final UserStorageService userStorage;

    public ConnectionListener(AccountManager manager) {
        this.manager = manager;
        userStorage = Sponge.getServiceManager().provide(UserStorageService.class).orElse(null);
    }

    @Listener
    public void onPlayerQuit(ClientConnectionEvent.Disconnect event) {
        manager.logout(event.getTargetEntity());
    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event) {
        manager.login(event.getTargetEntity());
    }

    @Listener
    public void onPlayerAuth(ClientConnectionEvent.Auth event) {
        GameProfile profile = event.getProfile();
        // TODO 如果玩家的用户名和之前注册的大小写不一致，尝试更改用户名和UUID,
        // TODO 或 禁止登陆
        profile.getName().ifPresent(name -> {
            userStorage.get(profile.getUniqueId()).ifPresent(user->{
                if(user.getName().equals(name)){

                }else {
                    event.setMessage(Text.of(manager.trans("notMatchReg")));
                    event.setCancelled(true);
                }
            });

        });
        String playerName = event.getProfile().getName().get();
        UUID uuid = event.getProfile().getUniqueId();
        if (!playerName.matches(VALID_USERNAME)) {
            //validate invalid characters
            event.setMessage(Text.of(manager.trans("illegalName")));
            event.setCancelled(true);
        } else {
            Optional<Player> player = Sponge.getServer().getPlayer(playerName);
            Sponge.getServer().getPlayer(uuid).ifPresent(player1 -> {

            });
            if (player.isPresent() && player.get().getName().equals(playerName)) {
                event.setMessage(Text.of(manager.trans("alreadyOnline")));
                event.setCancelled(true);
            }
        }
    }

    public void onAccountLoaded(Player player) {
        Account account = manager.getDatabase().loadAccount(player);

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
