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
package org.soraworld.authme.commands;

import com.google.common.collect.Lists;
import org.soraworld.authme.Authme;
import org.soraworld.authme.tasks.RegisterTask;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandPermissionException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

import java.util.Collection;
import java.util.List;

public class RegisterCommand implements CommandExecutor {

    private final Authme plugin = Authme.getInstance();

    @Override
    public CommandResult execute(CommandSource source, CommandContext args) throws CommandException {
        if (!(source instanceof Player)) {
            source.sendMessage(plugin.getCfgLoader().getTextConfig().getPlayersOnlyActionMessage());
            return CommandResult.success();
        }

        if (plugin.getCfgLoader().getConfig().isPlayerPermissions()
                && !source.hasPermission(plugin.getContainer().getId() + ".command.register")) {
            throw new CommandPermissionException();
        }

        //If the server is using TOTP, no password is required
        if (!args.hasAny("password")) {
            if ("totp".equals(plugin.getCfgLoader().getConfig().getHashAlgo())) {
                startTask(source, "");
            } else {
                source.sendMessage(plugin.getCfgLoader().getTextConfig().getTotpNotEnabledMessage());
            }

            return CommandResult.success();
        }

        Collection<String> passwords = args.getAll("password");
        List<String> indexPasswords = Lists.newArrayList(passwords);
        String password = indexPasswords.get(0);
        if (password.equals(indexPasswords.get(1))) {
            if (password.length() >= plugin.getCfgLoader().getConfig().getMinPasswordLength()) {
                //Check if the first two passwords are equal to prevent typos
                startTask(source, password);
            } else {
                source.sendMessage(plugin.getCfgLoader().getTextConfig().getTooShortPasswordMessage());
            }
        } else {
            source.sendMessage(plugin.getCfgLoader().getTextConfig().getUnequalPasswordsMessage());
        }

        return CommandResult.success();
    }

    private void startTask(CommandSource source, String password) {
        Sponge.getScheduler().createTaskBuilder()
                //we are executing a SQL Query which is blocking
                .async()
                .execute(new RegisterTask((Player) source, password))
                .name("Register Query")
                .submit(plugin);
    }
}
