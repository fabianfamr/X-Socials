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
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        // Use central execution logic for rewards, JSON, and enabled check
        plugin.getSocialManager().executeSocialCommand(sender, social);
        return true;
    }
}
