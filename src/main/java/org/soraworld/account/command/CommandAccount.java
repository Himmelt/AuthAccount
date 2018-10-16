package org.soraworld.account.command;

import org.soraworld.violet.command.Args;
import org.soraworld.violet.command.SpongeCommand;
import org.soraworld.violet.command.Sub;
import org.spongepowered.api.command.CommandSource;

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

    @Sub(onlyPlayer = true, aliases = {"mail"}, usage = "/account email [mail-address]")
    public static void email(SpongeCommand self, CommandSource sender, Args args) {

    }

    @Sub(onlyPlayer = true, aliases = {"forgotpswd"}, usage = "/account email [mail-address]")
    public static void forgotpassword(SpongeCommand self, CommandSource sender, Args args) {

    }
}
