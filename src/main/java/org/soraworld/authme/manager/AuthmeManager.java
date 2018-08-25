package org.soraworld.authme.manager;

import org.soraworld.violet.manager.SpongeManager;
import org.soraworld.violet.plugin.SpongePlugin;
import org.soraworld.violet.util.ChatColor;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public class AuthmeManager extends SpongeManager {
    /**
     * 实例化管理器.
     *
     * @param plugin 插件实例
     * @param path   配置保存路径
     */
    public AuthmeManager(SpongePlugin plugin, Path path) {
        super(plugin, path);
    }

    @Nonnull
    public ChatColor defChatColor() {
        return ChatColor.AQUA;
    }

    public void beforeLoad() {
    }

    public void afterLoad() {
    }
}
