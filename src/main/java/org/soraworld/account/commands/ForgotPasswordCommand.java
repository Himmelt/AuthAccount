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


    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {


        return CommandResult.success();
    }

    private String replace(String text, Player player, String password) {
        return text.replace("%player%", player.getName()).replace("%password%", password);
    }
}
