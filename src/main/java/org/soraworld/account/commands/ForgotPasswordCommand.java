package org.soraworld.account.commands;

import org.soraworld.account.data.Account;
import org.soraworld.account.AuthAccount;
import org.soraworld.account.manager.EmailSetting;
import org.soraworld.account.tasks.SaveTask;
import org.soraworld.account.tasks.SendEmailTask;
import org.soraworld.account.util.Rand;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandPermissionException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.LiteralText;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import javax.mail.Message.RecipientType;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Properties;

public class ForgotPasswordCommand implements CommandExecutor {

    private final AuthAccount plugin = AuthAccount.getInstance();

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (!(src instanceof Player)) {
            src.sendMessage(plugin.loader().getTextConfig().getPlayersOnlyActionMessage());
            return CommandResult.success();
        }

        if (plugin.loader().config().isPlayerPermissions()
                && !src.hasPermission(plugin.plugin().getId() + ".command.forgot")) {
            throw new CommandPermissionException();
        }

        Player player = (Player) src;
        Account account = plugin.getDatabase().getAccountIfPresent(player);
        if (account == null) {
            src.sendMessage(plugin.loader().getTextConfig().getAccountNotLoadedMessage());
            return CommandResult.success();
        } else if (account.isOnline()) {
            src.sendMessage(plugin.loader().getTextConfig().getAlreadyLoggedInMessage());
            return CommandResult.success();
        }

        String email = account.getEmail();
        if (email == null || email.isEmpty()) {
            src.sendMessage(plugin.loader().getTextConfig().getUncommittedEmailAddressMessage());
            return CommandResult.success();
        }

        String password = Rand.randString(8);

        EmailSetting emailConfig = plugin.loader().config().getEmailConfiguration();

        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", emailConfig.getHost());
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.port", emailConfig.getPort());
        properties.put("mail.smtp.starttls.enable", true);

        Session session = Session.getDefaultInstance(properties);

        //prepare email
        MimeMessage message = new MimeMessage(session);
        try {
            String senderEmail = emailConfig.getAccount();
            //sender email with an alias
            message.setFrom(new InternetAddress(senderEmail, emailConfig.getSenderName()));
            message.setRecipient(RecipientType.TO, new InternetAddress(email, src.getName()));
            message.setSubject(replace(emailConfig.getSubject(), player, password));

            //current time
            message.setSentDate(Calendar.getInstance().getTime());

            String textContent = replace(emailConfig.getText(), player, password);
            //allow html
            message.setContent(textContent, "text/html;charset=utf-8");
            //we only need to send the message so we use smtp
            Transport transport = session.getTransport("smtp");
            //send email
            Sponge.getScheduler().createTaskBuilder()
                    .async()
                    .execute(new SendEmailTask(player, transport, message))
                    .submit(plugin);
            src.sendMessage(LiteralText.of("will send an email to your email address"));
            //set new password here if the email sending fails fails we have still the old password
            account.setPasswordHash(plugin.getHasher().hash(password));
            Sponge.getScheduler().createTaskBuilder()
                    .async()
                    .execute(new SaveTask(account))
                    .submit(plugin);
        } catch (UnsupportedEncodingException ex) {
            //we can ignore this, because we will encode with UTF-8 which all Java platforms supports
        } catch (Exception ex) {
            plugin.getLogger().error("Error executing command", ex);
            src.sendMessage(Text.of(TextColors.DARK_RED
                    , plugin.loader().getTextConfig().getErrorCommandMessage()));
        }

        return CommandResult.success();
    }

    private String replace(String text, Player player, String password) {
        return text.replace("%player%", player.getName()).replace("%password%", password);
    }
}
