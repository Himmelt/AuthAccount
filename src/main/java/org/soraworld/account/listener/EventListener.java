package org.soraworld.account.listener;

import com.flowpowered.math.vector.Vector3d;
import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;
import org.soraworld.account.tasks.LoginMessageTask;
import org.soraworld.account.util.IPUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.entity.spawn.EntitySpawnCause;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.*;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
                                    Account acc = manager.database.loadAccount(uuid);
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
        manager.login(event.getTargetEntity());
    }

    @Listener
    public void onPlayerQuit(ClientConnectionEvent.Disconnect event) {
        manager.logout(event.getTargetEntity());
    }

    @Listener(order = Order.AFTER_PRE)
    public void onPlayerMove(MoveEntityEvent event, @First Player player) {
        Vector3d oldLocation = event.getFromTransform().getPosition();
        Vector3d newLocation = event.getToTransform().getPosition();
        if ((oldLocation.getFloorX() != newLocation.getFloorX()
                || oldLocation.getFloorZ() != newLocation.getFloorZ())) {
            checkLoginStatus(event, player);
        }
    }

    @Listener(order = Order.AFTER_PRE)
    public void onInteractItem(InteractItemEvent event, @First Player player) {
        checkLoginStatus(event, player);
    }

    @Listener(order = Order.AFTER_PRE)
    public void onInventoryOpen(InteractInventoryEvent.Open event, @First Player player) {
        checkLoginStatus(event, player);
    }

    @Listener
    public void onChat(MessageChannelEvent.Chat event, @First Player player) {
        if (!checkLoginStatus(event, player)) {
            player.sendMessage(Text.of("您没有执行该操作的权限,请先登录!"));
        }
    }

    @Listener(order = Order.EARLY)
    public void onCommand(SendCommandEvent event, @First Player player) {
        String command = event.getCommand();

        Optional<? extends CommandMapping> commandOpt = authAccount.getGame().getCommandManager().get(command);
        if (commandOpt.isPresent()) {
            command = commandOpt.get().getPrimaryAlias();
        }

        //do not blacklist our own commands
        if (authAccount.getGame().getCommandManager()
                .getOwnedBy(authAccount.plugin())
                .stream()
                .map(CommandMapping::getPrimaryAlias)
                .collect(Collectors.toSet())
                .contains(command)) {
            return;
        }

        if (authAccount.loader().config().isCommandOnlyProtection()) {
            List<String> protectedCommands = authAccount.loader().config().getProtectedCommands();
            if ((protectedCommands.isEmpty() || protectedCommands.contains(command))) {
                if (!authAccount.getDatabase().isOnline(player)) {
                    player.sendMessage(authAccount.loader().getTextConfig().getProtectedCommand());
                    event.setCancelled(true);
                }
            }
        } else {
            if (!checkLoginStatus(event, player)) {
                player.sendMessage(Text.of("您没有执行该操作的权限,请先登录!"));
            }
        }
    }

    @Listener(order = Order.EARLY)
    public void onPlayerItemDrop(DropItemEvent.Dispense event, @First EntitySpawnCause spawnCause) {
        if (spawnCause.getEntity() instanceof Player) {
            checkLoginStatus(event, (Player) spawnCause.getEntity());
        }
    }

    @Listener(order = Order.AFTER_PRE)
    public void onItemConsume(UseItemStackEvent.Start event, @First Player player) {
        checkLoginStatus(event, player);
    }

    @Listener(order = Order.FIRST)
    public void onInventoryChange(ChangeInventoryEvent event, @First Player player) {
        checkLoginStatus(event, player);
    }

    @Listener(order = Order.EARLY)
    public void onInventoryDrop(DropItemEvent.Dispense event, @First Player player) {
        checkLoginStatus(event, player);
    }

    @Listener(order = Order.AFTER_PRE)
    public void onBlockInteract(InteractBlockEvent event, @First Player player) {
        checkLoginStatus(event, player);
    }

    @Listener
    public void onPlayerDamage(DamageEntityEvent event, @First Player player) {
        checkLoginStatus(event, player);
    }

    @Listener(order = Order.FIRST)
    public void onDamagePlayer(DamageEntityEvent event) {
        //check the target
        Entity targetEntity = event.getTargetEntity();
        //check only if the event isn't already cancelled by the first call
        if (targetEntity instanceof Player) {
            checkLoginStatus(event, (Player) event.getTargetEntity());
        }
    }

    private boolean checkLoginStatus(Cancellable event, Player player) {
        if (authAccount.loader().config().bypass() && player.hasPermission(authAccount.plugin().getId() + ".bypass")) {
            return true;
        }
        if (authAccount.loader().config().isCommandOnlyProtection()) {
            //check if the user is already registered
            if (authAccount.getDatabase().getAccountIfPresent(player) == null && player.hasPermission(authAccount.plugin().getId() + ".registerRequired")) {
                event.setCancelled(true);
                return false;
            }
        } else if (!authAccount.getDatabase().isOnline(player)) {
            event.setCancelled(true);
            return false;
        }
        return true;
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
