package org.soraworld.account.tasks;

import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;
import org.soraworld.account.util.IPUtil;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import java.util.UUID;

import static org.soraworld.account.manager.AccountManager.getAccount;
import static org.soraworld.account.util.Pswd.encode;

public class RegisterTask implements Runnable {

    private final Player player;
    private final String pswdhash;
    private final AccountManager manager;

    public RegisterTask(AccountManager manager, Player player, String password) {
        this.player = player;
        this.pswdhash = encode(password);
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
            int regIp = IPUtil.getPlayerIP(player);
            int count = manager.getRegistrationsCount(regIp);
            if (manager.getMaxIpReg() >= 1 && count >= manager.getMaxIpReg()) {
                manager.sendKey(player, "MaxIpRegMessage");
                return;
            }
            if (account == null) account = new Account();
            try {
                account.setUUID(uuid);
                account.setUsername(player.getName());
                account.setPassword(pswdhash);
                account.setOnline(true);
                account.setIp(regIp);
                // TODO 数据库操作失败 fallback
                if (manager.enableDB() && !manager.createAccount(account, true)) {
                    return;
                }
                manager.sendKey(player, "AccountCreated");
                // 必须等注册成功才能设置这一步
                // TODO 注册 IP 啥时候设置？
                account.setRegistered(true);
                System.out.println("reg task:" + account.hashCode() + "|" + account.isRegistered());

                if (manager.updateLoginStatus()) {
                    manager.flushLoginStatus(account, true);
                }

                // TODO 同步
                if (manager.enableDB()) getAccount(uuid).sync(account);
                Task.builder().execute(() -> manager.unprotect(player))
                        .submit(manager.getPlugin());
            } catch (Exception e) {
                if (manager.isDebug()) e.printStackTrace();
                manager.console("Error creating hash");
                manager.sendKey(player, "ErrorCommandMessage");
            }
        } else manager.sendKey(player, "AccountAlreadyExist");
    }
}
