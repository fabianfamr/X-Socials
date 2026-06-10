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

    private String name;
    private String command;
    private List<String> aliases;
    private String permission;
    private String link;
    private String hover;
    private List<String> messages;
    private List<String> rewardCommands;

    public SocialNetworkEditorSession(XSocials plugin, SocialNetwork social, File file) {
        this.plugin = plugin;
        this.originalName = social.getName();
        this.name = social.getName();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean hasChanges() {
        if (!originalName.equals(name)) return true;
        SocialNetwork original = plugin.getSocialManager().getSocialNetworks().values().stream()
                .filter(s -> s.getName().equals(originalName)).findFirst().orElse(null);
        if (original == null) return true;
        String origPerm = original.getPermission() != null ? original.getPermission() : "";
        String origLink = original.getLink() != null ? original.getLink() : "";
        String origHover = original.getHover() != null ? original.getHover() : "";
        String curPerm = permission != null ? permission : "";
        String curLink = link != null ? link : "";
        String curHover = hover != null ? hover : "";
        return !command.equals(original.getCommand()) ||
               !curPerm.equals(origPerm) ||
               !curLink.equals(origLink) ||
               !curHover.equals(origHover);
    }

    public void save() {
        if (!file.exists()) {
            plugin.getLogger().severe("Cannot save session: File not found " + file.getName());
            return;
        }

        if (!originalName.equals(name)) {
            ConfigUtils.renameRootKey(file, originalName, name);
            File newFile = new File(file.getParentFile(), name + ".yml");
            if (!newFile.exists() && file.renameTo(newFile)) {
                plugin.getLogger().info("Renamed social file from " + originalName + " to " + name);
            }
        }

        ConfigUtils.updateKey(file, "command", command);
        ConfigUtils.updateKey(file, "permission", permission != null ? permission : "");
        ConfigUtils.updateKey(file, "link", link);
        ConfigUtils.updateKey(file, "hover", hover != null ? hover : "");

        ConfigUtils.updateList(file, "aliases", aliases);
        ConfigUtils.updateList(file, "message", messages);
        ConfigUtils.updateList(file, "rewards.commands", rewardCommands);

        plugin.getSocialManager().reload();
    }
}
