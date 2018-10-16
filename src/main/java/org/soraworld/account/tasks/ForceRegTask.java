package org.soraworld.account.tasks;

import org.soraworld.account.data.Account;
import org.soraworld.account.AuthAccount;
import org.spongepowered.api.command.CommandSource;

import java.util.UUID;

public class ForceRegTask implements Runnable {

    private final AuthAccount plugin = AuthAccount.getInstance();

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

                src.sendMessage(plugin.loader().getTextConfig().getForceRegisterSuccessMessage());
            } catch (Exception ex) {
                plugin.getLogger().error("Error creating hash", ex);
                src.sendMessage(plugin.loader().getTextConfig().getErrorCommandMessage());
            }
        } else {
            src.sendMessage(plugin.loader().getTextConfig().getAccountAlreadyExists());
        }
    }
}
