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
        // %xsocials_<name>_link%
        // %xsocials_<name>_command%
        
        String[] split = params.split("_");
        if (split.length < 2) return null;
        
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
        }
        
        return null;
    }
}
