package org.soraworld.account.manager;

import org.soraworld.account.AuthAccount;
import org.soraworld.account.manager.SpawnSetting;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.UUID;

public class ProtectionManager {

    private final HashMap<UUID, Location<World>> oldLocations = new HashMap<>();
    private final AuthAccount authAccount = AuthAccount.getInstance();

    public void protect(Player player) {
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
        Location<World> oldLocation = oldLocations.remove(player.getUniqueId());
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
