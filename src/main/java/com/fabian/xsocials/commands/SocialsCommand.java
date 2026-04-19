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
                sender.sendMessage(ChatColor.RED + "Only players can use the GUI.");
                return true;
            }
            if (!sender.hasPermission("xsocials.admin")) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
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
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }
                plugin.reload();
                sender.sendMessage(plugin.getLanguageManager().getMessage("config-reloaded"));
                break;

            case "update":
                if (!sender.hasPermission("xsocials.update")) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }
                sender.sendMessage(plugin.getLanguageManager().getPrefix() + " "
                        + plugin.getLanguageManager().getMessage("checking-updates"));
                new UpdateChecker(plugin).checkForUpdates(sender);
                break;

            case "list":
                listSocials(sender);
                break;

            case "edit":
                handleEdit(sender, args);
                break;

            default:
                SocialNetwork social = plugin.getSocialManager().getSocialNetwork(subCommand);
                if (social != null) {
                    executeSocialCommand(sender, social);
                } else {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("command-usage"));
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
            sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /xs edit <social> <link|hover|permission|command> <value>");
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
            sender.sendMessage(plugin.getLanguageManager().getMessage("social-not-found").replace("{social}", socialName));
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
                sender.sendMessage(ChatColor.RED + "Invalid property. Use: link, hover, permission, command");
                return;
        }

        session.save();
        sender.sendMessage(ChatColor.GREEN + "Property '" + property + "' updated for " + social.getName());
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getLanguageManager().getMessage("help-header"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("help-title"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("help-list"));
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            subcommands.add("list");
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
