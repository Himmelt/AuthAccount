package org.soraworld.account.tasks;

import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;
import org.soraworld.account.util.IPUtil;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

public class RegisterTask implements Runnable {

    private final Player player;
    private final String password;
    private final AccountManager manager;

    public RegisterTask(AccountManager manager, Player player, String password) {
        this.player = player;
        this.password = password;
        this.manager = manager;
    }

    public void run() {
        if (manager.database.loadAccount(player) == null) {
            int regByIp = manager.database.getRegistrationsCount(IPUtil.getPlayerIP(player));
            if (manager.general.maxIpReg >= 1 && regByIp >= manager.general.maxIpReg) {
                manager.sendKey(player, "MaxIpRegMessage");
                return;
            }

            try {
                Account account = new Account(player, password);
                if (!manager.database.createAccount(account, true)) {
                    return;
                }
                manager.sendKey(player, "AccountCreated");
                account.setOnline(true);
                if (manager.general.updateLoginStatus) {
                    manager.database.flushLoginStatus(account, true);
                }

                Task.builder()
                        .execute(() -> manager.unprotect(player))
                        .submit(manager.getPlugin());
            } catch (Exception ex) {
                if (manager.isDebug()) ex.printStackTrace();
                manager.console("Error creating hash");
                manager.sendKey(player, "ErrorCommandMessage");
            }
        } else manager.sendKey(player, "AccountAlreadyExist");
    }
}
