package com.inpuzah.discordRelay.util;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public class FloodgateUtil {
    private static final boolean floodgatePresent;
    private static Class<?> apiClass;
    private static Method getInstance;
    private static Method isFloodgatePlayer;

    static {
        boolean present = false;
        try {
            apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            getInstance = apiClass.getMethod("getInstance");
            isFloodgatePlayer = apiClass.getMethod("isFloodgatePlayer", UUID.class);
            present = true;
        } catch (Throwable ignored) {}
        floodgatePresent = present;
    }

    public static String bedrockTagIfPresent(Player p, String tag) {
        if (!floodgatePresent) return "";
        try {
            Object api = getInstance.invoke(null);
            boolean is = (boolean) isFloodgatePlayer.invoke(api, p.getUniqueId());
            return is ? tag : "";
        } catch (Throwable t) {
            return "";
        }
    }
}
