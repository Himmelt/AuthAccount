package org.soraworld.account.manager;

import org.soraworld.hocon.node.Serializable;
import org.soraworld.hocon.node.Setting;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

@Serializable
class SpawnSetting {
    @Setting
    private boolean enabled;
    @Setting(comment = "Should the plugin use the default spawn from the world you specify below")
    private boolean defaultSpawn;
    @Setting(comment = "Spawn world or let it empty to use the default world specified in the server properties")
    private String worldName = Sponge.getServer().getDefaultWorldName();
    @Setting
    private int coordX, coordY, coordZ;

    public Location<World> getSpawnLocation() {
        if (worldName.isEmpty()) worldName = Sponge.getServer().getDefaultWorldName();

        Optional<World> optionalWorld = Sponge.getServer().getWorld(worldName);
        if (optionalWorld.isPresent()) {
            World world = optionalWorld.get();
            if (defaultSpawn) {
                return world.getSpawnLocation();
            }
            return world.getLocation(coordX, coordY, coordZ);
        }
        return null;
    }
}
