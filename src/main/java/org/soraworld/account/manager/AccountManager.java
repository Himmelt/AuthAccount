package org.soraworld.account.manager;

import org.soraworld.account.data.Account;
import org.soraworld.account.data.Database;
import org.soraworld.account.hasher.BCryptHasher;
import org.soraworld.account.util.Rand;
import org.soraworld.hocon.node.Setting;
import org.soraworld.violet.manager.SpongeManager;
import org.soraworld.violet.plugin.SpongePlugin;
import org.soraworld.violet.util.ChatColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static jdk.nashorn.internal.objects.NativeString.replace;

public class AccountManager extends SpongeManager {

    @Setting(comment = "comment.general")
    public GeneralSetting general = new GeneralSetting();
    @Setting(comment = "comment.spawn")
    public SpawnSetting spawn = new SpawnSetting();
    @Setting(path = "database", comment = "comment.database")
    public DatabaseSetting databaseSetting = new DatabaseSetting();
    @Setting(comment = "comment.email")
    public EmailSetting emailSetting = new EmailSetting();

    public final Database database;
    public final ProtectionManager protectionManager = new ProtectionManager();
    public static final BCryptHasher hasher = new BCryptHasher();

    public AccountManager(SpongePlugin plugin, Path path) {
        super(plugin, path);
        database = new Database(this, path);
    }

    public ChatColor defChatColor() {
        return ChatColor.GOLD;
    }

    public void beforeLoad() {
        //run this task sync in order let it finish before the process ends
        Database database = new Database(this, path);
        database.close();
        Sponge.getServer().getOnlinePlayers().forEach(protectionManager::unprotect);
    }

    public void afterLoad() {
        database.createTable();
        Sponge.getServer().getOnlinePlayers().forEach(player -> {
            protectionManager.protect(player);
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
            player.offer(account);
        });
        Account account = database.remove(player);

        protectionManager.unprotect(player);

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

    public void login(Player player) {
        protectionManager.protect(player);

/*        Sponge.getScheduler().createTaskBuilder()
                .async()
                .execute(() -> onAccountLoaded(player))
                .submit(plugin);*/
    }

    public Database getDatabase() {
        return database;
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
            message.setSubject(replace(emailSetting.getSubject(), account, password));

            //current time
            message.setSentDate(new Date());

            String htmlContent = replace(emailSetting.getText(), account, password);
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
            account.setPasswordHash(hasher.hash(password));
            Task.builder().async().execute(() -> database.save(account)).submit(plugin);
        } catch (Throwable e) {
            if (debug) e.printStackTrace();
            consoleKey("sendMailException");
            sendKey(player, "sendMailFailed");
        }
    }

    public Map<String, Integer> getAttempts() {
        return new HashMap<>();
    }
}
