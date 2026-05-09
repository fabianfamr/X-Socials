package com.fabian.xsocials.utils;

import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.Color;

public class ColorUtils {

    private static boolean miniMessageSupported = false;
    private static Object miniMessageInstance;
    private static java.lang.reflect.Method deserializeMethod;
    private static Object legacySerializerInstance;
    private static java.lang.reflect.Method serializeMethod;

    static {
        try {
            Class<?> miniMessageClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            miniMessageInstance = miniMessageClass.getMethod("miniMessage").invoke(null);
            deserializeMethod = miniMessageClass.getMethod("deserialize", String.class);
            
            Class<?> legacySerializerClass = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
            legacySerializerInstance = legacySerializerClass.getMethod("legacySection").invoke(null);
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            serializeMethod = legacySerializerClass.getMethod("serialize", componentClass);
            
            miniMessageSupported = true;
        } catch (Exception e) {}
    }

    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:(#[A-Fa-f0-9]{6}):(#[A-Fa-f0-9]{6})>(.*?)(</gradient>|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEX_PATTERN_1 = Pattern.compile("<(#[A-Fa-f0-9]{6})>");
    private static final Pattern HEX_PATTERN_2 = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_PATTERN_3 = Pattern.compile("(#[A-Fa-f0-9]{6})");

    public static String translate(String message) {
        if (message == null || message.isEmpty()) return message;

        boolean center = false;
        if (message.contains("<center>") || message.contains("</center>")) {
            center = true;
            message = message.replace("<center>", "").replace("</center>", "");
        }

        String translated;
        // Use Paper's native MiniMessage for perfect parsing if available
        if (miniMessageSupported) {
            try {
                Object component = deserializeMethod.invoke(miniMessageInstance, message);
                translated = (String) serializeMethod.invoke(legacySerializerInstance, component);
            } catch (Exception e) {
                translated = manualTranslate(message);
            }
        } else {
            translated = manualTranslate(message);
        }

        if (center) {
            translated = centerText(translated);
        }
        
        return translated;
    }

    public static String manualTranslate(String message) {
        // Basic MiniMessage tags
        message = message.replace("<bold>", "&l").replace("<b>", "&l").replace("</bold>", "&r").replace("</b>", "&r");
        message = message.replace("<italic>", "&o").replace("<i>", "&o").replace("</italic>", "&r").replace("</i>", "&r");
        message = message.replace("<underlined>", "&n").replace("<u>", "&n").replace("</underlined>", "&r").replace("</u>", "&r");
        message = message.replace("<strikethrough>", "&m").replace("<st>", "&m").replace("</strikethrough>", "&r").replace("</st>", "&r");
        message = message.replace("<obfuscated>", "&k").replace("<obf>", "&k").replace("</obfuscated>", "&r").replace("</obf>", "&r");
        message = message.replace("<reset>", "&r").replace("<r>", "&r");
        
        message = message.replace("<white>", "&f").replace("<black>", "&0");
        message = message.replace("<dark_blue>", "&1").replace("<dark_green>", "&2");
        message = message.replace("<dark_aqua>", "&3").replace("<dark_red>", "&4");
        message = message.replace("<dark_purple>", "&5").replace("<gold>", "&6");
        message = message.replace("<gray>", "&7").replace("<dark_gray>", "&8");
        message = message.replace("<blue>", "&9").replace("<green>", "&a");
        message = message.replace("<aqua>", "&b").replace("<red>", "&c");
        message = message.replace("<light_purple>", "&d").replace("<yellow>", "&e");

        // Parse Gradients
        Matcher gradientMatcher = GRADIENT_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (gradientMatcher.find()) {
            String hex1 = gradientMatcher.group(1);
            String hex2 = gradientMatcher.group(2);
            String text = gradientMatcher.group(3);
            gradientMatcher.appendReplacement(sb, Matcher.quoteReplacement(applyGradient(text, hex1, hex2)));
        }
        gradientMatcher.appendTail(sb);
        message = sb.toString();

        // Parse Hex: <#RRGGBB>
        Matcher hexMatcher1 = HEX_PATTERN_1.matcher(message);
        sb = new StringBuffer();
        while (hexMatcher1.find()) {
            hexMatcher1.appendReplacement(sb, convertHexToBungee(hexMatcher1.group(1)));
        }
        hexMatcher1.appendTail(sb);
        message = sb.toString();

        // Parse Hex: &#RRGGBB
        Matcher hexMatcher2 = HEX_PATTERN_2.matcher(message);
        sb = new StringBuffer();
        while (hexMatcher2.find()) {
            hexMatcher2.appendReplacement(sb, convertHexToBungee("#" + hexMatcher2.group(1)));
        }
        hexMatcher2.appendTail(sb);
        message = sb.toString();

        // Parse Hex: #RRGGBB
        Matcher hexMatcher3 = HEX_PATTERN_3.matcher(message);
        sb = new StringBuffer();
        while (hexMatcher3.find()) {
            hexMatcher3.appendReplacement(sb, convertHexToBungee(hexMatcher3.group(1)));
        }
        hexMatcher3.appendTail(sb);
        message = sb.toString();

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String applyGradient(String text, String startHex, String endHex) {
        try {
            Color start = Color.decode(startHex);
            Color end = Color.decode(endHex);
            
            int length = text.length();
            if (length == 0) return "";
            if (length == 1) return convertHexToBungee(startHex) + text;

            StringBuilder result = new StringBuilder();
            int step = 0;
            
            String stripped = text.replaceAll("&[0-9a-fk-or]", "");
            int steps = Math.max(1, stripped.length() - 1);
            
            String currentFormat = "";

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '&' && i + 1 < text.length()) {
                    char code = text.charAt(i + 1);
                    if ("0123456789abcdefklmnorABCDEFKLMNOR".indexOf(code) != -1) {
                        currentFormat += "&" + code;
                        i++;
                        continue;
                    }
                }
                
                float ratio = (float) step / steps;
                int red = (int) (start.getRed() * (1 - ratio) + end.getRed() * ratio);
                int green = (int) (start.getGreen() * (1 - ratio) + end.getGreen() * ratio);
                int blue = (int) (start.getBlue() * (1 - ratio) + end.getBlue() * ratio);
                
                String hex = String.format("#%02X%02X%02X", red, green, blue);
                result.append(convertHexToBungee(hex)).append(currentFormat).append(c);
                if (c != ' ') step++;
            }
            return result.toString();
        } catch (Exception e) {
            return text;
        }
    }

    private static String convertHexToBungee(String hex) {
        if (hex.length() != 7) return hex;
        try {
            // Converts #RRGGBB to §x§R§R§G§G§B§B
            StringBuilder builder = new StringBuilder("§x");
            for (int i = 1; i < 7; i++) {
                builder.append("§").append(hex.charAt(i));
            }
            return builder.toString();
        } catch (Exception e) {
            return hex;
        }
    }

    public static String centerText(String message) {
        if (message == null || message.isEmpty()) return "";
        
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;
        
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '§') {
                previousCode = true;
            } else if (previousCode) {
                previousCode = false;
                if (c == 'l' || c == 'L') {
                    isBold = true;
                } else if ("0123456789abcdefxrABCDEFXR".indexOf(c) != -1) {
                    isBold = false;
                }
            } else {
                int charInfo = getCharWidth(c);
                messagePxSize += charInfo;
                if (isBold) {
                    messagePxSize += 1;
                }
                // 1 pixel between characters
                messagePxSize += 1;
            }
        }
        
        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = 154 - halvedMessageSize;
        int spaceLength = 4; // Space is 3 pixels + 1 pixel spacing
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
}
