package org.soraworld.account.tasks;

import org.bukkit.command.CommandSender;
import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;

import java.util.UUID;

public class ResetPwTask implements Runnable {

    private final CommandSender src;
    private final Object accountIndentifer;
    private final String password;
    private final AccountManager manager;

    public ResetPwTask(CommandSender src, UUID uuid, String password, AccountManager manager) {
        this.src = src;
        this.accountIndentifer = uuid;
        this.password = password;
        this.manager = manager;
    }

    public ResetPwTask(CommandSender src, String playerName, String password, AccountManager manager) {
        this.src = src;
        this.accountIndentifer = playerName;
        this.password = password;
        this.manager = manager;
    }

    public void run() {
        Account account;
        if (accountIndentifer instanceof String) {
            account = manager.pullAccount((String) accountIndentifer);
        } else {
            account = manager.pullAccount((UUID) accountIndentifer);
        }

        if (account == null) {
            manager.sendKey(src, "AccountNotFound");
        } else {
            try {
                account.setPassword(password);
                manager.sendKey(src, "ChangePasswordMessage");
            } catch (Exception e) {
                manager.console("Error creating hash");
            }
        }
    }
}
