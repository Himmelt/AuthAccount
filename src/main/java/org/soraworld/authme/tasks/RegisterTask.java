/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 games647 and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.soraworld.authme.tasks;

import org.soraworld.authme.Account;
import org.soraworld.authme.Authme;
import org.soraworld.authme.hasher.TOTP;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import java.net.MalformedURLException;
import java.net.URL;

public class RegisterTask implements Runnable {

    private final Authme plugin = Authme.getInstance();

    private final Player player;
    private final String password;

    public RegisterTask(Player player, String password) {
        this.player = player;
        this.password = password;
    }

    @Override
    public void run() {
        if (plugin.getDatabase().loadAccount(player) == null) {
            byte[] ipAddress = player.getConnection().getAddress().getAddress().getAddress();
            int regByIp = plugin.getDatabase().getRegistrationsCount(ipAddress);
            if (regByIp > plugin.getCfgLoader().getConfig().getMaxIpReg()) {
                player.sendMessage(plugin.getCfgLoader().getTextConfig().getMaxIpRegMessage());
                return;
            }

            try {
                String hashedPassword = plugin.getHasher().hash(password);
                Account createdAccount = new Account(player, hashedPassword);
                if (!plugin.getDatabase().createAccount(createdAccount, true)) {
                    return;
                }

                //thread-safe, because it's immutable after config load
                if ("totp".equalsIgnoreCase(plugin.getCfgLoader().getConfig().getHashAlgo())) {
                    sendTotpHint(hashedPassword);
                }

                player.sendMessage(plugin.getCfgLoader().getTextConfig().getAccountCreated());
                createdAccount.setLoggedIn(true);
                if (plugin.getCfgLoader().getConfig().isUpdateLoginStatus()) {
                    plugin.getDatabase().flushLoginStatus(createdAccount, true);
                }

                Sponge.getScheduler().createTaskBuilder()
                        .execute(() -> plugin.getProtectionManager().unprotect(player))
                        .submit(plugin);
            } catch (Exception ex) {
                plugin.getLogger().error("Error creating hash", ex);
                player.sendMessage(plugin.getCfgLoader().getTextConfig().getErrorCommandMessage());
            }
        } else {
            player.sendMessage(plugin.getCfgLoader().getTextConfig().getAccountAlreadyExists());
        }
    }

    private void sendTotpHint(String secretCode) {
        //I assume this thread-safe, because PlayerChat is also in an async task
        String host = Sponge.getServer().getBoundAddress().get().getAddress().getCanonicalHostName();
        try {
            URL barcodeUrl = new URL(TOTP.getQRBarcodeURL(player.getName(), host, secretCode));
            player.sendMessage(Text.builder()
                    .append(plugin.getCfgLoader().getTextConfig().getKeyGenerated())
                    .build());
            player.sendMessage(Text.builder(secretCode)
                    .color(TextColors.GOLD)
                    .append(Text.of(TextColors.DARK_BLUE, " / "))
                    .append(Text.builder()
                            .append(plugin.getCfgLoader().getTextConfig().getScanQr())
                            .onClick(TextActions.openUrl(barcodeUrl))
                            .build())
                    .build());
        } catch (MalformedURLException ex) {
            plugin.getLogger().error("Malformed totp url link", ex);
        }
    }
}
