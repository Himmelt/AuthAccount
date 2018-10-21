package org.soraworld.account.tasks;

import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;
import org.soraworld.account.util.IPUtil;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.soraworld.account.manager.AccountManager.getAccount;

public class LoginTask implements Runnable {

    private final Player player;
    private final String password;
    private final AccountManager manager;

    public LoginTask(AccountManager manager, Player player, String password) {
        this.player = player;
        this.manager = manager;
        this.password = password;
    }

    public void run() {
        UUID uuid = player.getUniqueId();
        Account account;
        if (manager.enableDB()) {
            account = manager.pullAccount(uuid);
            if (account == null && manager.fallBack()) account = getAccount(uuid);
        } else account = getAccount(uuid);

        if (account != null && account.isRegistered()) {
            // TODO 验证登陆时，从 DB 拉取数据 DB --> NBT 并更新缓存
            try {
                Integer attempts = manager.getAttempts().computeIfAbsent(uuid, id -> 0);
                if (attempts > manager.maxAttempts()) {
                    manager.sendKey(player, "MaxAttemptsMessage");
                    // TODO 超过尝试次数的冷却时间
                    Task.builder().delay(manager.waitTime(), TimeUnit.SECONDS)
                            .execute(() -> manager.getAttempts().remove(uuid)).submit(manager);
                    return;
                }

                if (account.checkPassword(password)) {
                    // 验证通过
                    manager.getAttempts().remove(uuid);
                    account.setOnline(true);
                    //update the ip
                    account.setIp(IPUtil.getPlayerIP(player));

                    manager.sendKey(player, "LoggedIn");
                    Task.builder().execute(() -> manager.unprotect(player)).submit(manager.getPlugin());
                    if (manager.enableDB()) {
                        getAccount(uuid).sync(account);// 更新缓存 DB --> NBT
                        manager.pushAccount(account);// 更新数据库状态 NBT --> DB
                        if (manager.updateLoginStatus()) {
                            manager.flushLoginStatus(account, true);
                        }
                    }
                } else {
                    attempts++;
                    manager.getAttempts().put(uuid, attempts);
                    manager.sendKey(player, "IncorrectPassword");
                }
            } catch (Exception e) {
                if (manager.isDebug()) e.printStackTrace();
                manager.consoleKey("Unexpected error while password checking");
            }
        } else manager.sendKey(player, "pleaseRegister");
    }
}
