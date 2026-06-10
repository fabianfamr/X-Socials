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

            String searchKey = key + ":";
            int baseIndent = -1;

            for (String line : lines) {
                String trimmed = line.trim();

                if (baseIndent == -1 && trimmed.startsWith(searchKey)) {
                    baseIndent = line.indexOf(searchKey);
                }

                if (baseIndent != -1 && trimmed.startsWith(searchKey)) {
                    int indent = line.indexOf(searchKey);
                    if (indent == baseIndent) {
                        String valueToSave = newValue;
                        if (!valueToSave.isEmpty() && !valueToSave.startsWith("\"") && !valueToSave.endsWith("\"")
                                && (valueToSave.contains("&") || valueToSave.contains(" ") || valueToSave.contains(":"))) {
                            valueToSave = "\"" + valueToSave.replace("\"", "\\\"") + "\"";
                        }
                        newLines.add(line.substring(0, indent) + key + ": " + valueToSave);
                        updated = true;
                        continue;
                    }
                }

                if (trimmed.startsWith("#")) {
                    newLines.add(line);
                    continue;
                }

                if (baseIndent != -1 && !trimmed.isEmpty()) {
                    int currentIndent = line.length() - line.replaceFirst("^\\s*", "").length();
                    if (currentIndent <= baseIndent && !trimmed.equals(line.trim())) {
                        baseIndent = -1;
                    }
                }

                newLines.add(line);
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

    public static void renameRootKey(File file, String oldName, String newName) {
        if (oldName.equals(newName)) return;
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            List<String> newLines = new ArrayList<>();
            boolean renamed = false;

            for (String line : lines) {
                String trimmed = line.trim();
                if (!renamed && trimmed.equals(oldName + ":")) {
                    int indent = line.length() - line.replaceFirst("^\\s*", "").length();
                    String indentStr = line.substring(0, indent);
                    newLines.add(indentStr + newName + ":");
                    renamed = true;
                } else if (!renamed && trimmed.startsWith(oldName + ".")) {
                    newLines.add(line.replace(oldName + ".", newName + "."));
                    renamed = true;
                } else {
                    newLines.add(line);
                }
            }

            if (renamed) {
                Files.write(file.toPath(), newLines, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
