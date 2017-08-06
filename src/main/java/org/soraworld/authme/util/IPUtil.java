package org.soraworld.authme.util;

import org.spongepowered.api.entity.living.player.Player;

public class IPUtil {

    public static String byte2ipv4(byte[] ip) {
        if (ip != null && ip.length == 4) {
            return String.format("%d.%d.%d.%d", Math.uint(ip[0]), Math.uint(ip[1]), Math.uint(ip[2]), Math.uint(ip[3]));
        } else {
            return "invalid-ip";
        }
    }

    public static String getPlayerIP(Player player) {
        return byte2ipv4(player.getConnection().getAddress().getAddress().getAddress());
    }
}
