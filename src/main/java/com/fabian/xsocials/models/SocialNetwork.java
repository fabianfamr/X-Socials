package com.fabian.xsocials.models;

import java.util.List;

public class SocialNetwork {

    private final String name;
    private final String command;
    private final String permission;
    private final String link;
    private final String hover;
    private final List<String> aliases;
    private final List<String> messages;
    private final List<String> rewardCommands;
    private final boolean enabled;
    private final boolean registerCommand;
    private final String itemType; // HEAD or BLOCK
    private final String itemValue; // Material or Name/Base64

    public SocialNetwork(String name, String command, List<String> aliases, String permission, String link,
            String hover, List<String> messages, List<String> rewardCommands, boolean enabled, 
            boolean registerCommand, String itemType, String itemValue) {
        this.name = name;
        this.command = command;
        this.aliases = aliases;
        this.permission = permission;
        this.link = link;
        this.hover = hover;
        this.messages = messages;
        this.rewardCommands = rewardCommands;
        this.enabled = enabled;
        this.registerCommand = registerCommand;
        this.itemType = itemType;
        this.itemValue = itemValue;
    }

    public String getName() {
        return name;
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

    public boolean hasPermission() {
        return permission != null && !permission.isEmpty();
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

    public boolean isEnabled() {
        return enabled;
    }

    public boolean shouldRegisterCommand() {
        return registerCommand;
    }

    public String getItemType() {
        return itemType;
    }

    public String getItemValue() {
        return itemValue;
    }
}
