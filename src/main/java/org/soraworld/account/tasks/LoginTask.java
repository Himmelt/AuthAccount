package org.soraworld.account.tasks;

import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;
import org.soraworld.account.util.IPUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;

import java.util.concurrent.TimeUnit;

import static org.soraworld.account.config.Database.getAccount;

public class LoginTask implements Runnable {

    private final Player player;
    private final String userInput;
    private final AccountManager manager;

    public LoginTask(AccountManager manager, Player player, String password) {
        this.manager = manager;
        this.player = player;
        this.userInput = password;
    }

    public void run() {
        // TODO 异步 有问题
        Account account = manager.enableDB() ? manager.pullAccount(player.getUniqueId()) : getAccount(player);
        // TODO 验证登陆时，从 DB 拉取数据 DB --> NBT 并更新缓存
        if (account == null || !account.isRegistered()) {
            // TODO HINT 未注册
            manager.sendKey(player, "AccountNotFound");
            return;
        }

        try {
            Integer attempts = manager.getAttempts().computeIfAbsent(player.getName(), (playerName) -> 0);
            if (attempts > manager.maxAttempts()) {
                manager.sendKey(player, "MaxAttemptsMessage");
                // TODO 超过尝试次数的冷却时间
                Sponge.getScheduler().createTaskBuilder()
                        .delay(manager.waitTime(), TimeUnit.SECONDS)
                        .execute(() -> manager.getAttempts().remove(player.getName())).submit(manager);
                return;
            }

            if (account.checkPassword(userInput)) {
                // 验证通过
                manager.getAttempts().remove(player.getName());
                account.setOnline(true);
                //update the ip
                account.setIp(IPUtil.getPlayerIP(player));
                manager.sendKey(player, "LoggedIn");
                Sponge.getScheduler().createTaskBuilder()
                        .execute(() -> manager.unprotect(player))
                        .submit(manager);
                if (manager.enableDB()) {
                    // DB --> NBT
                    getAccount(player).sync(player, account);
                    //flushes the ip update
                    manager.pushAccount(account);
                    if (manager.updateLoginStatus()) {
                        manager.flushLoginStatus(account, true);
                    }
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
