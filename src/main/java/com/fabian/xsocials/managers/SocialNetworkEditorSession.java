package com.fabian.xsocials.managers;

import com.fabian.xsocials.XSocials;
import com.fabian.xsocials.models.SocialNetwork;
import com.fabian.xsocials.utils.ConfigUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SocialNetworkEditorSession {

    private final XSocials plugin;
    private final String originalName;
    private final File file;

    private String command;
    private List<String> aliases;
    private String permission;
    private String link;
    private String hover;
    private List<String> messages;
    private List<String> rewardCommands;

    public SocialNetworkEditorSession(XSocials plugin, SocialNetwork social, File file) {
        this.plugin = plugin;
        this.originalName = social.getName(); // The key in the yaml (e.g. 'discord')
        this.file = file;

        // Initialize with current values
        this.command = social.getCommand();
        this.aliases = new ArrayList<>(social.getAliases() != null ? social.getAliases() : new ArrayList<>());
        this.permission = social.getPermission();
        this.link = social.getLink();
        this.hover = social.getHover();
        this.messages = new ArrayList<>(social.getMessages());
        this.rewardCommands = new ArrayList<>(
                social.getRewardCommands() != null ? social.getRewardCommands() : new ArrayList<>());
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setHover(String hover) {
        this.hover = hover;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public void setRewardCommands(List<String> rewardCommands) {
        this.rewardCommands = rewardCommands;
    }

    public String getCommand() {
        return command;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public String getPermission() {
        return permission;
    }

    public String getLink() {
        return link;
    }

    public String getHover() {
        return hover;
    }

    public List<String> getMessages() {
        return messages;
    }

    public List<String> getRewardCommands() {
        return rewardCommands;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void save() {
        // Use ConfigUtils to update the file preserving comments
        if (!file.exists()) {
            plugin.getLogger().severe("Cannot save session: File not found " + file.getName());
            return;
        }

        ConfigUtils.updateKey(file, "command", command);
        ConfigUtils.updateKey(file, "permission", permission != null ? permission : "");
        ConfigUtils.updateKey(file, "link", link);
        ConfigUtils.updateKey(file, "hover", hover != null ? hover : "");

        ConfigUtils.updateList(file, "aliases", aliases);
        ConfigUtils.updateList(file, "message", messages);
        ConfigUtils.updateList(file, "commands", rewardCommands); // Inside 'rewards' section

        // Reload the specific social network or all
        plugin.getSocialManager().reload();
    }
}
