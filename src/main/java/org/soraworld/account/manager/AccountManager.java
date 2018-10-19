package org.soraworld.account.manager;

import org.soraworld.account.config.Database;
import org.soraworld.account.config.Email;
import org.soraworld.account.config.General;
import org.soraworld.account.config.Spawn;
import org.soraworld.account.data.Account;
import org.soraworld.hocon.node.Setting;
import org.soraworld.violet.manager.SpongeManager;
import org.soraworld.violet.plugin.SpongePlugin;
import org.soraworld.violet.util.ChatColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.manipulator.mutable.entity.MovementSpeedData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AccountManager extends SpongeManager {

    @Setting(comment = "comment.general")
    private final General general;
    @Setting(path = "database", comment = "comment.database")
    private final Database database;
    @Setting(comment = "comment.email")
    private final Email email;
    @Setting(comment = "comment.spawn")
    private final Spawn spawn;

    private final HashMap<UUID, Location<World>> oldLocations = new HashMap<>();

    private static final HashMap<UUID, Double> originWalkSpeed = new HashMap<>();
    private static final HashMap<UUID, Double> originFlySpeed = new HashMap<>();

    public AccountManager(SpongePlugin plugin, Path path) {
        super(plugin, path);
        this.general = new General();
        this.database = new Database(this, path);
        this.email = new Email(this);
        this.spawn = new Spawn();
    }

    public ChatColor defChatColor() {
        return ChatColor.GOLD;
    }

    public void beforeLoad() {
        //run this task sync in order let it finish before the process ends
        database.close();
        Sponge.getServer().getOnlinePlayers().forEach(this::unprotect);
    }

    public void afterLoad() {
        database.createTable();
        Sponge.getServer().getOnlinePlayers().forEach(player -> {
            protect(player);
            database.loadAccount(player.getUniqueId());
        });
    }

    public void closeDatabase() {

    }

    public void unProtectAll() {

    }

    public void logout(Player player) {
        player.getOrCreate(Account.class).ifPresent(account -> {
            account.setOnline(false);
            // TODO check user data will effect ??
            player.offer(account);
        });
        Account account = database.remove(player);

        unprotect(player);

/*        if (account != null) {
            plugin.getAttempts().remove(player.getName());
            //account is loaded -> mark the player as logout as it could remain in the cache
            account.setOnline(false);

            if (plugin.loader().config().isUpdateLoginStatus()) {
                Sponge.getScheduler().createTaskBuilder()
                        .async().execute(() -> plugin.getDatabase().flushLoginStatus(account, false))
                        .submit(plugin);
            }
        }*/
    }

    public void join(Player player) {
        protect(player);

/*        Sponge.getScheduler().createTaskBuilder()
                .async()
                .execute(() -> onAccountLoaded(player))
                .submit(plugin);*/
    }

    public void sendResetEmail(Account account, Player player) {
        email.sendResetEmail(account, player);
    }

    public Map<String, Integer> getAttempts() {
        return new HashMap<>();
    }

    public void protect(Player player) {
        player.getOrCreate(MovementSpeedData.class).ifPresent(speed -> {
            originWalkSpeed.put(player.getUniqueId(), speed.walkSpeed().get());
            originFlySpeed.put(player.getUniqueId(), speed.flySpeed().get());
            // TODO check negative speed
            speed.walkSpeed().set(0.0D);
            speed.flySpeed().set(0.0D);
            player.offer(speed);
        });
        if (spawn.enabled) {
            Location<World> spawnLocation = spawn.getSpawnLocation();
            if (spawnLocation != null) {
                oldLocations.put(player.getUniqueId(), player.getLocation());
                if (general.safeLocation) {
                    Sponge.getTeleportHelper().getSafeLocation(spawnLocation).ifPresent(player::setLocation);
                } else {
                    player.setLocation(spawnLocation);
                }
            }
        } else {
            Location<World> oldLoc = player.getLocation();
            //sometimes players stuck in a wall
            if (general.safeLocation) {
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

        if (general.safeLocation) {
            Sponge.getTeleportHelper().getSafeLocation(oldLocation).ifPresent(player::setLocation);
        } else {
            player.setLocation(oldLocation);
        }
    }

    public Account getAccountIfPresent(Player player) {
        return database.getAccountIfPresent(player);
    }

    public boolean saveAccount(Account account) {
        return database.save(account);
    }

    public Account loadAccount(UUID uuid) {
        return database.loadAccount(uuid);
    }

    public Account loadAccount(String name) {
        return database.loadAccount(name);
    }

    public void flushLoginStatus(Account account, boolean online) {
        database.flushLoginStatus(account, online);
    }

    public int getRegistrationsCount(String ip) {
        return database.getRegistrationsCount(ip);
    }

    public boolean createAccount(Account account, boolean cache) {
        return database.createAccount(account, cache);
    }

    public Account deleteAccount(UUID uuid) {
        return database.deleteAccount(uuid);
    }

    public Account deleteAccount(String name) {
        return database.deleteAccount(name);
    }

    public int getMaxIpReg() {
        return general.maxIpReg;
    }

    public boolean updateLoginStatus() {
        return general.updateLoginStatus;
    }

    public int maxAttempts() {
        return general.maxAttempts;
    }

    public int waitTime() {
        return general.waitTime;
    }
}
