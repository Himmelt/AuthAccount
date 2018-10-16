package org.soraworld.account.tasks;

import org.soraworld.account.AuthAccount;
import org.spongepowered.api.command.CommandSource;

import java.util.UUID;

public class unregisterTask implements Runnable {

    private final AuthAccount authAccount = AuthAccount.getInstance();
    private final CommandSource source;

    private final String identity;

    public unregisterTask(CommandSource source, UUID uuid) {
        this.source = source;
        this.identity = uuid.toString();
    }

    public unregisterTask(CommandSource source, String username) {
        this.source = source;
        this.identity = username;
    }

    @Override
    public void run() {
        boolean accountFound;
        accountFound = authAccount.getDatabase().deleteAccount(identity);

        if (accountFound) {
            source.sendMessage(authAccount.loader().getTextConfig().getAccountDeleted(identity));
        } else {
            source.sendMessage(authAccount.loader().getTextConfig().getAccountNotFound());
        }
    }
}
