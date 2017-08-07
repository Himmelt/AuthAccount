package org.soraworld.authme.tasks;

import org.soraworld.authme.Account;
import org.soraworld.authme.Authme;
import org.soraworld.authme.hasher.TOTP;
import org.soraworld.authme.util.IPUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import java.net.MalformedURLException;
import java.net.URL;

public class RegisterTask implements Runnable {

    private final Authme authme = Authme.getInstance();

    private final Player player;
    private final String password;

    public RegisterTask(Player player, String password) {
        this.player = player;
        this.password = password;
    }

    @Override
    public void run() {
        if (authme.getDatabase().loadAccount(player) == null) {
            int regByIp = authme.getDatabase().getRegistrationsCount(IPUtil.getPlayerIP(player));
            if (authme.loader().config().getMaxIpReg() >= 1 && regByIp >= authme.loader().config().getMaxIpReg()) {
                player.sendMessage(authme.loader().getTextConfig().getMaxIpRegMessage());
                return;
            }

            try {
                String hashedPassword = authme.getHasher().hash(password);
                Account createdAccount = new Account(player, hashedPassword);
                if (!authme.getDatabase().createAccount(createdAccount, true)) {
                    return;
                }

                //thread-safe, because it's immutable after config load
                if ("totp".equalsIgnoreCase(authme.loader().config().getHashAlgo())) {
                    sendTotpHint(hashedPassword);
                }

                player.sendMessage(authme.loader().getTextConfig().getAccountCreated());
                createdAccount.setOnline(true);
                if (authme.loader().config().isUpdateLoginStatus()) {
                    authme.getDatabase().flushLoginStatus(createdAccount, true);
                }

                Sponge.getScheduler().createTaskBuilder()
                        .execute(() -> authme.getProtectionManager().unprotect(player))
                        .submit(authme);
            } catch (Exception ex) {
                authme.getLogger().error("Error creating hash", ex);
                player.sendMessage(authme.loader().getTextConfig().getErrorCommandMessage());
            }
        } else {
            player.sendMessage(authme.loader().getTextConfig().getAccountAlreadyExists());
        }
    }

    private void sendTotpHint(String secretCode) {
        //I assume this thread-safe, because PlayerChat is also in an async task
        String host = Sponge.getServer().getBoundAddress().get().getAddress().getCanonicalHostName();
        try {
            URL barcodeUrl = new URL(TOTP.getQRBarcodeURL(player.getName(), host, secretCode));
            player.sendMessage(Text.builder()
                    .append(authme.loader().getTextConfig().getKeyGenerated())
                    .build());
            player.sendMessage(Text.builder(secretCode)
                    .color(TextColors.GOLD)
                    .append(Text.of(TextColors.DARK_BLUE, " / "))
                    .append(Text.builder()
                            .append(authme.loader().getTextConfig().getScanQr())
                            .onClick(TextActions.openUrl(barcodeUrl))
                            .build())
                    .build());
        } catch (MalformedURLException ex) {
            authme.getLogger().error("Malformed totp url link", ex);
        }
    }
}
