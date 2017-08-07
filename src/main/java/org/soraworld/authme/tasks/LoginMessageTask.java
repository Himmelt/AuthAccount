package org.soraworld.authme.tasks;

import org.soraworld.authme.Account;
import org.soraworld.authme.Authme;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import java.util.function.Consumer;

public class LoginMessageTask implements Consumer<Task> {

    private final Authme plugin = Authme.getInstance();
    private final Player player;

    public LoginMessageTask(Player player) {
        this.player = player;
    }

    @Override
    public void accept(Task task) {
        Account account = plugin.getDatabase().getAccountIfPresent(player);
        if (account != null && account.isOnline()) {
            task.cancel();
            return;
        }

        if (account == null) {
            player.sendMessage(plugin.loader().getTextConfig().getNotRegisteredMessage());
        } else {
            player.sendMessage(plugin.loader().getTextConfig().getNotLoggedInMessage());
        }
    }
}
