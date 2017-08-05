package org.soraworld.authme.util;

import org.spongepowered.api.entity.living.player.Player;

public class IPUtil {

    public static String byte2ipv4(byte[] ip) {
        if (ip != null && ip.length == 4) {
            return ip[0] + "." + ip[1] + "." + ip[2] + "." + ip[3];
        } else {
            return "invalid-ip";
        }
    }

    public static String getPlayerIP(Player player) {
        byte[] ip = player.getConnection().getAddress().getAddress().getAddress();
        return byte2ipv4(ip);
    }
}
