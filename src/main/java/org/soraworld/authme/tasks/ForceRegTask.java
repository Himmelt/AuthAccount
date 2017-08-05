package org.soraworld.authme.tasks;

import org.soraworld.authme.Account;
import org.soraworld.authme.Authme;
import org.spongepowered.api.command.CommandSource;

import java.util.UUID;

public class ForceRegTask implements Runnable {

    private final Authme plugin = Authme.getInstance();

    private final CommandSource src;
    private final UUID accountIndentifer;
    private final String password;

    public ForceRegTask(CommandSource src, UUID uuid, String password) {
        this.src = src;
        this.accountIndentifer = uuid;
        this.password = password;
    }

    @Override
    public void run() {
        Account account = plugin.getDatabase().loadAccount(accountIndentifer);

        if (account == null) {
            try {
                String hash = plugin.getHasher().hash(password);
                account = new Account(accountIndentifer, "", hash, "invalid");
                plugin.getDatabase().createAccount(account, false);

                src.sendMessage(plugin.getCfgLoader().getTextConfig().getForceRegisterSuccessMessage());
            } catch (Exception ex) {
                plugin.getLogger().error("Error creating hash", ex);
                src.sendMessage(plugin.getCfgLoader().getTextConfig().getErrorCommandMessage());
            }
        } else {
            src.sendMessage(plugin.getCfgLoader().getTextConfig().getAccountAlreadyExists());
        }
    }
}
