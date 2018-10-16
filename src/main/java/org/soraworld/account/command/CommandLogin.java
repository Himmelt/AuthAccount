package org.soraworld.account.command;

import org.soraworld.violet.command.Args;
import org.soraworld.violet.command.SpongeCommand;
import org.soraworld.violet.manager.SpongeManager;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

public class CommandLogin extends SpongeCommand {
    public CommandLogin(String name, String perm, boolean onlyPlayer, SpongeManager manager, String... aliases) {
        super(name, perm, onlyPlayer, manager, aliases);
    }

    public void execute(Player player, Args args) {

    }

    public Text getUsage(CommandSource source) {
        return Text.of("/login <password>");
    }
}
