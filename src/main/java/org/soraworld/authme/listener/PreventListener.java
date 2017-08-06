package org.soraworld.authme.listener;

import com.flowpowered.math.vector.Vector3d;
import org.soraworld.authme.Authme;
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
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.item.inventory.UseItemStackEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PreventListener {

    private final Authme authme = Authme.getInstance();

    @Listener(order = Order.EARLY)
    public void onPlayerMove(MoveEntityEvent event, @First Player player) {
        Vector3d oldLocation = event.getFromTransform().getPosition();
        Vector3d newLocation = event.getToTransform().getPosition();
        if ((oldLocation.getFloorX() != newLocation.getFloorX()
                || oldLocation.getFloorZ() != newLocation.getFloorZ())) {
            checkLoginStatus(event, player);
        }
    }

    @Listener
    public void onChat(MessageChannelEvent.Chat event, @First Player player) {
        checkLoginStatus(event, player);
    }

    @Listener(order = Order.EARLY)
    public void onCommand(SendCommandEvent event, @First Player player) {
        String command = event.getCommand();

        Optional<? extends CommandMapping> commandOpt = authme.getGame().getCommandManager().get(command);
        if (commandOpt.isPresent()) {
            command = commandOpt.get().getPrimaryAlias();
        }

        //do not blacklist our own commands
        if (authme.getGame().getCommandManager()
                .getOwnedBy(authme.getContainer())
                .stream()
                .map(CommandMapping::getPrimaryAlias)
                .collect(Collectors.toSet())
                .contains(command)) {
            return;
        }

        if (authme.getCfgLoader().getConfig().isCommandOnlyProtection()) {
            List<String> protectedCommands = authme.getCfgLoader().getConfig().getProtectedCommands();
            if ((protectedCommands.isEmpty() || protectedCommands.contains(command))) {
                if (!authme.getDatabase().isOnline(player)) {
                    player.sendMessage(authme.getCfgLoader().getTextConfig().getProtectedCommand());
                    event.setCancelled(true);
                }
            }
        } else {
            checkLoginStatus(event, player);
        }
    }

    @Listener(order = Order.EARLY)
    public void onPlayerItemDrop(DropItemEvent.Dispense event, @First EntitySpawnCause spawnCause) {
        if (spawnCause.getEntity() instanceof Player) {
            checkLoginStatus(event, (Player) spawnCause.getEntity());
        }
    }

    @Listener(order = Order.EARLY)
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

    @Listener(order = Order.FIRST)
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

    private void checkLoginStatus(Cancellable event, Player player) {
        if (authme.getCfgLoader().getConfig().isBypassPermission()
                && player.hasPermission(authme.getContainer().getId() + ".bypass")) {
            return;
        }

        if (authme.getCfgLoader().getConfig().isCommandOnlyProtection()) {
            //check if the user is already registered
            if (authme.getDatabase().getAccountIfPresent(player) == null
                    && player.hasPermission(authme.getContainer().getId() + ".registerRequired")) {
                event.setCancelled(true);
            }
        } else if (!authme.getDatabase().isOnline(player)) {
            event.setCancelled(true);
        }
    }
}
