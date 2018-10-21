package org.soraworld.account.listener;

import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.*;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;

import java.util.Optional;
import java.util.UUID;

import static org.soraworld.account.manager.AccountManager.getAccount;

public class EventListener {

    private final AccountManager manager;

    public EventListener(AccountManager manager) {
        this.manager = manager;
    }

    /* Auth Join Quit */

    /* 检查在线状态 以及 用户名是否合法 */
    // TODO 检查大小写是否为同一玩家（不同UUID）
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

    /* 数据同步 */
    @Listener
    public void onPlayerLogin(ClientConnectionEvent.Login event, @First User user) {
        Account account = getAccount(user.getUniqueId());
        account.setUsername(user.getName());
        account.setOnline(false);
        user.offer(account);
        // TODO 玩家第一次登陆，此时的 nbt 里的 注册状态 是 false
        // TODO 如果 数据库 里的状态是 true (管理员直接注册的)
        // TODO 此时的数据同步该怎么做
        // 本地已注册，并且已启用数据库 NBT --> DB(未注册则创建，已注册则同步 DB 没有的数据)
        // 验证登陆时，从 DB 拉取数据 DB --> NBT 并更新缓存
        if (account.isRegistered() && manager.enableDB()) {
            final Account nbt = account.copy();
            Task.builder().async().name("DB->NBT").execute(() -> {
                // 拉取 DB 数据，不存在则 null
                Account db = manager.pullAccount(nbt.uuid());
                if (db == null) db = new Account();
                db.sync(nbt);
                manager.pushAccount(db);
            }).submit(manager.getPlugin());
        }
    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event, @First Player player) {
        // TODO autoLogin
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

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerMove(MoveEntityEvent event, @First Player player) {
        // TODO 登陆之前传送至出生点不能取消
        if (getAccount(player.getUniqueId()).offline()) event.setCancelled(true);
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onChat(MessageChannelEvent.Chat event, @First Player player) {
        if (getAccount(player.getUniqueId()).offline()) {
            event.setCancelled(true);
            manager.sendKey(player, "pleaseLogin");
        }
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onCommand(SendCommandEvent event, @First Player player) {
        if (getAccount(player.getUniqueId()).offline() && !manager.allowCommand(event.getCommand())) {
            // TODO kill 命令在验证登陆前取消
            event.setCancelled(true);
            manager.sendKey(player, "pleaseLogin");
        }
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onClickInventory(ClickInventoryEvent event, @First Player player) {
        if (getAccount(player.getUniqueId()).offline()) event.setCancelled(true);
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onOpenInventory(InteractInventoryEvent.Open event, @First Player player) {
        if (getAccount(player.getUniqueId()).offline()) event.setCancelled(true);
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerItemDrop(DropItemEvent event, @First Player player) {
        if (getAccount(player.getUniqueId()).offline()) event.setCancelled(true);
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerItemPickup(ChangeInventoryEvent.Pickup event, @Root Player player) {
        if (getAccount(player.getUniqueId()).offline()) event.setCancelled(true);
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onItemConsume(UseItemStackEvent.Start event, @First Player player) {
        if (getAccount(player.getUniqueId()).offline()) event.setCancelled(true);
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onItemInteract(InteractItemEvent event, @First Player player) {
        if (getAccount(player.getUniqueId()).offline()) event.setCancelled(true);
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onInventoryChange(ChangeInventoryEvent event, @First Player player) {
        if (getAccount(player.getUniqueId()).offline()) event.setCancelled(true);
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onInventoryInteract(InteractInventoryEvent event, @First Player player) {
        if (getAccount(player.getUniqueId()).offline()) event.setCancelled(true);
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onInventoryClick(ClickInventoryEvent event, @First Player player) {
        if (getAccount(player.getUniqueId()).offline()) event.setCancelled(true);
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onBlockInteract(InteractBlockEvent event, @First Player player) {
        if (getAccount(player.getUniqueId()).offline()) event.setCancelled(true);
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerInteractEntity(InteractEntityEvent event, @First Player player) {
        if (getAccount(player.getUniqueId()).offline()) event.setCancelled(true);
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerDamage(DamageEntityEvent event, @First Player player) {
        //player is damage source
        if (getAccount(player.getUniqueId()).offline()) event.setCancelled(true);
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onDamagePlayer(DamageEntityEvent event, @Getter("getTargetEntity") Player player) {
        //player is damage target
        if (getAccount(player.getUniqueId()).offline()) event.setCancelled(true);
    }

    /* prevent log password */
    @Listener
    public void onCommand(SendCommandEvent event) {
        if (manager.shouldHide(event.getCommand())) {
            //fake the cancelled event
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.POST)
    @IsCancelled(Tristate.TRUE)
    public void onPostCommand(SendCommandEvent event) {
        if (manager.shouldHide(event.getCommand())) {
            //re-enable it
            event.getContext();// TODO What's this ?
            event.setCancelled(true);
        }
    }
}
