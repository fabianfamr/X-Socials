package com.fabian.xsocials.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ConfigUtils {

    /**
     * Updates a specific key in a YAML file while preserving comments.
     * Only works for simple key-value pairs at specific indentation levels.
     */
    public static void updateKey(File file, String key, String newValue) {
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            List<String> newLines = new ArrayList<>();
            boolean updated = false;

            // Simple pattern matching for " key: value" or "key: value"
            // We assume the key is the last part of the path if it's dot notation,
            // but for this plugin we are mostly editing values inside a section.
            // Since our structure is social -> key: value, we need to be careful.
            // But actually, we are editing specific files per social network, so key is
            // usually direct.
            // Example: "command: discord"

            String searchKey = key + ":";

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith(searchKey) && !trimmed.startsWith("#")) {
                    // Calculate indentation
                    int indentation = line.indexOf(searchKey);
                    String indentStr = line.substring(0, indentation);

                    // Quote functionality if needed, for strings with special chars
                    String valueToSave = newValue;
                    if (valueToSave.contains("&") || valueToSave.contains(" ")) {
                        valueToSave = "\"" + valueToSave + "\"";
                    }

                    newLines.add(indentStr + key + ": " + valueToSave);
                    updated = true;
                } else {
                    newLines.add(line);
                }
            }

            if (updated) {
                Files.write(file.toPath(), newLines, StandardCharsets.UTF_8);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates a list in a YAML file.
     * This is a simplified version that replaces a block.
     * It assumes the list is defined as:
     * key:
     * - item1
     * - item2
     */
    public static void updateList(File file, String key, List<String> newList) {
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            List<String> newLines = new ArrayList<>();
            boolean inList = false;
            boolean updated = false;

            String searchKey = key + ":";
            String indentStr = "";

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String trimmed = line.trim();

                if (inList) {
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        if (trimmed.startsWith("#"))
                            newLines.add(line);
                        continue;
                    }

                    if (line.length() <= indentStr.length() || !line.startsWith(indentStr + " ")) {
                        inList = false;
                        newLines.add(line);
                    }
                } else {
                    // Search for key at the end of a path or alone
                    // e.g. "  commands:" inside rewards
                    if (trimmed.startsWith(searchKey) && !trimmed.startsWith("#")) {
                        int indentation = line.indexOf(searchKey);
                        indentStr = line.substring(0, indentation);

                        newLines.add(indentStr + key.substring(key.lastIndexOf(".") + 1) + ":");
                        if (newList.isEmpty()) {
                            newLines.set(newLines.size() - 1, indentStr + key.substring(key.lastIndexOf(".") + 1) + ": []");
                        } else {
                            for (String item : newList) {
                                String val = item;
                                if (val.contains("&") || val.contains(":"))
                                    val = "\"" + val + "\"";
                                newLines.add(indentStr + "  - " + val);
                            }
                        }

                        updated = true;
                        inList = true;
                    } else {
                        newLines.add(line);
                    }
                }
            }

            // Verify if we were still in list at the end
            if (inList) {
                // End of file, nothing to do
            }

            if (updated) {
                Files.write(file.toPath(), newLines, StandardCharsets.UTF_8);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
