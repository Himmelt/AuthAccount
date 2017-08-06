package org.soraworld.authme;

import org.soraworld.authme.config.SpawnTeleportConfig;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.UUID;

public class ProtectionManager {

    private final HashMap<UUID, Location<World>> oldLocations = new HashMap<>();
    private final Authme authme = Authme.getInstance();

    public void protect(Player player) {
        SpawnTeleportConfig teleportConfig = authme.getCfgLoader().getConfig().getTeleportConfig();
        if (teleportConfig.isEnabled()) {
            Location<World> spawnLocation = teleportConfig.getSpawnLocation();
            if (spawnLocation != null) {
                oldLocations.put(player.getUniqueId(), player.getLocation());
                if (authme.getCfgLoader().getConfig().isSafeLocation()) {
                    Sponge.getTeleportHelper().getSafeLocation(spawnLocation).ifPresent(player::setLocation);
                } else {
                    player.setLocation(spawnLocation);
                }
            }
        } else {
            Location<World> oldLoc = player.getLocation();

            //sometimes players stuck in a wall
            if (authme.getCfgLoader().getConfig().isSafeLocation()) {
                Sponge.getTeleportHelper().getSafeLocation(oldLoc).ifPresent(player::setLocation);
            } else {
                player.setLocation(oldLoc);
            }
        }
    }

    public void unprotect(Player player) {
        Location<World> oldLocation = oldLocations.remove(player.getUniqueId());
        if (oldLocation == null) {
            return;
        }

        if (authme.getCfgLoader().getConfig().isSafeLocation()) {
            Sponge.getTeleportHelper().getSafeLocation(oldLocation).ifPresent(player::setLocation);
        } else {
            player.setLocation(oldLocation);
        }
    }
}
