package org.soraworld.account.listener;

import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.event.item.inventory.TargetContainerEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import java.util.Optional;
import java.util.UUID;

public class EventListener {

    private final AccountManager manager;

    public EventListener(AccountManager manager) {
        this.manager = manager;
    }

    /* Auth Join Quit */

    @Listener
    public void onPlayerAuth(ClientConnectionEvent.Auth event) {
        String name = event.getProfile().getName().orElse("");
        if (manager.legalName(name)) {
            Optional<Player> player = Sponge.getServer().getPlayer(name);
            if (player.isPresent()) {
                event.setMessage(Text.of(manager.trans("alreadyOnline")));
                event.setCancelled(true);
            }
        } else {
            event.setMessage(Text.of(manager.trans("illegalName")));
            event.setCancelled(true);
        }
    }

    @Listener
    public void onPlayerLogin(ClientConnectionEvent.Login event, @First User user) {
        user.getOrCreate(Account.class).ifPresent(data -> {
            if (data.isRegistered() && manager.enableDB()) {
                final UUID uuid = user.getUniqueId();
                Task.builder().async().name("SyncSQL").execute(() -> {
                    Account acc = manager.queryAccount(uuid);
                    if (acc != null) {
                        // Sync User <- DataBase
                        data.sync(user, acc);
                        // TODO check sync main thread ???
                        Task.builder().name("SyncUser").execute(() -> {
                            user.offer(data);
                            if (manager.isDebug()) manager.console("SyncUser");
                        }).submit(manager.getPlugin());
                        // Sync User -> DataBase
                        acc.sync(user, data);
                        manager.saveAccount(acc);
                    } else manager.saveAccount(new Account(user, data));
                }).submit(manager.getPlugin());
            } else {
                data.reset();
                user.offer(data);
            }
        });
    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event, @First Player player) {
        manager.protect(player);
    }

    @Listener
    public void onPlayerQuit(ClientConnectionEvent.Disconnect event, @First Player player) {
        // TODO 如果玩家退出游戏，再次登陆 online 状态是否恢复默认值
        manager.logout(event.getTargetEntity());
        player.getOrCreate(Account.class).ifPresent(data -> {
            data.setOnline(false);
            player.offer(data);
            if (manager.enableDB()) {
                final UUID uuid = player.getUniqueId();
                Task.builder().async().name("SyncOffline").execute(() -> {
                    manager.setOffline(uuid);
                }).submit(manager.getPlugin());
            }
        });
    }

    /* protect */

    @Listener
    public void onChat(MessageChannelEvent.Chat event, @First Player player) {
        player.getOrCreate(Account.class).ifPresent(account -> {
            if (account.offline()) {
                event.setCancelled(true);
                manager.sendKey(player, "pleaseLogin");
            }
        });
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onCommand(SendCommandEvent event, @First Player player) {
        player.getOrCreate(Account.class).ifPresent(account -> {
            if (account.offline() || manager.allowCommand(event.getCommand())) {
                event.setCancelled(true);
                manager.sendKey(player, "pleaseLogin");
            }
        });
    }

    @Listener
    public void on(TargetContainerEvent event) {
        System.out.println(event.getClass());
    }

    @Listener
    public void onNumberPress(ClickInventoryEvent.NumberPress event) {
        System.out.println("onNumberPress");
        /*player.getOrCreate(Account.class).ifPresent(account -> {
            if (account.offline()) event.setCancelled(true);
        });*/
    }

    @Listener
    public void onOpenInventory(InteractInventoryEvent.Open event) {
        System.out.println("onOpenInventory");
        /*player.getOrCreate(Account.class).ifPresent(account -> {
            if (account.offline()) event.setCancelled(true);
        });*/
    }
}
