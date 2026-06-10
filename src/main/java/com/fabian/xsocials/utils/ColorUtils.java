package com.fabian.xsocials.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern SPIGOT_HEX_PATTERN = Pattern.compile("(?i)&x(&[A-Fa-f0-9]){6}");

    private static boolean papiAvailable = false;
    private static Method setPlaceholdersMethod;
    private static boolean paperAdventureAvailable = false;

    static {
        try {
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            setPlaceholdersMethod = papiClass.getMethod("setPlaceholders", org.bukkit.OfflinePlayer.class, String.class);
            papiAvailable = true;
        } catch (Exception ignored) {}
        try {
            Class.forName("io.papermc.paper.adventure.PaperAudiences");
            paperAdventureAvailable = true;
        } catch (Exception e) {
            paperAdventureAvailable = false;
        }
    }

    /**
     * Main translate method - replaces the old translate() and manualTranslate().
     * Converts all color formats to MiniMessage, deserializes, then returns legacy string.
     * Supports: & codes, &#hex, <gradient>, <bold>, <#hex>, MiniMessage tags.
     */
    public static String translate(String text) {
        if (text == null || text.isEmpty()) return text;

        // Handle <center> tags
        boolean center = false;
        if (text.contains("<center>") || text.contains("</center>")) {
            center = true;
            text = text.replace("<center>", "").replace("</center>", "");
        }

        String result = translateColors(text);

        if (center) {
            result = centerText(result);
        }

        return result;
    }

    public static String translateColors(String text) {
        if (text == null || text.isEmpty()) return text;
        text = convertLegacyAndHex(text);
        Component component = MINI_MESSAGE.deserialize(text);
        return LEGACY_SERIALIZER.serialize(component);
    }

    public static Component format(Player player, String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        text = applyPapi(player, text);
        text = convertLegacyAndHex(text);
        return MINI_MESSAGE.deserialize(text);
    }

    public static String convertLegacyAndHex(String text) {
        if (text == null || text.isEmpty()) return text;
        text = text.replace('\u00a7', '&');
        text = text.replaceAll("(?i)<color:&#([A-Fa-f0-9]{6})>", "<#$1>");
        Matcher spigotMatcher = SPIGOT_HEX_PATTERN.matcher(text);
        StringBuilder spigotBuilder = new StringBuilder();
        while (spigotMatcher.find()) {
            String hex = spigotMatcher.group().replaceAll("[&xX]", "");
            spigotMatcher.appendReplacement(spigotBuilder, "<#" + hex + ">");
        }
        spigotMatcher.appendTail(spigotBuilder);
        text = spigotBuilder.toString();
        Matcher hexMatcher = HEX_PATTERN.matcher(text);
        StringBuilder builder = new StringBuilder();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(builder, "<#" + hexMatcher.group(1) + ">");
        }
        hexMatcher.appendTail(builder);
        text = builder.toString();
        text = text.replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>")
                   .replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
                   .replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>")
                   .replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>")
                   .replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>")
                   .replace("&f", "<white>").replace("&l", "<bold>").replace("&m", "<strikethrough>")
                   .replace("&n", "<underlined>").replace("&o", "<italic>").replace("&r", "<reset>");
        text = text.replace("<b>", "<bold>").replace("</b>", "</bold>")
                   .replace("<i>", "<italic>").replace("</i>", "</italic>")
                   .replace("<u>", "<underlined>").replace("</u>", "</underlined>")
                   .replace("<s>", "<strikethrough>").replace("</s>", "</strikethrough>");
        return text;
    }

    public static String applyPapi(Player player, String text) {
        if (text == null || text.isEmpty()) return text;
        if (player != null && papiAvailable && setPlaceholdersMethod != null) {
            try {
                text = (String) setPlaceholdersMethod.invoke(null, player, text);
            } catch (Exception ignored) {}
        }
        return text;
    }

    public static String toLegacyString(Component component) {
        return LEGACY_SERIALIZER.serialize(component);
    }

    public static void sendComponent(CommandSender sender, Component component) {
        try {
            sender.sendMessage(component);
            return;
        } catch (NoSuchMethodError | NoClassDefFoundError e) {}
        sender.sendMessage(toLegacyString(component));
    }

    /**
     * Centers text in the Minecraft chat (preserved from old implementation).
     */
    public static String centerText(String message) {
        if (message == null || message.isEmpty()) return "";
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '\u00a7') {
                previousCode = true;
            } else if (previousCode) {
                previousCode = false;
                if (c == 'l' || c == 'L') isBold = true;
                else if ("0123456789abcdefxrABCDEFXR".indexOf(c) != -1) isBold = false;
            } else {
                int charInfo = getCharWidth(c);
                messagePxSize += charInfo;
                if (isBold) messagePxSize += 1;
                messagePxSize += 1;
            }
        }
        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = 154 - halvedMessageSize;
        int spaceLength = 4;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceLength;
        }
        return sb.toString() + message;
    }

    private static int getCharWidth(char c) {
        if (c == 'i' || c == '!' || c == '.' || c == ',' || c == ':' || c == ';' || c == '|' || c == '\'') return 1;
        if (c == 'l' || c == '`') return 2;
        if (c == 'I' || c == '[' || c == ']' || c == '"' || c == ' ') return 3;
        if (c == 'f' || c == 'k' || c == 't' || c == '(' || c == ')' || c == '{' || c == '}' || c == '<' || c == '>') return 4;
        if (c == '@') return 6;
        return 5;
    }

    public static boolean isPAPIAvailable() { return papiAvailable; }
    public static boolean isPaperAdventureAvailable() { return paperAdventureAvailable; }
}
