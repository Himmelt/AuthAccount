package org.soraworld.authme.tasks;

import org.soraworld.authme.Account;
import org.soraworld.authme.Authme;

public class SaveTask implements Runnable {

    private final Authme plugin = Authme.getInstance();
    private final Account account;

    public SaveTask(Account account) {
        this.account = account;
    }

    @Override
    public void run() {
        plugin.getDatabase().save(account);
    }
}
