package org.soraworld.account.listener;

import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public class EventListener {

    private final AccountManager manager;
    private final UserStorageService userStorage;
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_-]{1,15}");

    public EventListener(AccountManager manager, UserStorageService userStorage) {
        this.manager = manager;
        this.userStorage = userStorage;
    }

    /* Auth Join Quit */

    @Listener
    public void onPlayerAuth(ClientConnectionEvent.Auth event) {
        final UUID uuid = event.getProfile().getUniqueId();
        // TODO check illegal username
        String name = event.getProfile().getName().orElse(null);
        if (name != null && !name.isEmpty()) {
            if (USERNAME.matcher(name).matches()) {
                Optional<Player> player = Sponge.getServer().getPlayer(name);
                if (player.isPresent() && player.get().getName().equals(name)) {
                    event.setMessage(Text.of(manager.trans("alreadyOnline")));
                    event.setCancelled(true);
                } else {
                    userStorage.get(event.getProfile()).ifPresent(user -> {
                        user.getOrCreate(Account.class).ifPresent(data -> {
                            if (data.uid >= 0) {
                                Task.builder().async().name("SyncSQLData").execute(() -> {
                                    Account acc = manager.loadAccount(uuid);
                                    if (uuid.equals(acc.uuid())) {
                                        data.copy(acc);
                                        // TODO check sync main thread ???
                                        Task.builder().name("SyncUserData").execute(() -> {
                                            user.offer(data);
                                            if (manager.isDebug()) manager.console("SyncUserData");
                                        }).submit(manager.getPlugin());
                                    } else {
                                        // TODO kick ???
                                    }
                                }).submit(manager.getPlugin());
                            }
                        });
                    });
                }
                return;
            }
        }
        event.setMessage(Text.of(manager.trans("illegalName")));
        event.setCancelled(true);
    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event) {
        manager.join(event.getTargetEntity());
    }

    @Listener
    public void onPlayerQuit(ClientConnectionEvent.Disconnect event) {
        manager.logout(event.getTargetEntity());
    }

    @Listener
    public void onChat(MessageChannelEvent.Chat event, @First Player player) {

    }

    @Listener(order = Order.EARLY)
    public void onCommand(SendCommandEvent event, @First Player player) {

    }
}
