package org.soraworld.authme.tasks;

import org.soraworld.authme.Authme;
import org.spongepowered.api.command.CommandSource;

import java.util.UUID;

public class unregisterTask implements Runnable {

    private final Authme authme = Authme.getInstance();
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
        accountFound = authme.getDatabase().deleteAccount(identity);

        if (accountFound) {
            source.sendMessage(authme.loader().getTextConfig().getAccountDeleted(identity));
        } else {
            source.sendMessage(authme.loader().getTextConfig().getAccountNotFound());
        }
    }
}
