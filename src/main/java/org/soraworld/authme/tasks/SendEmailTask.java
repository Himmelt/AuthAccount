package org.soraworld.authme.tasks;

import org.soraworld.authme.Authme;
import org.soraworld.authme.config.EmailConfiguration;
import org.spongepowered.api.entity.living.player.Player;

import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

public class SendEmailTask implements Runnable {

    private final Authme plugin = Authme.getInstance();
    private final Transport transport;
    private final MimeMessage email;

    private final Player player;

    public SendEmailTask(Player player, Transport transport, MimeMessage email) {
        this.transport = transport;
        this.email = email;
        this.player = player;
    }

    @Override
    public void run() {
        try {
            EmailConfiguration emailConfig = plugin.loader().config().getEmailConfiguration();

            //connect to host and send message
            if (!transport.isConnected()) {
                transport.connect(emailConfig.getHost(), emailConfig.getAccount(), emailConfig.getPassword());
            }

            transport.sendMessage(email, email.getAllRecipients());
            player.sendMessage(plugin.loader().getTextConfig().getMailSent());
        } catch (Exception ex) {
            plugin.getLogger().error("Error sending email", ex);
            player.sendMessage(plugin.loader().getTextConfig().getErrorCommandMessage());
        }
    }
}
