package org.soraworld.account.tasks;

import org.soraworld.account.data.Account;
import org.soraworld.account.AuthAccount;

public class SaveTask implements Runnable {

    private final AuthAccount plugin = AuthAccount.getInstance();
    private final Account account;

    public SaveTask(Account account) {
        this.account = account;
    }

    @Override
    public void run() {
        plugin.getDatabase().save(account);
    }
}
