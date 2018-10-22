package org.soraworld.account.command;

import com.google.common.base.Charsets;
import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;
import org.soraworld.account.tasks.ForceRegTask;
import org.soraworld.account.tasks.RegisterTask;
import org.soraworld.account.tasks.ResetPwTask;
import org.soraworld.violet.command.Args;
import org.soraworld.violet.command.SpongeCommand;
import org.soraworld.violet.command.Sub;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.soraworld.account.manager.AccountManager.getAccount;

public final class CommandAccount {

    private static final Pattern E_MAIL = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+");

    /* user commands */

    @Sub(onlyPlayer = true, aliases = {"reg"}, usage = "/account reg <password> <password>")
    public static void register(SpongeCommand self, CommandSource sender, Args args) {
        AccountManager manager = (AccountManager) self.manager;
        Player player = (Player) sender;
        if (args.size() == 2) {
            String password = args.get(1);
            if (password.equals(args.get(2))) {
                if (manager.legalPswd(password)) {
                    //Check if the first two passwords are equal to prevent typos
                    //we are executing a SQL Query which is blocking
                    Task.builder().async().name("RegisterQuery")
                            .execute(new RegisterTask(manager, player, password))
                            .submit(manager.getPlugin());
                } else manager.sendKey(player, "illegalPassword");
            } else manager.sendKey(player, "UnequalPasswordsMessage");
        } else manager.sendKey(player, "regUsage");
    }

    @Sub(onlyPlayer = true, aliases = {"unreg"}, usage = "/account unregister <password>")
    public static void unregister(SpongeCommand self, CommandSource sender, Args args) {
        // TODO 需要登陆
    }

    @Sub(onlyPlayer = true, aliases = {"l", "log"}, usage = "/account login <password>")
    public static void login(SpongeCommand self, CommandSource sender, Args args) {

    }

    @Sub(onlyPlayer = true, aliases = {"mail"}, usage = "/account email [mail-address]")
    public static void email(SpongeCommand self, CommandSource sender, Args args) {
        Player player = (Player) sender;
        AccountManager manager = (AccountManager) self.manager;
        Account account = getAccount(player.getUniqueId());
        if (account.isRegistered()) {
            if (account.online()) {
                if (args.notEmpty()) {
                    String mail = args.first();
                    if (E_MAIL.matcher(mail).matches()) {
                        account.setEmail(mail);
                        manager.sendKey(player, "setEmail", mail);
                        Task.builder().async().execute(() -> manager.pushAccount(account)).submit(manager.getPlugin());
                    } else manager.sendKey(player, "invalidEmail");
                } else manager.sendKey(player, "getEmail", account.getEmail());
            } else manager.sendKey(player, "pleaseLogin");
        } else manager.sendKey(player, "accountNotRegister");
    }

    @Sub(onlyPlayer = true, aliases = {"change", "changepswd"}, usage = "/account changepswd <old> <new> <new>")
    public static void changepassword(SpongeCommand self, CommandSource sender, Args args) {
        Player player = (Player) sender;
        AccountManager manager = (AccountManager) self.manager;
        Account account = getAccount(player.getUniqueId());
        if (account.isRegistered()) {
            if (account.online()) {
                if (args.size() == 3) {
                    if (account.checkPassword(args.first())) {
                        // TODO password format check
                        String password = args.get(1);
                        if (!password.isEmpty() && password.equals(args.get(2))) {
                            try {
                                //Check if the first two passwords are equal to prevent typos
                                Sponge.getScheduler().createTaskBuilder()
                                        //we are executing a SQL Query which is blocking
                                        .async()
                                        .name("Register Query")
                                        .execute(() -> {
                                            account.setPassword(password);
                                            if (manager.pushAccount(account)) {
                                                manager.sendKey(player, "ChangePasswordMessage");
                                            } else manager.sendKey(player, "ErrorCommandMessage");
                                        }).submit(manager.getPlugin());
                            } catch (Exception e) {
                                if (manager.isDebug()) e.printStackTrace();
                                manager.consoleKey("ErrorCommandMessage");
                            }
                        } else manager.sendKey(player, "UnequalPasswordsMessage");
                    } else manager.sendKey(player, "wrongOldPswd");
                } else manager.sendKey(player, "invalidArgs");
            } else manager.sendKey(player, "pleaseLogin");
        } else manager.sendKey(player, "accountNotRegister");
    }

    @Sub(onlyPlayer = true, aliases = {"forgot", "forgotpswd"}, usage = "/account forgotpassword")
    public static void forgotpassword(SpongeCommand self, CommandSource sender, Args args) {
        Player player = (Player) sender;
        AccountManager manager = (AccountManager) self.manager;
        Account account = getAccount(player.getUniqueId());
        if (account.isRegistered()) {
            if (account.offline()) {
                String email = account.getEmail();
                if (email != null && !email.isEmpty()) {
                    manager.sendResetEmail(account, player);
                } else manager.sendKey(player, "UncommittedEmailAddressMessage");
            } else manager.sendKey(player, "AlreadyLoggedInMessage");
        } else manager.sendKey(player, "accountNotRegister");
    }

    /* admin commands */

    @Sub(path = "admin.register", perm = "admin", aliases = {"reg"}, usage = "/account admin reg <account> <password>")
    public static void admin_register(SpongeCommand self, CommandSource sender, Args args) {
        AccountManager manager = (AccountManager) self.manager;

        if (!checkOnline(manager, sender)) return;

        if (args.size() == 2) {
            String text = args.first();
            String pswd = args.get(1);
            try {
                UUID uuid = UUID.fromString(text);

                Optional<Player> player = Sponge.getServer().getPlayer(uuid);
                if (player.isPresent()) {
                    manager.sendKey(sender, "ForceRegisterOnlineMessage");
                } else {
                    Task.builder().async().execute(new ForceRegTask(manager, sender, uuid, pswd)).submit(manager.getPlugin());
                }
            } catch (Throwable e) {

                // TODO check illegal name

                Optional<Player> player = Sponge.getServer().getPlayer(text);
                if (player.isPresent()) {
                    manager.sendKey(sender, "ForceRegisterOnlineMessage");
                } else {
                    // TODO
                    UUID offlineUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + text).getBytes(Charsets.UTF_8));

                    Task.builder().async().execute(new ForceRegTask(manager, sender, offlineUUID, pswd)).submit(manager.getPlugin());
                }
            }
        } else manager.sendKey(sender, "invalidArgs");
    }

    @Sub(path = "admin.unregister", perm = "admin", aliases = {"unreg"}, usage = "/account admin unreg <account> [confirm]")
    public static void admin_unregister(SpongeCommand self, CommandSource sender, Args args) {
        AccountManager manager = (AccountManager) self.manager;
        if (!checkOnline(manager, sender)) return;
        if (args.notEmpty()) {
            String text = args.first();
            try {
                UUID uuid = UUID.fromString(text);
                // TODO confirm
                Task.builder().async().execute(() -> {
                    Account acc = manager.deleteAccount(uuid);
                    if (acc != null) manager.sendKey(sender, "AccountDeleted", acc.username(), acc.uuid());
                    else manager.sendKey(sender, "AccountNotFound");
                }).submit(manager.getPlugin());
            } catch (Throwable e) {
                // TODO confirm
                Task.builder().async().execute(() -> {
                    Account acc = manager.deleteAccount(text);
                    if (acc != null) manager.sendKey(sender, "AccountDeleted", acc.username(), acc.uuid());
                    else manager.sendKey(sender, "AccountNotFound");
                }).submit(manager.getPlugin());
            }
        } else manager.sendKey(sender, "emptyArgs");
    }

    @Sub(path = "admin.resetpassword", perm = "admin", aliases = {"reset", "resetpswd"}, usage = "/account admin resetpswd <account> <password>")
    public static void resetpassword(SpongeCommand self, CommandSource sender, Args args) {
        AccountManager manager = (AccountManager) self.manager;
        if (!checkOnline(manager, sender)) return;
        if (args.size() == 2) {
            String text = args.first();
            String pswd = args.get(1);
            try {
                UUID uuid = UUID.fromString(text);
                //check if the account is an UUID
                Optional<Player> player = Sponge.getServer().getPlayer(uuid);
                if (player.isPresent()) {
                    Account account = getAccount(uuid);
                    if (!account.isRegistered()) {
                        manager.sendKey(sender, "AccountNotFound");
                    } else {
                        try {
                            account.setPassword(pswd);
                            manager.sendKey(sender, "ChangePasswordMessage");
                        } catch (Exception e) {
                            if (manager.isDebug()) e.printStackTrace();
                            manager.consoleKey("Error creating hash");
                        }
                    }
                } else {
                    Task.builder().async().execute(new ResetPwTask(sender, uuid, pswd, manager)).submit(manager.getPlugin());
                }
            } catch (Throwable e) {
                Optional<Player> player = Sponge.getServer().getPlayer(text);
                if (player.isPresent()) {
                    Account account = getAccount(player.get().getUniqueId());
                    if (!account.isRegistered()) {
                        manager.sendKey(sender, "AccountNotFound");
                    } else {
                        try {
                            account.setPassword(pswd);
                            manager.sendKey(sender, "ChangePasswordMessage");
                        } catch (Exception e2) {
                            if (manager.isDebug()) e2.printStackTrace();
                            manager.consoleKey("Error creating hash");
                        }
                    }
                } else {
                    Task.builder().async().execute(new ResetPwTask(sender, text, pswd, manager)).submit(manager.getPlugin());
                }
            }
        } else manager.sendKey(sender, "invalidArgs");
    }

    private static boolean checkOnline(AccountManager manager, CommandSource sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Account account = getAccount(player.getUniqueId());
            if (!account.isRegistered()) {
                manager.sendKey(player, "accountNotRegister");
                return false;
            }
            if (account.offline()) {
                manager.sendKey(player, "pleaseLogin");
                return false;
            }
        }
        return true;
    }
}
