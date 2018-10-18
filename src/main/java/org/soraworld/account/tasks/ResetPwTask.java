package org.soraworld.account.tasks;

import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;
import org.spongepowered.api.command.CommandSource;

import java.util.UUID;

public class ResetPwTask implements Runnable {

    private final CommandSource src;
    private final Object accountIndentifer;
    private final String password;
    private final AccountManager manager;

    public ResetPwTask(CommandSource src, UUID uuid, String password, AccountManager manager) {
        this.src = src;
        this.accountIndentifer = uuid;
        this.password = password;
        this.manager = manager;
    }

    public ResetPwTask(CommandSource src, String playerName, String password, AccountManager manager) {
        this.src = src;
        this.accountIndentifer = playerName;
        this.password = password;
        this.manager = manager;
    }

    @Override
    public void run() {
        Account account;
        if (accountIndentifer instanceof String) {
            account = manager.loadAccount((String) accountIndentifer);
        } else {
            account = manager.loadAccount((UUID) accountIndentifer);
        }

        if (account == null) {
            manager.sendKey(src, "AccountNotFound");
        } else {
            try {
                account.setPassword(password);
                manager.sendKey(src, "ChangePasswordMessage");
            } catch (Exception ex) {
                manager.console("Error creating hash");
            }
        }
    }
}
