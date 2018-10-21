package org.soraworld.account.tasks;

import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;
import org.soraworld.account.util.Hash;
import org.soraworld.account.util.IPUtil;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import java.util.UUID;

import static org.soraworld.account.manager.AccountManager.getAccount;

public class RegisterTask implements Runnable {

    private final Player player;
    private final String pswdhash;
    private final AccountManager manager;

    // TODO 添加 email 参数
    public RegisterTask(AccountManager manager, Player player, String password) {
        this.player = player;
        this.pswdhash = Hash.hash(password);
        this.manager = manager;
    }

    public void run() {
        UUID uuid = player.getUniqueId();
        Account account;
        if (manager.enableDB()) {
            account = manager.pullAccount(uuid);
            if (account == null && manager.fallBack()) account = getAccount(uuid);
        } else account = getAccount(uuid);

        if (account == null || !account.isRegistered()) {
            int regByIp = manager.getRegistrationsCount(IPUtil.getPlayerIP(player));
            if (manager.getMaxIpReg() >= 1 && regByIp >= manager.getMaxIpReg()) {
                manager.sendKey(player, "MaxIpRegMessage");
                return;
            }
            if (account == null) account = new Account();
            try {
                account.setUUID(uuid);
                account.setUsername(player.getName());
                account.setPassword(pswdhash);
                if (!manager.createAccount(account, true)) {
                    return;
                }
                manager.sendKey(player, "AccountCreated");
                // 必须等注册成功才能设置这一步
                account.setRegistered(true);
                account.setOnline(true);
                if (manager.updateLoginStatus()) {
                    manager.flushLoginStatus(account, true);
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
