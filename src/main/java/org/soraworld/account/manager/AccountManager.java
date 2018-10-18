package org.soraworld.account.manager;

import org.soraworld.account.data.Account;
import org.soraworld.account.util.Rand;
import org.soraworld.hocon.node.Setting;
import org.soraworld.violet.manager.SpongeManager;
import org.soraworld.violet.plugin.SpongePlugin;
import org.soraworld.violet.util.ChatColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.manipulator.mutable.entity.MovementSpeedData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.nio.file.Path;
import java.util.*;

public class AccountManager extends SpongeManager {

    @Setting(comment = "comment.general")
    public final GeneralSetting general;
    @Setting(comment = "comment.spawn")
    public final SpawnSetting spawn;
    @Setting(path = "database", comment = "comment.database")
    public final Database database;
    @Setting(comment = "comment.email")
    public final EmailSetting emailSetting;

    private final HashMap<UUID, Location<World>> oldLocations = new HashMap<>();

    private static final HashMap<UUID, Double> originWalkSpeed = new HashMap<>();
    private static final HashMap<UUID, Double> originFlySpeed = new HashMap<>();

    public AccountManager(SpongePlugin plugin, Path path) {
        super(plugin, path);
        this.general = new GeneralSetting();
        this.spawn = new SpawnSetting();
        this.database = new Database(this, path);
        this.emailSetting = new EmailSetting();
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
            database.loadAccount(player);
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
        String password = Rand.randString(8);

        // TODO check setProperty & put
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", emailSetting.getHost());
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.port", emailSetting.getPort());
        properties.put("mail.smtp.starttls.enable", true);

        Session session = Session.getDefaultInstance(properties);

        //prepare email
        MimeMessage message = new MimeMessage(session);
        try {
            String YuiAccount = emailSetting.getAccount();
            //sender email with an alias
            message.setFrom(new InternetAddress(YuiAccount, emailSetting.getSenderName()));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(account.getEmail(), account.username()));
            message.setSubject(replace(emailSetting.getSubject(), player, password));

            //current time
            message.setSentDate(new Date());

            String htmlContent = replace(emailSetting.getText(), player, password);
            //allow html
            message.setContent(htmlContent, "text/html;charset=utf-8");
            //we only need to send the message so we use smtp
            Transport transport = session.getTransport("smtp");
            //send email
            Task.builder().async().execute(() -> {
                try {
                    //connect to host and send message
                    if (!transport.isConnected()) {
                        transport.connect(emailSetting.getHost(), emailSetting.getAccount(), emailSetting.getPassword());
                    }
                    transport.sendMessage(message, message.getAllRecipients());
                    sendKey(player, "RecoveryEmailSent");
                } catch (Exception e) {
                    if (debug) e.printStackTrace();
                    consoleKey("sendMailException");
                    sendKey(player, "sendMailFailed");
                }
            }).submit(plugin);
            //set new password here if the email sending fails fails we have still the old password
            account.setPassword(password);
            Task.builder().async().execute(() -> database.save(account)).submit(plugin);
        } catch (Throwable e) {
            if (debug) e.printStackTrace();
            consoleKey("sendMailException");
            sendKey(player, "sendMailFailed");
        }
    }

    private static String replace(String text, Player player, String password) {
        return text.replace("%player%", player.getName()).replace("%password%", password);
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
}
