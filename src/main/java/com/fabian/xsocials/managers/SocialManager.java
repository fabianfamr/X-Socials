package com.fabian.xsocials.managers;

import com.fabian.xsocials.XSocials;
import com.fabian.xsocials.commands.DynamicSocialCommand;
import com.fabian.xsocials.models.SocialNetwork;
import com.fabian.xsocials.utils.ConfigUpdater;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

public class SocialManager {

    private final XSocials plugin;
    private final Map<String, SocialNetwork> socialNetworks;
    private final File socialsFolder;
    private final Map<String, File> socialFiles; // Map social name -> file

    public SocialManager(XSocials plugin) {
        this.plugin = plugin;
        this.socialNetworks = new HashMap<>();
        this.socialFiles = new HashMap<>();
        this.socialsFolder = new File(plugin.getDataFolder(), "socials");

        // Create folder and save defaults
        saveDefaultConfig();

        // Cargar redes sociales
        loadSocialNetworks();
    }

    public void saveDefaultConfig() {
        if (!socialsFolder.exists()) {
            socialsFolder.mkdirs();

            String[] defaultSocials = {"discord.yml", "twitter.yml", "youtube.yml"};
            for (String social : defaultSocials) {
                plugin.saveResource("socials/" + social, false);
            }
        }

        ConfigUpdater.update(plugin, "broadcasts.yml", "broadcasts.yml");
    }

    public void loadSocialNetworks() {
        socialNetworks.clear();
        socialFiles.clear();

        File[] files = socialsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.log(org.bukkit.ChatColor.YELLOW
                    + "No se encontraron archivos de redes sociales en la carpeta 'socials'");
            return;
        }

        int loadedCount = 0;
        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                // Obtener la primera clave (nombre de la red social)
                Set<String> keys = config.getKeys(false);
                if (keys.isEmpty()) {
                    plugin.getLogger().warning("Archivo vacío: " + file.getName());
                    continue;
                }

                String socialName = keys.iterator().next();
                ConfigurationSection section = config.getConfigurationSection(socialName);

                if (section == null) {
                    plugin.getLogger().warning("Sección inválida en: " + file.getName());
                    continue;
                }

                String command = section.getString("command");
                List<String> aliases = section.getStringList("aliases");
                String permission = section.getString("permission", "");
                String link = section.getString("link", "");
                String hover = section.getString("hover", "");
                List<String> messages = section.getStringList("message");
                List<String> rewardCommands = section.getStringList("rewards.commands");
                boolean enabled = section.getBoolean("enabled", true);
                boolean registerCommand = section.getBoolean("register-command", true);
                String itemType = section.getString("gui-item.type", "BLOCK");
                String itemValue = section.getString("gui-item.material", "PAPER");

                long cooldownSeconds = section.getLong("cooldown.seconds", 0);
                String sound = section.getString("sound.name", "");
                float soundVolume = (float) section.getDouble("sound.volume", 1.0);
                float soundPitch = (float) section.getDouble("sound.pitch", 1.0);
                boolean showTitle = section.getBoolean("title.enable", false);
                String titleText = section.getString("title.title", "&b{social}").replace("{social}", socialName);
                String subtitleText = section.getString("title.subtitle", "&7Click to visit!");

                if (command == null || command.isEmpty()) {
                    plugin.getLogger().warning("Comando no definido en: " + file.getName());
                    continue;
                }

                SocialNetwork social = new SocialNetwork(socialName, command, aliases, permission, link, hover, messages,
                        rewardCommands, enabled, registerCommand, itemType, itemValue, cooldownSeconds,
                        sound, soundVolume, soundPitch, showTitle, titleText, subtitleText);
                socialNetworks.put(command.toLowerCase(), social);
                socialFiles.put(socialName, file);
                loadedCount++;

            } catch (Exception e) {
                plugin.getLogger().severe("Error al cargar " + file.getName() + ": " + e.getMessage());
            }
        }

        // Cargar datos de recompensas
        loadRewardsData();

        // Log summary message
        if (loadedCount > 0) {
            plugin.log(org.bukkit.ChatColor.GREEN + "Social networks loaded: " + loadedCount);
        }
    }

    public void createNewSocial(String name) {
        File file = new File(socialsFolder, name + ".yml");
        if (!file.exists()) {
            YamlConfiguration config = new YamlConfiguration();
            org.bukkit.configuration.ConfigurationSection section = config.createSection(name);
            section.set("command", name);
            section.set("aliases", java.util.Arrays.asList(name.substring(0, Math.min(2, name.length()))));
            section.set("permission", "xsocials." + name);
            section.set("link", "https://example.com/" + name);
            section.set("hover", "&7Click to visit our " + name + "!");
            section.set("message", java.util.Arrays.asList(
                    "<center><gradient:#E7C500:#FCA606><bold>" + name.toUpperCase() + "</bold></gradient> &8» &7Haz clic en el enlace:",
                    "<center>&b{link}"
            ));
            section.set("rewards.commands", new java.util.ArrayList<String>());
            section.set("enabled", true);
            section.set("register-command", true);
            section.set("gui-item.type", "BLOCK");
            section.set("gui-item.material", "PAPER");
            try {
                config.save(file);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create new social: " + e.getMessage());
            }
        }
    }

    private File dataFile;
    private YamlConfiguration dataConfig;

    private void loadRewardsData() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create data file: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public boolean hasClaimedReward(UUID uuid, String socialName) {
        return dataConfig.getBoolean("rewards." + uuid.toString() + "." + socialName.toLowerCase(), false);
    }

    public void setClaimedReward(UUID uuid, String socialName) {
        dataConfig.set("rewards." + uuid.toString() + "." + socialName.toLowerCase(), true);
        try {
            dataConfig.save(dataFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save reward data: " + e.getMessage());
        }
    }

    public void registerSocialCommands() {
        try {
            CommandMap commandMap = null;
            
            // Try via CraftServer
            try {
                Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
                f.setAccessible(true);
                commandMap = (CommandMap) f.get(Bukkit.getServer());
            } catch (Exception ignored) {}

            // Try via SimplePluginManager if CraftServer failed
            if (commandMap == null) {
                try {
                    Field f = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
                    f.setAccessible(true);
                    commandMap = (CommandMap) f.get(Bukkit.getPluginManager());
                } catch (Exception ignored) {}
            }

            if (commandMap == null) {
                plugin.getLogger().severe("Could not find CommandMap. Dynamic social commands will not work.");
                return;
            }

            for (SocialNetwork social : socialNetworks.values()) {
                if (social.shouldRegisterCommand()) {
                    DynamicSocialCommand dynamicCommand = new DynamicSocialCommand(social, plugin);
                    commandMap.register(plugin.getName(), dynamicCommand);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error registering dynamic commands: " + e.getMessage());
        }
    }

    public void reload() {
        loadSocialNetworks();
        registerSocialCommands();
    }

    public SocialNetwork getSocialNetwork(String command) {
        return socialNetworks.get(command.toLowerCase());
    }

    public Collection<SocialNetwork> getAllSocialNetworks() {
        return socialNetworks.values();
    }

    public Map<String, SocialNetwork> getSocialNetworks() {
        return socialNetworks;
    }

    public File getSocialFile(String socialName) {
        return socialFiles.get(socialName);
    }

    public void executeSocialCommand(org.bukkit.command.CommandSender sender, SocialNetwork social) {
        if (!social.isEnabled()) {
            sender.sendMessage(plugin.getLanguageManager().getPrefix() + " " + plugin.getLanguageManager().getMessage("social-not-found").replace("{social}", social.getName()));
            return;
        }

        if (social.getPermission() != null && !social.getPermission().isEmpty() && !sender.hasPermission(social.getPermission())) {
            sender.sendMessage(plugin.getLanguageManager().getPrefix() + " " + plugin.getLanguageManager().getMessage("no-permission"));
            return;
        }

        if (!(sender instanceof org.bukkit.entity.Player)) {
            for (String message : social.getMessages()) {
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    message.replace("{link}", social.getLink()).replace("{player}", "Console")));
            }
            return;
        }

        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;

        // Cooldown check
        if (social.getCooldownSeconds() > 0) {
            if (plugin.getStatsManager().isOnCooldown(player.getUniqueId(), social.getName())) {
                long remaining = plugin.getStatsManager().getCooldownRemaining(player.getUniqueId(), social.getName());
                player.sendMessage(plugin.getLanguageManager().getPrefix() + " " + org.bukkit.ChatColor.RED + "You must wait " + remaining + " seconds before using this social again!");
                return;
            }
        }

        // Reward logic
        if (plugin.getConfig().getBoolean("rewards.enable", true)) {
            if (!hasClaimedReward(player.getUniqueId(), social.getName())) {
                boolean hasReward = social.getRewardCommands() != null && !social.getRewardCommands().isEmpty();
                if (hasReward) {
                    String safePlayerName = sanitizePlayerName(player.getName());
                    com.fabian.xsocials.utils.SchedulerUtils.runSync(plugin, () -> {
                        for (String cmd : social.getRewardCommands()) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", safePlayerName));
                        }
                    });
                    setClaimedReward(player.getUniqueId(), social.getName());
                    player.sendMessage(plugin.getLanguageManager().getPrefix() + " " + plugin.getLanguageManager().getMessage("reward-received", social.getName()));
                }
            }
        }

        // Interactive messages
        String hoverText = com.fabian.xsocials.utils.ColorUtils.translate(social.getHover());
        if (hoverText.isEmpty()) hoverText = "Click to visit!";

        boolean hasSpigot = isSpigot();

        for (String message : social.getMessages()) {
            String processed = message.replace("{link}", social.getLink()).replace("{player}", player.getName());
            processed = com.fabian.xsocials.utils.ColorUtils.translate(processed);

            if (processed.contains(social.getLink()) && hasSpigot) {
                net.md_5.bungee.api.chat.TextComponent component = new net.md_5.bungee.api.chat.TextComponent(processed);
                component.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, social.getLink()));
                setHoverEvent(component, hoverText);
                player.spigot().sendMessage(component);
            } else {
                player.sendMessage(processed);
            }
        }

        // Play sound
        if (social.getSound() != null && !social.getSound().isEmpty() && !social.getSound().equalsIgnoreCase("none")) {
            try {
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(social.getSound());
                player.playSound(player.getLocation(), sound, social.getSoundVolume(), social.getSoundPitch());
            } catch (Exception e) {
                // Invalid sound, ignore
            }
        }

        // Show title
        if (social.showTitle()) {
            String titleText = org.bukkit.ChatColor.translateAlternateColorCodes('&', social.getTitleText());
            String subtitleText = org.bukkit.ChatColor.translateAlternateColorCodes('&', social.getSubtitleText());
            player.sendTitle(titleText, subtitleText, 10, 60, 20);
        }

        // Set cooldown and increment stats
        if (social.getCooldownSeconds() > 0) {
            plugin.getStatsManager().setCooldown(player.getUniqueId(), social.getName());
        }
        plugin.getStatsManager().incrementUse(social.getName());
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

    private boolean isSpigot() {
        try {
            Class.forName("org.bukkit.entity.Player$Spigot");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private String sanitizePlayerName(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[^a-zA-Z0-9_]", "");
    }
}
