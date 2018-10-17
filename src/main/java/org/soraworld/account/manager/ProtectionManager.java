package org.soraworld.account.manager;

import org.soraworld.account.AuthAccount;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.manipulator.mutable.entity.MovementSpeedData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.UUID;

public class ProtectionManager {

    private final HashMap<UUID, Location<World>> oldLocations = new HashMap<>();
    private final AuthAccount authAccount = AuthAccount.getInstance();

    private static final HashMap<UUID, Double> originWalkSpeed = new HashMap<>();
    private static final HashMap<UUID, Double> originFlySpeed = new HashMap<>();

    public void protect(Player player) {
        player.getOrCreate(MovementSpeedData.class).ifPresent(speed -> {
            originWalkSpeed.put(player.getUniqueId(), speed.walkSpeed().get());
            originFlySpeed.put(player.getUniqueId(), speed.flySpeed().get());
            // TODO check negative speed
            speed.walkSpeed().set(0.0D);
            speed.flySpeed().set(0.0D);
            player.offer(speed);
        });
        SpawnSetting teleportConfig = authAccount.loader().config().getTeleportConfig();
        if (teleportConfig.isEnabled()) {
            Location<World> spawnLocation = teleportConfig.getSpawnLocation();
            if (spawnLocation != null) {
                oldLocations.put(player.getUniqueId(), player.getLocation());
                if (authAccount.loader().config().isSafeLocation()) {
                    Sponge.getTeleportHelper().getSafeLocation(spawnLocation).ifPresent(player::setLocation);
                } else {
                    player.setLocation(spawnLocation);
                }
            }
        } else {
            Location<World> oldLoc = player.getLocation();

            //sometimes players stuck in a wall
            if (authAccount.loader().config().isSafeLocation()) {
                Sponge.getTeleportHelper().getSafeLocation(oldLoc).ifPresent(player::setLocation);
            } else {
                player.setLocation(oldLoc);
            }
        }
    }

    public void unprotect(Player player) {
        UUID uuid = player.getUniqueId();
        player.getOrCreate(MovementSpeedData.class).ifPresent(speed -> {
            // TODO check default value
            speed.walkSpeed().set(originWalkSpeed.getOrDefault(uuid, 0.1D));
            originWalkSpeed.remove(uuid);
            speed.flySpeed().set(originFlySpeed.getOrDefault(uuid, 0.1D));
            originFlySpeed.remove(uuid);
            player.offer(speed);
        });

        Location<World> oldLocation = oldLocations.remove(uuid);
        if (oldLocation == null) {
            return;
        }

        if (authAccount.loader().config().isSafeLocation()) {
            Sponge.getTeleportHelper().getSafeLocation(oldLocation).ifPresent(player::setLocation);
        } else {
            player.setLocation(oldLocation);
        }
    }
}
