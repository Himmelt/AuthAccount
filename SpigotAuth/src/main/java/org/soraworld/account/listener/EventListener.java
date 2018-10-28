package org.soraworld.account.listener;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerCommandEvent;
import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;

import java.util.UUID;

import static org.soraworld.account.manager.AccountManager.getAccount;
import static org.soraworld.account.manager.AccountManager.removeCache;

public class EventListener implements Listener {

    private final AccountManager manager;

    public EventListener(AccountManager manager) {
        this.manager = manager;
    }

    /* Auth Join Quit */

    /* 检查在线状态 以及 用户名是否合法 */
    // TODO 实际测试检查大小写是否为同一玩家（不同UUID）
    @EventHandler
    public void onPlayerAuth(PlayerLoginEvent event) {
/*        String name = event.getProfile().getName().orElse("");
        if (manager.legalName(name)) {
            Optional<Player> player = Sponge.getServer().getPlayer(name);
            if (player.isPresent()) {
                event.setMessage(Text.of(manager.trans("alreadyOnline")));
                event.setCancelled(true);
            }
        } else {
            event.setMessage(Text.of(manager.trans("illegalName")));
            event.setCancelled(true);
        }*/
    }

    /* 数据同步 */
    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        // NBT -> CACHE
        final Account cache = getAccount(event.getPlayer()).setOnline(false);
        if (manager.enableDB()) {
            // 异步 1. DB -> CACHE
            // 同步 更新 CACHE
            // 异步 2. CACHE -> DB
            final UUID uuid = cache.uuid();
            final Account copy = cache.copy();
            /*Task.builder().async().execute(() -> {
                Account db = manager.pullAccount(uuid);
                if (db != null) {
                    Task.builder().execute(() -> {
                        cache.sync(db);
                        db.sync(cache);
                        Task.builder().async().execute(() -> {
                            manager.pushAccount(db);
                        }).submit(manager.getPlugin());
                    }).submit(manager.getPlugin());
                } else {
                    manager.pushAccount(copy);
                }
            }).submit(manager.getPlugin());*/
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerLoginEvent event) {
        // TODO autoLogin
        manager.protect(event.getPlayer());
    }

    /* 数据同步 */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.unprotect(event.getPlayer());
        // CACHE -> NBT && CLEAR
        if (manager.enableDB()) {
            // 异步 1. CACHE -> DB UPDATE STATUS
        }
        removeCache(event.getPlayer());
    }

    /* protect */

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerMove(PlayerMoveEvent event) {
        // TODO 登陆之前传送至出生点不能取消
        if (getAccount(event.getPlayer()).offline() && !event.getFrom().equals(event.getTo())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent event) {
        if (getAccount(event.getPlayer()).offline()) {
            event.setCancelled(true);
            manager.sendKey(event.getPlayer(), "pleaseLogin");
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCommand(PlayerCommandSendEvent event) {
        if (getAccount(event.getPlayer()).offline() && !manager.allowCommand(/*TODO fix*/event.getCommands().toString())) {
            // TODO kill 命令在验证登陆前取消
            //event.setCancelled(true);
            manager.sendKey(event.getPlayer(), "pleaseLogin");
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onClickInventory(InventoryClickEvent event) {
        HumanEntity human = event.getWhoClicked();
        if (human instanceof Player) {
            if (getAccount((Player) human).offline()) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onOpenInventory(InventoryOpenEvent event) {
        HumanEntity human = event.getPlayer();
        if (human instanceof Player) {
            if (getAccount((Player) human).offline()) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerItemDrop(PlayerDropItemEvent event) {
        if (getAccount(event.getPlayer()).offline()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerItemPickup(EntityPickupItemEvent event) {
        LivingEntity living = event.getEntity();
        if (living instanceof Player) {
            if (getAccount((Player) living).offline()) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (getAccount(event.getPlayer()).offline()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onItemInteract(PlayerInteractEvent event) {
        if (getAccount(event.getPlayer()).offline()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryChange(InventoryMoveItemEvent event) {
        //if (getAccount(player).offline()) event.setCancelled(true);
    }

    /* @EventHandler(priority = EventPriority.LOW)
     public void onInventoryInteract(InteractInventoryEvent event) {
         if (getAccount(player).offline()) event.setCancelled(true);
     }

     @EventHandler(priority = EventPriority.LOW)
     public void onInventoryClick(ClickInventoryEvent event) {
         if (getAccount(player).offline()) event.setCancelled(true);
     }

     @EventHandler(priority = EventPriority.LOW)
     public void onBlockInteract(InteractBlockEvent event) {
         if (getAccount(player).offline()) event.setCancelled(true);
     }

     @EventHandler(priority = EventPriority.LOW)
     public void onPlayerInteractEntity(InteractEntityEvent event) {
         if (getAccount(player).offline()) event.setCancelled(true);
     }

     @EventHandler(priority = EventPriority.LOW)
     public void onPlayerDamage(DamageEntityEvent event) {
         //player is damage source
         if (getAccount(player).offline()) event.setCancelled(true);
     }

     @EventHandler(priority = EventPriority.LOW)
     public void onDamagePlayer(DamageEntityEvent event, @Getter("getTargetEntity") Player player) {
         //player is damage target
         if (getAccount(player).offline()) event.setCancelled(true);
     }
 */
    /* prevent log password */
    //@EventHandler
    public void onCommand(ServerCommandEvent event) {
        if (manager.shouldHide(event.getCommand())) {
            //fake the cancelled event
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPostCommand(ServerCommandEvent event) {
        if (manager.shouldHide(event.getCommand())) {
            //re-enable it
            //event.getContext();// TODO What's this ?
            event.setCancelled(true);
        }
    }
}
