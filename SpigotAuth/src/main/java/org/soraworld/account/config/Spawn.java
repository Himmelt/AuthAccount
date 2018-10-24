package org.soraworld.account.config;

import org.soraworld.hocon.node.Serializable;
import org.soraworld.hocon.node.Setting;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

@Serializable
public class Spawn {
    @Setting(comment = "comment.spawn.enabled")
    public boolean enabled = true;
    @Setting(comment = "Should the plugin use the default spawn from the world you specify below")
    public boolean defaultSpawn = false;
    @Setting(comment = "Spawn world or let it empty to use the default world specified in the server properties")
    public String worldName = "";
    @Setting
    public int coordX, coordY, coordZ;

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
