package org.soraworld.authme.command;

import org.soraworld.violet.command.SpongeCommand;
import org.soraworld.violet.manager.SpongeManager;

public class CommandAuthme extends SpongeCommand.CommandViolet {
    /**
     * 实例化.
     *
     * @param perm       权限
     * @param onlyPlayer 是否仅玩家执行
     * @param manager    管理器
     * @param aliases    别名
     */
    public CommandAuthme(String perm, boolean onlyPlayer, SpongeManager manager, String... aliases) {
        super(perm, onlyPlayer, manager, aliases);
    }
}
