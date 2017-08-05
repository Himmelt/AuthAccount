package org.soraworld.authme;

import com.google.common.collect.Maps;
import org.soraworld.authme.config.SpawnTeleportConfig;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Map;
import java.util.UUID;

public class ProtectionManager {

    private final Map<UUID, Location<World>> oldLocations = Maps.newHashMap();
    private final Authme plugin = Authme.getInstance();

    public void protect(Player player) {
        SpawnTeleportConfig teleportConfig = plugin.getCfgLoader().getConfig().getTeleportConfig();
        if (teleportConfig.isEnabled()) {
            Location<World> spawnLocation = teleportConfig.getSpawnLocation();
            if (spawnLocation != null) {
                oldLocations.put(player.getUniqueId(), player.getLocation());
                if (plugin.getCfgLoader().getConfig().isSafeLocation()) {
                    Sponge.getTeleportHelper().getSafeLocation(spawnLocation).ifPresent(player::setLocation);
                } else {
                    player.setLocation(spawnLocation);
                }
            }
        } else {
            Location<World> oldLoc = player.getLocation();

            //sometimes players stuck in a wall
            if (plugin.getCfgLoader().getConfig().isSafeLocation()) {
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

        if (plugin.getCfgLoader().getConfig().isSafeLocation()) {
            Sponge.getTeleportHelper().getSafeLocation(oldLocation).ifPresent(player::setLocation);
        } else {
            player.setLocation(oldLocation);
        }
    }
}
