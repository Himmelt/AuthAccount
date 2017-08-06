package org.soraworld.authme.tasks;

import org.soraworld.authme.Account;
import org.soraworld.authme.Authme;
import org.soraworld.authme.util.IPUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.entity.living.player.Player;

import java.util.concurrent.TimeUnit;

public class LoginTask implements Runnable {

    private final Authme plugin = Authme.getInstance();

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
            player.sendMessage(plugin.getCfgLoader().getTextConfig().getAccountNotFound());
            return;
        }

        try {
            Integer attempts = plugin.getAttempts().computeIfAbsent(player.getName(), (playerName) -> 0);
            if (attempts > plugin.getCfgLoader().getConfig().getMaxAttempts()) {
                player.sendMessage(plugin.getCfgLoader().getTextConfig().getMaxAttemptsMessage());
                String lockCommand = plugin.getCfgLoader().getConfig().getLockCommand();
                if (lockCommand != null && !lockCommand.isEmpty()) {
                    ConsoleSource console = Sponge.getServer().getConsole();
                    plugin.getGame().getCommandManager().process(console, lockCommand);
                }

                Sponge.getScheduler().createTaskBuilder()
                        .delay(plugin.getCfgLoader().getConfig().getWaitTime(), TimeUnit.SECONDS)
                        .execute(() -> plugin.getAttempts().remove(player.getName())).submit(plugin);
                return;
            }

            if (account.checkPassword(plugin, userInput)) {
                plugin.getAttempts().remove(player.getName());
                account.setOnline(true);
                //update the ip
                account.setIp(IPUtil.getPlayerIP(player));

                player.sendMessage(plugin.getCfgLoader().getTextConfig().getLoggedIn());
                Sponge.getScheduler().createTaskBuilder()
                        .execute(() -> plugin.getProtectionManager().unprotect(player))
                        .submit(plugin);

                //flushes the ip update
                plugin.getDatabase().save(account);
                if (plugin.getCfgLoader().getConfig().isUpdateLoginStatus()) {
                    plugin.getDatabase().flushLoginStatus(account, true);
                }
            } else {
                attempts++;
                plugin.getAttempts().put(player.getName(), attempts);

                player.sendMessage(plugin.getCfgLoader().getTextConfig().getIncorrectPassword());
            }
        } catch (Exception ex) {
            plugin.getLogger().error("Unexpected error while password checking", ex);
            player.sendMessage(plugin.getCfgLoader().getTextConfig().getErrorCommandMessage());
        }
    }
}
