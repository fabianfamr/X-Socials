package com.fabian.xsocials.commands;

import com.fabian.xsocials.XSocials;
import com.fabian.xsocials.models.SocialNetwork;
import com.fabian.xsocials.utils.UpdateChecker;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SocialsCommand implements CommandExecutor, TabCompleter {

    private final XSocials plugin;

    public SocialsCommand(XSocials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("-gui")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getLanguageManager().getPrefix() + " " + plugin.getLanguageManager().getMessage("player-only"));
                return true;
            }
            if (!sender.hasPermission("xsocials.admin")) {
                sender.sendMessage(plugin.getLanguageManager().getPrefix() + " " + plugin.getLanguageManager().getMessage("no-permission"));
                return true;
            }
            plugin.getGUIManager().openMainGUI((Player) sender);
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                if (!sender.hasPermission("xsocials.reload")) {
                    sender.sendMessage(plugin.getLanguageManager().getPrefix() + " " + plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }
                plugin.reload();
                sender.sendMessage(plugin.getLanguageManager().getPrefix() + " " + plugin.getLanguageManager().getMessage("config-reloaded"));
                break;

            case "update":
                if (!sender.hasPermission("xsocials.update")) {
                    sender.sendMessage(plugin.getLanguageManager().getPrefix() + " " + plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }
                sender.sendMessage(plugin.getLanguageManager().getPrefix() + " "
                        + plugin.getLanguageManager().getMessage("checking-updates"));
                new UpdateChecker(plugin).checkForUpdates(sender);
                break;

            case "list":
                listSocials(sender);
                break;

            case "version":
                showVersion(sender);
                break;

            case "edit":
                handleEdit(sender, args);
                break;

            default:
                SocialNetwork social = plugin.getSocialManager().getSocialNetwork(subCommand);
                if (social != null) {
                    executeSocialCommand(sender, social);
                } else {
                    sender.sendMessage(plugin.getLanguageManager().getPrefix() + " " + plugin.getLanguageManager().getMessage("command-usage"));
                }
                break;
        }

        return true;
    }

    private void executeSocialCommand(CommandSender sender, SocialNetwork social) {
        plugin.getSocialManager().executeSocialCommand(sender, social);
    }

    private void handleEdit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xsocials.admin")) {
            sender.sendMessage(plugin.getLanguageManager().getPrefix() + " " + plugin.getLanguageManager().getMessage("no-permission"));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(plugin.getLanguageManager().getPrefix() + " " + ChatColor.RED + "Usage: /xs edit <social> <link|hover|permission|command> <value>");
            return;
        }

        String socialName = args[1];
        String property = args[2].toLowerCase();
        
        // Join the rest of args for value (links can have spaces or special chars if not careful)
        StringBuilder valueBuilder = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            valueBuilder.append(args[i]).append(i == args.length - 1 ? "" : " ");
        }
        String value = valueBuilder.toString();

        SocialNetwork social = plugin.getSocialManager().getSocialNetworks().values().stream()
                .filter(s -> s.getName().equalsIgnoreCase(socialName))
                .findFirst().orElse(null);

        if (social == null) {
            sender.sendMessage(plugin.getLanguageManager().getPrefix() + " " + plugin.getLanguageManager().getMessage("social-not-found").replace("{social}", socialName));
            return;
        }

        java.io.File file = plugin.getSocialManager().getSocialFile(social.getName());
        com.fabian.xsocials.managers.SocialNetworkEditorSession session = 
            new com.fabian.xsocials.managers.SocialNetworkEditorSession(plugin, social, file);

        switch (property) {
            case "link": session.setLink(value); break;
            case "hover": session.setHover(value); break;
            case "permission": session.setPermission(value.equalsIgnoreCase("none") ? "" : value); break;
            case "command": session.setCommand(value.split(" ")[0]); break;
            default:
                sender.sendMessage(plugin.getLanguageManager().getPrefix() + " " + ChatColor.RED + "Invalid property. Use: link, hover, permission, command");
                return;
        }

        session.save();
        sender.sendMessage(plugin.getLanguageManager().getPrefix() + " " + ChatColor.GREEN + "Property '" + property + "' updated for " + social.getName());
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getLanguageManager().getMessage("help-header"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("help-title"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("help-list"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("help-version"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("help-gui"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("help-reload"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("help-update"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("help-footer"));
    }

    private void listSocials(CommandSender sender) {
        sender.sendMessage(plugin.getLanguageManager().getMessage("list-header"));
        for (SocialNetwork social : plugin.getSocialManager().getSocialNetworks().values()) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("list-format", social.getCommand(), social.getName()));
        }
        sender.sendMessage(plugin.getLanguageManager().getMessage("list-footer"));
    }

    private void showVersion(CommandSender sender) {
        String serverVersion = org.bukkit.Bukkit.getVersion();
        if (serverVersion == null) {
            serverVersion = org.bukkit.Bukkit.getServer().getClass().getPackage().getName().replace("org.bukkit.craftbukkit.", "").split("\\.")[0] + ".X";
        }
        String pluginVersion = plugin.getDescription().getVersion();
        String serverName = getServerImplementation();

        sender.sendMessage(plugin.getLanguageManager().getPrefix() + " " + ChatColor.GRAY + "=== " + ChatColor.AQUA + "X-Socials " + pluginVersion + ChatColor.GRAY + " ===");
        sender.sendMessage(plugin.getLanguageManager().getPrefix() + " " + ChatColor.GRAY + "Server: " + ChatColor.WHITE + serverName + " " + serverVersion);
        sender.sendMessage(plugin.getLanguageManager().getPrefix() + " " + ChatColor.GRAY + "Protocol: " + ChatColor.WHITE + getProtocolVersion() + ChatColor.GRAY + " (" + org.bukkit.Bukkit.getVersion() + ")");
    }

    private String getServerImplementation() {
        String name = org.bukkit.Bukkit.getServer().getClass().getSimpleName();
        if (name.contains("Folia")) return "Folia";
        if (name.contains("Paper")) return "Paper";
        if (name.contains("Purpur")) return "Purpur";
        if (name.contains("Spigot")) return "Spigot";
        if (name.contains("CraftBukkit")) return "CraftBukkit";
        return "Bukkit";
    }

    private String getProtocolVersion() {
        try {
            // Safe check for Paper or other forks that provide protocol version
            Object unsafe = org.bukkit.Bukkit.class.getMethod("getUnsafe").invoke(null);
            Integer protocol = (Integer) unsafe.getClass().getMethod("getProtocolVersion").invoke(unsafe);
            if (protocol != null && protocol > 0) return String.valueOf(protocol);
        } catch (Exception ignored) {}
        try {
            String version = org.bukkit.Bukkit.getVersion();
            if (version.contains("1.21")) return "767 (1.21.x)";
            if (version.contains("1.20.6")) return "765";
            if (version.contains("1.20.4")) return "763";
            if (version.contains("1.20.2")) return "762";
            if (version.contains("1.20")) return "763";
            if (version.contains("1.19.4")) return "760";
            if (version.contains("1.19.3")) return "759";
            if (version.contains("1.19.2")) return "758";
            if (version.contains("1.19")) return "760";
            if (version.contains("1.18.2")) return "757";
            if (version.contains("1.18")) return "757";
            if (version.contains("1.17.1")) return "755";
            if (version.contains("1.17")) return "755";
            if (version.contains("1.16.5")) return "754";
            if (version.contains("1.16.4")) return "754";
            if (version.contains("1.16.3")) return "753";
            if (version.contains("1.16.2")) return "752";
            if (version.contains("1.16.1")) return "751";
            if (version.contains("1.16")) return "754";
            if (version.contains("1.15.2")) return "735";
            if (version.contains("1.15.1")) return "575";
            if (version.contains("1.15")) return "735";
            if (version.contains("1.14.4")) return "498";
            if (version.contains("1.14.3")) return "497";
            if (version.contains("1.14.2")) return "498";
            if (version.contains("1.14.1")) return "480";
            if (version.contains("1.14")) return "498";
            if (version.contains("1.13.2")) return "404";
            if (version.contains("1.13.1")) return "393";
            if (version.contains("1.13")) return "404";
            if (version.contains("1.12.2")) return "340";
            if (version.contains("1.12.1")) return "338";
            if (version.contains("1.12")) return "340";
            if (version.contains("1.11.2")) return "315";
            if (version.contains("1.11.1")) return "315";
            if (version.contains("1.11")) return "315";
            if (version.contains("1.10.2")) return "210";
            if (version.contains("1.10")) return "210";
            if (version.contains("1.9.4")) return "110";
            if (version.contains("1.9.3")) return "109";
            if (version.contains("1.9.2")) return "108";
            if (version.contains("1.9")) return "107";
            if (version.contains("1.8.9")) return "47";
            if (version.contains("1.8.8")) return "47";
            if (version.contains("1.8")) return "47";
        } catch (Exception ex) {}
        return "Unknown";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            subcommands.add("list");
            subcommands.add("version");
            if (sender.hasPermission("xsocials.reload")) subcommands.add("reload");
            if (sender.hasPermission("xsocials.update")) subcommands.add("update");
            if (sender.hasPermission("xsocials.admin")) {
                subcommands.add("edit");
                subcommands.add("-gui");
            }
            return StringUtil.copyPartialMatches(args[0], subcommands, new ArrayList<>());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("edit") && sender.hasPermission("xsocials.admin")) {
            List<String> names = new ArrayList<>();
            for (SocialNetwork s : plugin.getSocialManager().getSocialNetworks().values()) names.add(s.getName());
            return StringUtil.copyPartialMatches(args[1], names, new ArrayList<>());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("edit") && sender.hasPermission("xsocials.admin")) {
            return StringUtil.copyPartialMatches(args[2], Arrays.asList("link", "hover", "permission", "command"), new ArrayList<>());
        }

        return Collections.emptyList();
    }
}
