package org.soraworld.authme.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

@ConfigSerializable
public class SpawnTeleportConfig {

    @Setting
    private boolean enabled;

    @Setting(comment = "Should the plugin use the default spawn from the world you specify below")
    private boolean defaultSpawn;

    @Setting(comment = "Spawn world or let it empty to use the default world specified in the server properties")
    private String worldName = "";

    @Setting
    private int coordX;

    @Setting
    private int coordY;

    @Setting
    private int coordZ;

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDefaultSpawn() {
        return defaultSpawn;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getCoordX() {
        return coordX;
    }

    public int getCoordY() {
        return coordY;
    }

    public int getCoordZ() {
        return coordZ;
    }

    public Location<World> getSpawnLocation() {
        if (worldName.isEmpty()) {
            worldName = Sponge.getServer().getDefaultWorldName();
        }

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
