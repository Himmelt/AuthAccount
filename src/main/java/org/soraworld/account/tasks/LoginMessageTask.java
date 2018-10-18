package org.soraworld.account.tasks;

import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import java.util.function.Consumer;

public class LoginMessageTask implements Consumer<Task> {

    private final AccountManager manager;
    private final Player player;

    public LoginMessageTask(AccountManager manager, Player player) {
        this.manager = manager;
        this.player = player;
    }

    @Override
    public void accept(Task task) {
        Account account = manager.getAccountIfPresent(player);
        if (account != null && account.isOnline()) {
            task.cancel();
            return;
        }

        if (account == null) {
            manager.sendKey(player, "NotRegisteredMessage");
        } else {
            manager.sendKey(player, "NotLoggedInMessage");
        }
    }
}
