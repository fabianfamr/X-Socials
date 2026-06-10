package com.fabian.xsocials.utils;

import com.fabian.xsocials.XSocials;
import com.fabian.xsocials.models.SocialNetwork;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class XSocialsExpansion extends PlaceholderExpansion {

    private final XSocials plugin;

    public XSocialsExpansion(XSocials plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getIdentifier() {
        return "xsocials";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

@Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String[] split = params.split("_");
        if (split.length < 2) return null;

        // Global stats placeholders
        if (params.equalsIgnoreCase("total_uses")) {
            return String.valueOf(plugin.getStatsManager().getTotalUses());
        }

        if (split[0].equalsIgnoreCase("total") && split[1].equalsIgnoreCase("uses")) {
            return String.valueOf(plugin.getStatsManager().getTotalUses());
        }

        // Player stats placeholders
        if (split[0].equalsIgnoreCase("player") && split.length >= 3) {
            if (split[1].equalsIgnoreCase("has")) {
                String socialName = params.substring(params.indexOf("_", 7) + 1);
                if (player.getPlayer() != null) {
                    return String.valueOf(plugin.getSocialManager().hasClaimedReward(player.getUniqueId(), socialName));
                }
            }
            return null;
        }

        // Social network placeholders: %xsocials_<name>_link% and %xsocials_<name>_command%
        String socialName = split[0];
        String type = split[1];

        SocialNetwork social = plugin.getSocialManager().getSocialNetworks().values().stream()
                .filter(s -> s.getName().equalsIgnoreCase(socialName))
                .findFirst().orElse(null);

        if (social == null) return null;

        if (type.equalsIgnoreCase("link")) {
            return social.getLink();
        } else if (type.equalsIgnoreCase("command")) {
            return social.getCommand();
        } else if (type.equalsIgnoreCase("uses")) {
            return String.valueOf(plugin.getStatsManager().getSocialUses(socialName));
        } else if (type.equalsIgnoreCase("enabled")) {
            return social.isEnabled() ? "true" : "false";
        }

        return null;
    }
}
