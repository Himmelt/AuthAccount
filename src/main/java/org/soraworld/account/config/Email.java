package org.soraworld.account.config;

import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;
import org.soraworld.account.util.Rand;
import org.soraworld.hocon.node.Serializable;
import org.soraworld.hocon.node.Setting;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

@Serializable
public class Email {

    @Setting(comment = "Is password recovery using an email allowed")
    private boolean enabled = false;

    @Setting(comment = "Mail server")
    private String host = "smtp.gmail.com";

    @Setting(comment = "SMTP Port for outgoing messages")
    private int port = 465;

    @Setting(comment = "Username for the account you want to the email from")
    private String account = "";

    @Setting(comment = "Password for the account you want to the email from")
    private String password = "";

    @Setting(comment = "Displays as sender in the email client")
    private String senderName = "Your minecraft server name";

    @Setting(comment = "Email subject/title")
    private String subject = "Your new Password";

    @Setting(comment = "Email contents. You can use HTML here")
    private String text = "New password for %player% on Minecraft server %server%: %password%";

    private final AccountManager manager;

    public Email(AccountManager manager) {
        this.manager = manager;
    }

    public void sendResetEmail(Account target, Player player) {
        String password = Rand.randString(8);

        // TODO check setProperty & put
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", host);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.starttls.enable", true);

        Session session = Session.getDefaultInstance(properties);

        //prepare email
        MimeMessage message = new MimeMessage(session);
        try {
            //sender email with an alias
            message.setFrom(new InternetAddress(account, senderName));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(target.getEmail(), target.username()));
            message.setSubject(replace(subject, player, password));

            //current time
            message.setSentDate(new Date());

            String htmlContent = replace(text, player, password);
            //allow html
            message.setContent(htmlContent, "text/html;charset=utf-8");
            //we only need to send the message so we use smtp
            Transport transport = session.getTransport("smtp");
            //send email
            Task.builder().async().execute(() -> {
                try {
                    //connect to host and send message
                    if (!transport.isConnected()) {
                        transport.connect(host, account, password);
                    }
                    transport.sendMessage(message, message.getAllRecipients());
                    manager.sendKey(player, "RecoveryEmailSent");
                } catch (Exception e) {
                    if (manager.isDebug()) e.printStackTrace();
                    manager.consoleKey("sendMailException");
                    manager.sendKey(player, "sendMailFailed");
                }
            }).submit(manager.getPlugin());
            //set new password here if the email sending fails fails we have still the old password
            target.setPassword(password);
            Task.builder().async().execute(() -> manager.saveAccount(target)).submit(manager.getPlugin());
        } catch (Throwable e) {
            if (manager.isDebug()) e.printStackTrace();
            manager.consoleKey("sendMailException");
            manager.sendKey(player, "sendMailFailed");
        }
    }

    private static String replace(String text, Player player, String password) {
        return text.replace("%player%", player.getName()).replace("%password%", password);
    }
}
