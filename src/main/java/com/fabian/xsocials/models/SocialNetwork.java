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
    private final String itemType;
    private final String itemValue;
    private final long cooldownSeconds;
    private final String sound;
    private final float soundVolume;
    private final float soundPitch;
    private final boolean showTitle;
    private final String titleText;
    private final String subtitleText;

    public SocialNetwork(String name, String command, List<String> aliases, String permission, String link,
            String hover, List<String> messages, List<String> rewardCommands, boolean enabled,
            boolean registerCommand, String itemType, String itemValue, long cooldownSeconds,
            String sound, float soundVolume, float soundPitch, boolean showTitle,
            String titleText, String subtitleText) {
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
        this.cooldownSeconds = cooldownSeconds;
        this.sound = sound;
        this.soundVolume = soundVolume;
        this.soundPitch = soundPitch;
        this.showTitle = showTitle;
        this.titleText = titleText;
        this.subtitleText = subtitleText;
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

    public long getCooldownSeconds() {
        return cooldownSeconds;
    }

    public String getSound() {
        return sound;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public float getSoundPitch() {
        return soundPitch;
    }

    public boolean showTitle() {
        return showTitle;
    }

    public String getTitleText() {
        return titleText;
    }

    public String getSubtitleText() {
        return subtitleText;
    }
}
