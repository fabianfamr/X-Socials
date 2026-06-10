package com.fabian.xsocials.commands;

import com.fabian.xsocials.XSocials;
import com.fabian.xsocials.models.SocialNetwork;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class DynamicSocialCommand extends Command {

    private final SocialNetwork social;
    private final XSocials plugin;

    public DynamicSocialCommand(SocialNetwork social, XSocials plugin) {
        super(social.getCommand());
        this.social = social;
        this.plugin = plugin;

        this.setDescription("Social command: " + social.getName());
        this.setAliases(social.getAliases());
        if (social.getPermission() != null && !social.getPermission().isEmpty()) {
            this.setPermission(social.getPermission());
        }
    }

    @Override
    public boolean testPermission(CommandSender target) {
        if (testPermissionSilent(target)) {
            return true;
        }
        target.sendMessage(plugin.getLanguageManager().getPrefix() + " " + plugin.getLanguageManager().getMessage("no-permission"));
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length > 0) {
            sender.sendMessage(plugin.getLanguageManager().getPrefix() + " " + org.bukkit.ChatColor.RED + "This command does not accept arguments.");
            return true;
        }
        
        // Dynamically fetch the updated social network in case of a plugin reload
        SocialNetwork currentSocial = plugin.getSocialManager().getSocialNetwork(this.getName());
        if (currentSocial == null) {
            currentSocial = this.social; // Fallback to the original if not found
        }
        
        // Use central execution logic for rewards, JSON, and enabled check
        plugin.getSocialManager().executeSocialCommand(sender, currentSocial);
        return true;
    }
}
