package com.fabian.xsocials.utils;

import com.fabian.xsocials.XSocials;
import com.fabian.xsocials.managers.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private final XSocials plugin;
    private final int resourceId;
    private String latestVersion;
    private boolean updateAvailable;

    public UpdateChecker(XSocials plugin) {
        this.plugin = plugin;
        this.resourceId = 132425;
        this.updateAvailable = false;
    }

    public void checkForUpdates() {
        checkForUpdates(null);
    }

    public void checkForUpdates(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String currentVersion = plugin.getDescription().getVersion();

                // Spigot API for resource versions
                URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Fabian/X-Socials/" + currentVersion);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String version = reader.readLine();
                reader.close();

                this.latestVersion = version;
                LanguageManager lang = plugin.getLanguageManager();

                if (latestVersion != null && isNewer(currentVersion, latestVersion)) {
                    this.updateAvailable = true;

                    if (sender != null) {
                        sender.sendMessage(lang.getPrefix() + " "
                                + lang.getMessage("update-available", currentVersion, latestVersion));
                        sender.sendMessage(
                                lang.getPrefix() + " " + lang.getMessage("update-download", getDownloadUrl()));
                    } else {
                        Bukkit.getConsoleSender()
                                .sendMessage(lang.getPrefix() + " "
                                        + lang.getMessage("update-available", currentVersion, latestVersion));
                        Bukkit.getConsoleSender().sendMessage(
                                lang.getPrefix() + " " + lang.getMessage("update-download", getDownloadUrl()));
                    }
                } else {
                    if (sender != null) {
                        sender.sendMessage(lang.getPrefix() + " " + lang.getMessage("update-current"));
                    } else {
                        // Console startup message (same as player but without prefix per user request
                        // logic or just message)
                        // User said "Es la misma del jugador", so we use update-current
                        plugin.logWithConfigPrefix(lang.getMessage("update-current"));
                    }
                }

            } catch (Exception e) {
                // Only show error if explicitly requested by a player/sender
                if (sender != null) {
                    sender.sendMessage(plugin.getLanguageManager().getPrefix() + " "
                            + plugin.getLanguageManager().getMessage("update-error"));
                }
                // Silently fail for automatic checks (console) as requested
            }
        });
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getDownloadUrl() {
        return "https://www.spigotmc.org/resources/" + resourceId + "/";
    }

    private boolean isNewer(String current, String latest) {
        String[] currentParts = current.replace("v", "").split("\\.");
        String[] latestParts = latest.replace("v", "").split("\\.");
        int length = Math.max(currentParts.length, latestParts.length);
        for (int i = 0; i < length; i++) {
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
            if (latestPart > currentPart)
                return true;
            if (latestPart < currentPart)
                return false;
        }
        return false;
    }
}
