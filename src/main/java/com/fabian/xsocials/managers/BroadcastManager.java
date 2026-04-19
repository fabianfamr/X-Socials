package com.fabian.xsocials.managers;

import com.fabian.xsocials.XSocials;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BroadcastManager {

    private final XSocials plugin;
    private File broadcastsFile;
    private YamlConfiguration broadcastsConfig;
    private List<List<String>> broadcastGroups;
    private BukkitTask broadcastTask;
    private int currentIndex = 0;
    private final Random random = new Random();

    public BroadcastManager(XSocials plugin) {
        this.plugin = plugin;
        setupFile();
        startTask();
    }

    private void setupFile() {
        broadcastsFile = new File(plugin.getDataFolder(), "broadcasts.yml");
        if (!broadcastsFile.exists()) {
            plugin.saveResource("broadcasts.yml", false);
        }
        broadcastsConfig = YamlConfiguration.loadConfiguration(broadcastsFile);
        
        broadcastGroups = new ArrayList<>();
        for (String key : broadcastsConfig.getKeys(false)) {
            if (broadcastsConfig.isList(key + ".message")) {
                broadcastGroups.add(broadcastsConfig.getStringList(key + ".message"));
            }
        }
    }

    public void startTask() {
        if (broadcastTask != null) {
            broadcastTask.cancel();
        }

        if (!plugin.getConfig().getBoolean("broadcasts.enable", true) || broadcastGroups.isEmpty()) {
            return;
        }

        long interval = plugin.getConfig().getLong("broadcasts.interval", 300) * 20L;
        
        if (isFolia()) {
            // Folia: Use GlobalRegionScheduler via reflection to compile with Spigot API
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                scheduler.getClass().getMethod("runAtFixedRate", 
                    org.bukkit.plugin.Plugin.class, 
                    java.util.function.Consumer.class, 
                    long.class, 
                    long.class
                ).invoke(scheduler, plugin, (java.util.function.Consumer<Object>) task -> sendBroadcast(), 20L, interval);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to start Folia scheduler: " + e.getMessage());
            }
        } else {
            // Standard: Spigot/Paper/Bukkit
            broadcastTask = Bukkit.getScheduler().runTaskTimer(plugin, this::sendBroadcast, interval, interval);
        }
    }

    private void sendBroadcast() {
        if (broadcastGroups.isEmpty()) return;

        List<String> messages;
        if (plugin.getConfig().getBoolean("broadcasts.random", false)) {
            messages = broadcastGroups.get(random.nextInt(broadcastGroups.size()));
        } else {
            messages = broadcastGroups.get(currentIndex);
            currentIndex = (currentIndex + 1) % broadcastGroups.size();
        }

        boolean hasSpigot = isSpigot();

        for (String msg : messages) {
            msg = org.bukkit.ChatColor.translateAlternateColorCodes('&', msg);
            // Handle [hover="..."] tags if Spigot is available
            if (msg.contains("[hover=\"") && hasSpigot) {
                parseAndSendInteractiveMessage(msg);
            } else {
                // Remove tags if on bare Bukkit or if no tags found
                String plainMsg = msg.replaceAll("\\[hover=\".*?\"\\](.*?)\\[/hover\\]", "$1");
                Bukkit.broadcastMessage(plainMsg);
            }
        }
    }

    private void parseAndSendInteractiveMessage(String msg) {
        // Simple regex-less parser for [hover="tooltip"]text[/hover]
        net.md_5.bungee.api.chat.TextComponent finalComponent = new net.md_5.bungee.api.chat.TextComponent("");
        
        int lastPos = 0;
        int hoverStart = msg.indexOf("[hover=\"");
        
        while (hoverStart != -1) {
            // Add text before hover
            if (hoverStart > lastPos) {
                finalComponent.addExtra(msg.substring(lastPos, hoverStart));
            }
            
            int tooltipEnd = msg.indexOf("\"]", hoverStart + 8);
            if (tooltipEnd == -1) break;
            
            String tooltip = msg.substring(hoverStart + 8, tooltipEnd);
            int tagClose = msg.indexOf("[/hover]", tooltipEnd + 2);
            if (tagClose == -1) break;
            
            String content = msg.substring(tooltipEnd + 2, tagClose);
            
            net.md_5.bungee.api.chat.TextComponent hoverPart = new net.md_5.bungee.api.chat.TextComponent(content);
            setHoverEvent(hoverPart, tooltip);
            
            finalComponent.addExtra(hoverPart);
            lastPos = tagClose + 8;
            hoverStart = msg.indexOf("[hover=\"", lastPos);
        }
        
        // Add remaining text
        if (lastPos < msg.length()) {
            finalComponent.addExtra(msg.substring(lastPos));
        }
        
        Bukkit.getOnlinePlayers().forEach(p -> p.spigot().sendMessage(finalComponent));
        Bukkit.getConsoleSender().sendMessage(finalComponent.toLegacyText());
    }

    @SuppressWarnings("deprecation")
    private void setHoverEvent(net.md_5.bungee.api.chat.TextComponent component, String text) {
        try {
            // Modern API (1.16+)
            Class<?> textClass = Class.forName("net.md_5.bungee.api.chat.hover.content.Text");
            Object textContent = textClass.getConstructor(String.class).newInstance(text);
            Object hoverEvent = net.md_5.bungee.api.chat.HoverEvent.class.getConstructor(
                net.md_5.bungee.api.chat.HoverEvent.Action.class, 
                java.util.List.class
            ).newInstance(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, 
                java.util.Collections.singletonList(textContent)
            );
            component.setHoverEvent((net.md_5.bungee.api.chat.HoverEvent) hoverEvent);
        } catch (Exception e) {
            // Legacy API (1.8 - 1.15)
            component.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, 
                new net.md_5.bungee.api.chat.BaseComponent[]{new net.md_5.bungee.api.chat.TextComponent(text)}
            ));
        }
    }

    private boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean isSpigot() {
        try {
            Class.forName("org.bukkit.entity.Player$Spigot");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void reload() {
        setupFile();
        startTask();
    }
}
