package org.soraworld.account.util;

import org.spongepowered.api.entity.living.player.Player;

public class IPUtil {

    public static String byte2ipv4(byte[] ip) {
        if (ip != null && ip.length == 4) {
            return String.format("%d.%d.%d.%d", uint(ip[0]), uint(ip[1]), uint(ip[2]), uint(ip[3]));
        } else {
            return "invalid-ip";
        }
    }

    public static int uint(byte _byte) {
        return (int) _byte & 0xff;
    }

    public static String getPlayerIP(Player player) {
        return byte2ipv4(player.getConnection().getAddress().getAddress().getAddress());
    }
}
