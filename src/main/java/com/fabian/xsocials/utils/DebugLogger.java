package com.fabian.xsocials.utils;

import com.fabian.xsocials.XSocials;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * DebugLogger - Static utility for debug logging with two modes:
 * <p>
 * - Config debug (debug: true in config.yml): messages go to CONSOLE only.
 * - Command debug (/xsocials debug): messages go to the PLAYER who toggled it.
 * <p>
 * When both are active, only the player receives command-triggered debug messages.
 * Config debug always outputs to console independently.
 */
public final class DebugLogger {

    private static final String PLUGIN_NAME = "X-Socials";
    private static final String PREFIX = "&8[&bDEBUG&8] &f[" + PLUGIN_NAME + "&f]&r &7";

    private DebugLogger() {
        // utility class – no instances
    }

    /**
     * Checks if debug mode is active (either via config or via command).
     */
    private static boolean isDebugEnabled() {
        XSocials instance = XSocials.getInstance();
        if (instance == null) return false;
        try {
            boolean configDebug = instance.getConfig().getBoolean("debug", false);
            return configDebug || instance.debugPlayer != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if config-based debug is active (console output).
     */
    private static boolean isConfigDebug() {
        XSocials instance = XSocials.getInstance();
        if (instance == null) return false;
        try {
            return instance.getConfig().getBoolean("debug", false);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the player who enabled debug via command, or null.
     */
    private static Player getDebugPlayer() {
        XSocials instance = XSocials.getInstance();
        if (instance == null || instance.debugPlayer == null) return null;
        return Bukkit.getPlayer(instance.debugPlayer);
    }

    /**
     * Log a debug message without a category.
     */
    public static void debug(String message) {
        if (!isDebugEnabled()) return;
        send(message);
    }

    /**
     * Log a debug message with a category label.
     */
    public static void debug(String category, String message) {
        if (!isDebugEnabled()) return;
        send("[" + category + "] " + message);
    }

    /**
     * Log a debug message with a category and an associated throwable stack-trace.
     */
    public static void debug(String category, String message, Throwable throwable) {
        if (!isDebugEnabled()) return;
        send("[" + category + "] " + message);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }

    /**
     * Routes the message to the appropriate recipient:
     * - If a player enabled debug via command -> send to that player
     * - If debug is enabled via config -> send to console
     * - If both -> player gets it (config debug still goes to console independently via isConfigDebug)
     */
    private static void send(String message) {
        XSocials instance = XSocials.getInstance();
        if (instance == null) return;

        String formatted = ChatColor.translateAlternateColorCodes('&', PREFIX + message);

        // Player debug via command
        if (instance.debugPlayer != null) {
            Player debugPlayer = Bukkit.getPlayer(instance.debugPlayer);
            if (debugPlayer != null && debugPlayer.isOnline()) {
                debugPlayer.sendMessage(formatted);
                return;
            } else {
                // Player went offline, clean up
                instance.debugPlayer = null;
            }
        }

        // Config debug -> console only
        if (isConfigDebug()) {
            Bukkit.getConsoleSender().sendMessage(formatted);
        }
    }
}