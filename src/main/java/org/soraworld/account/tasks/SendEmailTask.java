package org.soraworld.account.tasks;

import org.soraworld.account.AuthAccount;
import org.soraworld.account.manager.EmailSetting;
import org.spongepowered.api.entity.living.player.Player;

import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

public class SendEmailTask implements Runnable {

    private final Transport transport;
    private final MimeMessage email;

    private final Player player;

    public SendEmailTask(Player player, Transport transport, MimeMessage email) {
        this.transport = transport;
        this.email = email;
        this.player = player;
    }

    public void run() {

    }
}
