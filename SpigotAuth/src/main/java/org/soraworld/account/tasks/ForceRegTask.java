package org.soraworld.account.tasks;

import org.bukkit.command.CommandSender;
import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;

import java.util.UUID;

public class ForceRegTask implements Runnable {

    private final AccountManager manager;

    private final CommandSender sender;
    private final UUID accountIndentifer;
    private final String password;

    public ForceRegTask(AccountManager manager, CommandSender sender, UUID uuid, String password) {
        this.manager = manager;
        this.sender = sender;
        this.accountIndentifer = uuid;
        this.password = password;
    }

    public void run() {
        Account account = manager.pullAccount(accountIndentifer);

        if (account == null) {
            try {
                account = new Account(accountIndentifer, "", password, 0);
                if (manager.createAccount(account, false)) {
                    manager.sendKey(sender, "ForceRegisterSuccessMessage");
                } else {
                    // TODO failed
                }
            } catch (Exception ex) {
                manager.console("Error creating hash");
            }
        } else {
            manager.sendKey(sender, "AccountAlreadyExists");
        }
    }
}
