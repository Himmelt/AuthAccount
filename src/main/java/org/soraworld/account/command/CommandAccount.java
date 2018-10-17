package org.soraworld.account.command;

import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;
import org.soraworld.violet.command.Args;
import org.soraworld.violet.command.SpongeCommand;
import org.soraworld.violet.command.Sub;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;

public final class CommandAccount {
    @Sub(onlyPlayer = true, aliases = {"reg"}, usage = "/account reg <password> <password>")
    public static void register(SpongeCommand self, CommandSource sender, Args args) {
        // TODO admin
    }

    @Sub(perm = "admin", aliases = {"unreg"}, usage = "/account unreg <account>")
    public static void unregister(SpongeCommand self, CommandSource sender, Args args) {

    }

    @Sub(perm = "admin", aliases = {"resetpswd"}, usage = "/account resetpswd <account>")
    public static void resetpassword(SpongeCommand self, CommandSource sender, Args args) {

    }

    @Sub(onlyPlayer = true, aliases = {"l", "log"}, usage = "/account login <password>")
    public static void login(SpongeCommand self, CommandSource sender, Args args) {

    }

    @Sub(onlyPlayer = true, aliases = {"changepswd"}, usage = "/account changepswd <old> <new>")
    public static void changepassword(SpongeCommand self, CommandSource sender, Args args) {

    }

    @Sub(onlyPlayer = true, aliases = {"mail"}, usage = "/account emailSetting [mail-address]")
    public static void email(SpongeCommand self, CommandSource sender, Args args) {

    }

    @Sub(onlyPlayer = true, aliases = {"forgotpswd"}, usage = "/account forgotpassword")
    public static void forgotpassword(SpongeCommand self, CommandSource sender, Args args) {
        Player player = (Player) sender;
        AccountManager manager = (AccountManager) self.manager;
        Account account = manager.getDatabase().getAccountIfPresent(player);
        if (account != null) {
            if (!account.isOnline()) {
                String email = account.getEmail();
                if (email != null && !email.isEmpty()) {
                    manager.sendResetEmail(account, player);
                } else manager.sendKey(player, "UncommittedEmailAddressMessage");
            } else manager.sendKey(player, "AlreadyLoggedInMessage");
        } else manager.sendKey(player, "AccountNotLoadedMessage");
    }
}
