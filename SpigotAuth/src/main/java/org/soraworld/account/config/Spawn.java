package org.soraworld.account.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.soraworld.hocon.node.Serializable;
import org.soraworld.hocon.node.Setting;

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

    public Location getSpawnLocation() {
        if (worldName.isEmpty()) worldName = Bukkit.getServer().getWorlds().get(0).getName();
        World world = Bukkit.getServer().getWorld(worldName);
        if (world != null) {
            if (defaultSpawn) {
                return world.getSpawnLocation();
            }
            return new Location(world, coordX, coordY, coordZ);
        }
        return null;
    }
}
