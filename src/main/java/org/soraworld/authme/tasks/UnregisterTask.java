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

import org.soraworld.authme.Authme;
import org.spongepowered.api.command.CommandSource;

import java.util.UUID;

public class UnregisterTask implements Runnable {

    private final Authme plugin = Authme.getInstance();
    private final CommandSource src;

    private final Object accountIndentifer;

    public UnregisterTask(CommandSource src, UUID uuid) {
        this.src = src;

        this.accountIndentifer = uuid;
    }

    public UnregisterTask(CommandSource src, String playerName) {
        this.src = src;

        this.accountIndentifer = playerName;
    }

    @Override
    public void run() {
        boolean accountFound;
        if (accountIndentifer instanceof String) {
            accountFound = plugin.getDatabase().deleteAccount((String) accountIndentifer);
        } else {
            accountFound = plugin.getDatabase().deleteAccount((UUID) accountIndentifer);
        }

        if (accountFound) {
            src.sendMessage(plugin.getCfgLoader().getTextConfig().getAccountDeleted(accountIndentifer.toString()));
        } else {
            src.sendMessage(plugin.getCfgLoader().getTextConfig().getAccountNotFound());
        }
    }
}
