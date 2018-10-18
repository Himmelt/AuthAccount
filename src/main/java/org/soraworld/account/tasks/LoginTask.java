package org.soraworld.account.tasks;

import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;
import org.soraworld.account.util.IPUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;

import java.util.concurrent.TimeUnit;

public class LoginTask implements Runnable {

    private final Player player;
    private final String userInput;
    private final AccountManager manager;

    public LoginTask(AccountManager manager, Player player, String password) {
        this.manager = manager;
        this.player = player;
        this.userInput = password;
    }

    @Override
    public void run() {
        Account account = manager.getDatabase().loadAccount(player);
        if (account == null) {
            manager.sendKey(player, "AccountNotFound");
            return;
        }

        try {
            Integer attempts = manager.getAttempts().computeIfAbsent(player.getName(), (playerName) -> 0);
            if (attempts > manager.general.maxAttempts) {
                manager.sendKey(player, "MaxAttemptsMessage");
                // TODO CoolDown Input
                Sponge.getScheduler().createTaskBuilder()
                        .delay(manager.general.waitTime, TimeUnit.SECONDS)
                        .execute(() -> manager.getAttempts().remove(player.getName())).submit(manager);
                return;
            }

            if (account.checkPassword(userInput)) {
                manager.getAttempts().remove(player.getName());
                account.setOnline(true);
                //update the ip
                account.setIp(IPUtil.getPlayerIP(player));
                manager.sendKey(player, "LoggedIn");
                Sponge.getScheduler().createTaskBuilder()
                        .execute(() -> manager.unprotect(player))
                        .submit(manager);

                //flushes the ip update
                manager.getDatabase().save(account);
                if (manager.general.updateLoginStatus) {
                    manager.getDatabase().flushLoginStatus(account, true);
                }
            } else {
                attempts++;
                manager.getAttempts().put(player.getName(), attempts);
                manager.sendKey(player, "IncorrectPassword");
            }
        } catch (Exception e) {
            if (manager.isDebug()) e.printStackTrace();
            manager.consoleKey("Unexpected error while password checking");
        }
    }
}
