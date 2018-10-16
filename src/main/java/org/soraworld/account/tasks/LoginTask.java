package org.soraworld.account.tasks;

import org.soraworld.account.data.Account;
import org.soraworld.account.AuthAccount;
import org.soraworld.account.util.IPUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.entity.living.player.Player;

import java.util.concurrent.TimeUnit;

public class LoginTask implements Runnable {

    private final AuthAccount plugin = AuthAccount.getInstance();

    private final Player player;
    private final String userInput;

    public LoginTask(Player player, String password) {
        this.player = player;
        this.userInput = password;
    }

    @Override
    public void run() {
        Account account = plugin.getDatabase().loadAccount(player);
        if (account == null) {
            player.sendMessage(plugin.loader().getTextConfig().getAccountNotFound());
            return;
        }

        try {
            Integer attempts = plugin.getAttempts().computeIfAbsent(player.getName(), (playerName) -> 0);
            if (attempts > plugin.loader().config().getMaxAttempts()) {
                player.sendMessage(plugin.loader().getTextConfig().getMaxAttemptsMessage());
                String lockCommand = plugin.loader().config().getLockCommand();
                if (lockCommand != null && !lockCommand.isEmpty()) {
                    ConsoleSource console = Sponge.getServer().getConsole();
                    plugin.getGame().getCommandManager().process(console, lockCommand);
                }

                Sponge.getScheduler().createTaskBuilder()
                        .delay(plugin.loader().config().getWaitTime(), TimeUnit.SECONDS)
                        .execute(() -> plugin.getAttempts().remove(player.getName())).submit(plugin);
                return;
            }

            if (account.checkPassword(plugin, userInput)) {
                plugin.getAttempts().remove(player.getName());
                account.setOnline(true);
                //update the ip
                account.setIp(IPUtil.getPlayerIP(player));

                player.sendMessage(plugin.loader().getTextConfig().getLoggedIn());
                Sponge.getScheduler().createTaskBuilder()
                        .execute(() -> plugin.getProtectionManager().unprotect(player))
                        .submit(plugin);

                //flushes the ip update
                plugin.getDatabase().save(account);
                if (plugin.loader().config().isUpdateLoginStatus()) {
                    plugin.getDatabase().flushLoginStatus(account, true);
                }
            } else {
                attempts++;
                plugin.getAttempts().put(player.getName(), attempts);

                player.sendMessage(plugin.loader().getTextConfig().getIncorrectPassword());
            }
        } catch (Exception ex) {
            plugin.getLogger().error("Unexpected error while password checking", ex);
            player.sendMessage(plugin.loader().getTextConfig().getErrorCommandMessage());
        }
    }
}
