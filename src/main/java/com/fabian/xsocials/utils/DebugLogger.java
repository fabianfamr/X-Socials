package com.fabian.xsocials.utils;

import com.fabian.xsocials.XSocials;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

/**
 * Static debug logger that reads the "debug" flag from config.yml.
 * Only outputs when {@code debug: true} is set in the plugin configuration.
 */
public final class DebugLogger {

    private static final String PREFIX = "&8[&bX-Socials&8] &b[DEBUG] &7";

    private DebugLogger() {
        // utility class – no instances
    }

    /**
     * Checks whether debug mode is enabled in the current config.
     * Includes null-safety for early init / hot-reload scenarios.
     */
    private static boolean isDebugEnabled() {
        XSocials instance = XSocials.getInstance();
        if (instance == null) return false;
        try {
            return instance.getConfig().getBoolean("debug", false);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Log a debug message without a category.
     */
    public static void debug(String message) {
        if (isDebugEnabled()) {
            Bukkit.getConsoleSender().sendMessage(
                    ChatColor.translateAlternateColorCodes('&', PREFIX + message));
        }
    }

    /**
     * Log a debug message with a category label.
     */
    public static void debug(String category, String message) {
        if (isDebugEnabled()) {
            Bukkit.getConsoleSender().sendMessage(
                    ChatColor.translateAlternateColorCodes('&',
                            PREFIX + "&f[" + category + "&f] &7" + message));
        }
    }

    /**
     * Log a debug message with a category and an associated throwable stack-trace.
     */
    public static void debug(String category, String message, Throwable throwable) {
        if (isDebugEnabled()) {
            Bukkit.getConsoleSender().sendMessage(
                    ChatColor.translateAlternateColorCodes('&',
                            PREFIX + "&f[" + category + "&f] &7" + message));
            throwable.printStackTrace();
        }
    }
}