package org.soraworld.account.manager;

import org.soraworld.account.AuthAccount;
import org.soraworld.account.config.Database;
import org.soraworld.account.config.Email;
import org.soraworld.account.config.General;
import org.soraworld.account.config.Spawn;
import org.soraworld.account.data.Account;
import org.soraworld.account.serializer.PatternSerializer;
import org.soraworld.hocon.node.Setting;
import org.soraworld.violet.manager.SpongeManager;
import org.soraworld.violet.util.ChatColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.manipulator.mutable.entity.MovementSpeedData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AccountManager extends SpongeManager {

    @Setting(comment = "comment._general")
    private final General general;
    @Setting(path = "database", comment = "comment._database")
    private final Database database;
    @Setting(comment = "comment._email")
    private final Email email;
    @Setting(comment = "comment._spawn")
    private final Spawn spawn;

    private final AuthAccount plugin;
    private final HashMap<UUID, Location<World>> oldLocations = new HashMap<>();

    private static final HashMap<UUID, Double> originWalkSpeed = new HashMap<>();
    private static final HashMap<UUID, Double> originFlySpeed = new HashMap<>();
    private static final HashMap<UUID, GameMode> originGameMode = new HashMap<>();
    private static final ConcurrentHashMap<UUID, Account> cache = new ConcurrentHashMap<>();

    public AccountManager(AuthAccount plugin, Path path) {
        super(plugin, path);
        this.plugin = plugin;
        this.options.registerType(new PatternSerializer());
        this.general = new General();
        this.database = new Database(this);
        this.email = new Email(this);
        this.spawn = new Spawn();
    }

    public boolean setLang(String lang) {
        boolean success = super.setLang(lang);
        email.loadHtml(lang);
        return success;
    }

    public ChatColor defChatColor() {
        return ChatColor.GOLD;
    }

    public void beforeLoad() {
        //run this task sync in order let it finish before the process ends
        cache.clear();
        database.close();
        Sponge.getServer().getOnlinePlayers().forEach(this::unprotect);
    }

    public void afterLoad() {
        database.createTable();
        Sponge.getServer().getOnlinePlayers().forEach(player -> {
            protect(player);
            // TODO 此处什么目的？？
            database.queryAccount(player.getUniqueId());
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
        // TODO remove cache remove database ?
        cache.remove(player.getUniqueId());
        //Account account = database.remove(player);

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

    public Map<UUID, Integer> getAttempts() {
        return new HashMap<>();
    }

    public void protect(Player player) {
        originGameMode.put(player.getUniqueId(), player.gameMode().get());
        player.offer(player.gameMode().set(GameModes.SPECTATOR));
        player.getOrCreate(MovementSpeedData.class).ifPresent(speed -> {
            originWalkSpeed.put(player.getUniqueId(), speed.walkSpeed().get());
            originFlySpeed.put(player.getUniqueId(), speed.flySpeed().get());
            // TODO check negative speed
            speed.set(speed.walkSpeed().set(0.0D));
            speed.set(speed.flySpeed().set(0.0D));
            System.out.println(speed.walkSpeed().get());
            System.out.println(speed.flySpeed().get());
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
        GameMode mode = originGameMode.remove(uuid);
        player.offer(player.gameMode().set(mode != null ? mode : GameModes.CREATIVE));
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

    public boolean pushAccount(Account account) {
        // TODO 更新状态，只更新部分数据
        // TODO 更新cache
        return database.save(account);
    }

    public Account pullAccount(UUID uuid) {
        return database.queryAccount(uuid);
    }

    public Account pullAccount(String name) {
        return database.queryAccount(name);
    }

    public void flushLoginStatus(Account account, boolean online) {
        database.flushLoginStatus(account, online);
    }

    public int getRegistrationsCount(String ip) {
        return database.getRegistrationsCount(ip);
    }

    public boolean createAccount(Account account, boolean cache) {
        // TODO cache
        return database.createAccount(account, cache);
    }

    public Account deleteAccount(UUID uuid) {
        return database.deleteAccount(uuid);
    }

    public Account deleteAccount(String name) {
        // TODO remove cache
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

    public boolean legalName(String name) {
        return name != null && !general.banNames.contains(name) && general.nameRegex.matcher(name).matches();
    }

    public boolean enableDB() {
        return database.enable;
    }

    public void setOffline(UUID uuid) {
        database.setOffline(uuid);
    }

    public boolean allowCommand(String command) {
        return plugin.getCmdNames().contains(command) || general.allowCommands.contains(command);
    }

    public boolean shouldHide(String command) {
        return plugin.getCmdNames().contains(command);
    }

    /**
     * 获取玩家账户<br>
     * 所有使用的地方都应该先用 {@link Account#isRegistered} 检查是否已注册.
     *
     * @param uuid 玩家UUID
     * @return 账户
     */
    public static Account getAccount(UUID uuid) {
        return cache.computeIfAbsent(uuid, Account::new);
    }

    public boolean fallBack() {
        return database.fallBack;
    }
}
