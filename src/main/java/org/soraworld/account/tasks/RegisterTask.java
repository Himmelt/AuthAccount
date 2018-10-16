package org.soraworld.account.tasks;

import org.soraworld.account.data.Account;
import org.soraworld.account.AuthAccount;
import org.soraworld.account.hasher.TOTP;
import org.soraworld.account.util.IPUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import java.net.MalformedURLException;
import java.net.URL;

public class RegisterTask implements Runnable {

    private final AuthAccount authAccount = AuthAccount.getInstance();

    private final Player player;
    private final String password;

    public RegisterTask(Player player, String password) {
        this.player = player;
        this.password = password;
    }

    @Override
    public void run() {
        if (authAccount.getDatabase().loadAccount(player) == null) {
            int regByIp = authAccount.getDatabase().getRegistrationsCount(IPUtil.getPlayerIP(player));
            if (authAccount.loader().config().getMaxIpReg() >= 1 && regByIp >= authAccount.loader().config().getMaxIpReg()) {
                player.sendMessage(authAccount.loader().getTextConfig().getMaxIpRegMessage());
                return;
            }

            try {
                String hashedPassword = authAccount.getHasher().hash(password);
                Account createdAccount = new Account(player, hashedPassword);
                if (!authAccount.getDatabase().createAccount(createdAccount, true)) {
                    return;
                }

                //thread-safe, because it's immutable after config load
                if ("totp".equalsIgnoreCase(authAccount.loader().config().getHashAlgo())) {
                    sendTotpHint(hashedPassword);
                }

                player.sendMessage(authAccount.loader().getTextConfig().getAccountCreated());
                createdAccount.setOnline(true);
                if (authAccount.loader().config().isUpdateLoginStatus()) {
                    authAccount.getDatabase().flushLoginStatus(createdAccount, true);
                }

                Sponge.getScheduler().createTaskBuilder()
                        .execute(() -> authAccount.getProtectionManager().unprotect(player))
                        .submit(authAccount);
            } catch (Exception ex) {
                authAccount.getLogger().error("Error creating hash", ex);
                player.sendMessage(authAccount.loader().getTextConfig().getErrorCommandMessage());
            }
        } else {
            player.sendMessage(authAccount.loader().getTextConfig().getAccountAlreadyExists());
        }
    }

    private void sendTotpHint(String secretCode) {
        //I assume this thread-safe, because PlayerChat is also in an async task
        String host = Sponge.getServer().getBoundAddress().get().getAddress().getCanonicalHostName();
        try {
            URL barcodeUrl = new URL(TOTP.getQRBarcodeURL(player.getName(), host, secretCode));
            player.sendMessage(Text.builder()
                    .append(authAccount.loader().getTextConfig().getKeyGenerated())
                    .build());
            player.sendMessage(Text.builder(secretCode)
                    .color(TextColors.GOLD)
                    .append(Text.of(TextColors.DARK_BLUE, " / "))
                    .append(Text.builder()
                            .append(authAccount.loader().getTextConfig().getScanQr())
                            .onClick(TextActions.openUrl(barcodeUrl))
                            .build())
                    .build());
        } catch (MalformedURLException ex) {
            authAccount.getLogger().error("Malformed totp url link", ex);
        }
    }
}
