package com.fabian.xsocials.managers;

import com.fabian.xsocials.XSocials;
import com.fabian.xsocials.models.SocialNetwork;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class GUIManager implements Listener {

    private final XSocials plugin;
    private final Map<UUID, SocialNetworkEditorSession> editorSessions;
    private final Map<UUID, String> chatInputContext;

    public GUIManager(XSocials plugin) {
        this.plugin = plugin;
        this.editorSessions = new HashMap<>();
        this.chatInputContext = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openMainGUI(Player player) {
        String title = plugin.getLanguageManager().getMessage("gui-main-title");
        Inventory gui = Bukkit.createInventory(null, 54, title);

        fillBorders(gui);

        int slot = 10;
        for (SocialNetwork social : plugin.getSocialManager().getAllSocialNetworks()) {
            if (!social.isEnabled()) continue;
            
            while (isBorder(slot)) slot++;
            if (slot >= 44) break;

            ItemStack item = getSocialItem(social);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.AQUA + ChatColor.BOLD.toString() + social.getName());
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Command: " + ChatColor.YELLOW + "/" + social.getCommand(),
                    ChatColor.GRAY + "Link: " + ChatColor.WHITE + social.getLink(),
                    "",
                    ChatColor.YELLOW + "Click to Edit Settings"
                ));
                item.setItemMeta(meta);
            }
            gui.setItem(slot++, item);
        }

        player.openInventory(gui);
    }

    public void openEditor(Player player, String socialName) {
        SocialNetwork social = plugin.getSocialManager().getSocialNetworks().values().stream()
                .filter(s -> s.getName().equals(socialName))
                .findFirst().orElse(null);

        if (social == null) {
            player.sendMessage(ChatColor.RED + "Error: Social network not found.");
            return;
        }

        File file = plugin.getSocialManager().getSocialFile(socialName);
        if (file == null) {
            player.sendMessage(ChatColor.RED + "Error: Configuration file not found.");
            return;
        }

        editorSessions.putIfAbsent(player.getUniqueId(), new SocialNetworkEditorSession(plugin, social, file));
        SocialNetworkEditorSession session = editorSessions.get(player.getUniqueId());

        String title = plugin.getLanguageManager().getMessage("gui-editor-title", socialName);
        Inventory gui = Bukkit.createInventory(null, 36, title);
        fillBorders(gui);

        // Info Item
        ItemStack infoItem = getSocialItem(social);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(ChatColor.AQUA + socialName);
            infoMeta.setLore(Collections.singletonList(plugin.getLanguageManager().getMessage("gui-item-edit-info-lore")));
            infoItem.setItemMeta(infoMeta);
        }
        gui.setItem(4, infoItem);

        // Properties
        gui.setItem(10, createItem(Material.PAPER, plugin.getLanguageManager().getMessage("gui-item-edit-command-name"),
                plugin.getLanguageManager().getMessage("gui-item-edit-command-lore", session.getCommand())));

        gui.setItem(11, createItem(getSignMaterial(), plugin.getLanguageManager().getMessage("gui-item-edit-name-name"),
                plugin.getLanguageManager().getMessage("gui-item-edit-name-lore", session.getOriginalName())));

        gui.setItem(12, createItem(Material.NAME_TAG, plugin.getLanguageManager().getMessage("gui-item-edit-permission-name"),
                plugin.getLanguageManager().getMessage("gui-item-edit-permission-lore", session.getPermission().isEmpty() ? "None" : session.getPermission())));

        gui.setItem(13, createItem(Material.BOOK, plugin.getLanguageManager().getMessage("gui-item-edit-link-name"),
                plugin.getLanguageManager().getMessage("gui-item-edit-link-lore", session.getLink())));

        gui.setItem(14, createItem(Material.FEATHER, "&eEdit Hover Tooltip",
                ChatColor.GRAY + "Current: " + ChatColor.WHITE + (session.getHover().isEmpty() ? "None" : session.getHover())));

        gui.setItem(15, createItem(Material.GOLD_INGOT, "&eEdit Rewards",
                ChatColor.GRAY + "Click to edit reward commands"));

        // Actions
        gui.setItem(22, createItem(Material.EMERALD, plugin.getLanguageManager().getMessage("gui-item-save-name"),
                plugin.getLanguageManager().getMessage("gui-item-save-lore")));

        gui.setItem(18, createItem(Material.ARROW, plugin.getLanguageManager().getMessage("gui-item-back-name")));

        gui.setItem(26, createItem(Material.BARRIER, plugin.getLanguageManager().getMessage("gui-item-delete-name"),
                plugin.getLanguageManager().getMessage("gui-item-delete-lore")));

        player.openInventory(gui);
    }

    private ItemStack getSocialItem(SocialNetwork social) {
        if (social.getItemType().equalsIgnoreCase("HEAD")) {
            return getSkull(social.getItemValue());
        }
        Material mat = Material.matchMaterial(social.getItemValue());
        if (mat == null) mat = Material.PAPER;
        return new ItemStack(mat);
    }

    @SuppressWarnings("deprecation")
    private ItemStack getSkull(String value) {
        Material type = Material.getMaterial("PLAYER_HEAD");
        if (type == null) type = Material.getMaterial("SKULL_ITEM");

        ItemStack skull = new ItemStack(type != null ? type : Material.PAPER, 1, (short) 3);

        if (value == null || value.isEmpty()) return skull;

        org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;

        if (value.length() > 30) {
            // Base64 texture string — apply via GameProfile reflection
            try {
                com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(
                    java.util.UUID.randomUUID(), null
                );
                // Reflects Property constructor — supports old (2-arg) and new (3-arg) authlib
                Object property = null;
                try {
                    property = com.mojang.authlib.properties.Property.class
                        .getConstructor(String.class, String.class)
                        .newInstance("textures", value);
                } catch (Exception ex) {
                    try {
                        // Newer authlib (1.20+): 3-arg constructor with nullable signature
                        property = com.mojang.authlib.properties.Property.class
                            .getConstructor(String.class, String.class, String.class)
                            .newInstance("textures", value, null);
                    } catch (Exception ex2) {
                        plugin.getLogger().warning("Could not create skull Property: " + ex2.getMessage());
                    }
                }
                if (property != null) {
                    profile.getProperties().put("textures",
                        (com.mojang.authlib.properties.Property) property);
                }
                java.lang.reflect.Field profileField = meta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(meta, profile);
            } catch (Exception e) {
                // If authlib is not available (very old server), silently skip
                plugin.getLogger().warning("Could not apply Base64 skull texture: " + e.getMessage());
            }
        } else {
            // Plain player name
            meta.setOwner(value);
        }

        skull.setItemMeta(meta);
        return skull;
    }

    public void openConfirmDelete(Player player, String socialName) {
        String title = plugin.getLanguageManager().getMessage("gui-confirm-delete-title");
        Inventory gui = Bukkit.createInventory(null, 27, title);
        
        SocialNetwork social = plugin.getSocialManager().getSocialNetworks().get(socialName);
        if (social == null) {
            player.sendMessage(ChatColor.RED + "Error: Social network not found for deletion confirmation.");
            player.closeInventory();
            return;
        }

        gui.setItem(11, createItem(Material.EMERALD_BLOCK, plugin.getLanguageManager().getMessage("gui-item-confirm-name")));
        
        ItemStack socialItem = getSocialItem(social);
        ItemMeta socialMeta = socialItem.getItemMeta();
        if (socialMeta != null) {
            socialMeta.setDisplayName(ChatColor.RED + socialName);
            socialItem.setItemMeta(socialMeta);
        }
        gui.setItem(13, socialItem);
        
        gui.setItem(15, createItem(Material.REDSTONE_BLOCK, plugin.getLanguageManager().getMessage("gui-item-cancel-name")));
        
        player.openInventory(gui);
    }

    private void fillBorders(Inventory inv) {
        ItemStack cyan = getGlassPane(9); // 9 is Cyan in Legacy
        ItemStack gray = getGlassPane(7); // 7 is Gray in Legacy
        
        for (int i = 0; i < inv.getSize(); i++) {
            if (isBorder(i, inv.getSize())) {
                inv.setItem(i, (i % 2 == 0) ? cyan : gray);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private ItemStack getGlassPane(int colorData) {
        Material mat = Material.matchMaterial("CYAN_STAINED_GLASS_PANE");
        if (mat == null) {
            // Legacy 1.8 - 1.12
            mat = Material.matchMaterial("STAINED_GLASS_PANE");
            if (mat == null) mat = Material.BARRIER; // Emergency fallback
            return new ItemStack(mat, 1, (short) colorData);
        } else {
            // Modern 1.13+
            String name = (colorData == 9) ? "CYAN_STAINED_GLASS_PANE" : "GRAY_STAINED_GLASS_PANE";
            return new ItemStack(Material.valueOf(name));
        }
    }

    private boolean isBorder(int slot, int size) {
        if (size == 54) {
            return slot < 9 || slot > 44 || slot % 9 == 0 || (slot + 1) % 9 == 0;
        }
        if (size == 36) {
            return slot < 9 || slot > 26 || slot % 9 == 0 || (slot + 1) % 9 == 0;
        }
        if (size == 27) {
            return slot < 9 || slot > 17 || slot % 9 == 0 || (slot + 1) % 9 == 0;
        }
        return false;
    }
    
    private boolean isBorder(int slot) {
        return isBorder(slot, 54);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material != null ? material : Material.STONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> l = new ArrayList<>();
            for (String s : lore) l.add(ChatColor.translateAlternateColorCodes('&', s));
            meta.setLore(l);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material getSignMaterial() {
        Material mat = Material.getMaterial("OAK_SIGN");
        if (mat == null) mat = Material.getMaterial("SIGN");
        return mat != null ? mat : Material.PAPER;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals(plugin.getLanguageManager().getMessage("gui-main-title"))) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || isBorder(event.getRawSlot(), 54)) return;
            
            String name = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            openEditor(player, name);

        } else if (title.contains("Editing: ") || title.contains("Editando: ")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || isBorder(event.getRawSlot(), 36)) return;

            SocialNetworkEditorSession session = editorSessions.get(player.getUniqueId());
            if (session == null) { player.closeInventory(); return; }

            Material type = event.getCurrentItem().getType();
            if (type == Material.PAPER) {
                chatInputContext.put(player.getUniqueId(), "COMMAND");
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Enter new command (or 'cancel'):");
            } else if (type == getSignMaterial()) {
                player.sendMessage(ChatColor.YELLOW + "Name editing is done via files for safety.");
            } else if (type == Material.NAME_TAG) {
                chatInputContext.put(player.getUniqueId(), "PERMISSION");
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Enter permission (or 'none', 'cancel'):");
            } else if (type == Material.BOOK) {
                chatInputContext.put(player.getUniqueId(), "LINK");
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Enter new link (or 'cancel'):");
            } else if (type == Material.FEATHER) {
                chatInputContext.put(player.getUniqueId(), "HOVER");
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Enter hover tooltip message (or 'cancel'):");
            } else if (type == Material.EMERALD) {
                session.save();
                player.sendMessage(ChatColor.GREEN + "Changes saved!");
                editorSessions.remove(player.getUniqueId());
                openMainGUI(player);
            } else if (type == Material.ARROW) {
                editorSessions.remove(player.getUniqueId());
                openMainGUI(player);
            } else if (type == Material.BARRIER) {
                openConfirmDelete(player, session.getOriginalName());
            }
        } else if (title.equals(plugin.getLanguageManager().getMessage("gui-confirm-delete-title"))) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            
            SocialNetworkEditorSession session = editorSessions.get(player.getUniqueId());
            if (session == null) return;
            
            if (event.getCurrentItem().getType() == Material.EMERALD_BLOCK) {
                File file = plugin.getSocialManager().getSocialFile(session.getOriginalName());
                if (file != null && file.delete()) {
                    player.sendMessage(ChatColor.RED + "Social network deleted.");
                    plugin.getSocialManager().reload();
                }
                editorSessions.remove(player.getUniqueId());
                openMainGUI(player);
            } else if (event.getCurrentItem().getType() == Material.REDSTONE_BLOCK) {
                openEditor(player, session.getOriginalName());
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!chatInputContext.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        String message = event.getMessage();
        String context = chatInputContext.remove(player.getUniqueId());

        if (message.equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                SocialNetworkEditorSession s = editorSessions.get(player.getUniqueId());
                if (s != null) openEditor(player, s.getOriginalName());
                else openMainGUI(player);
            });
            return;
        }

        SocialNetworkEditorSession session = editorSessions.get(player.getUniqueId());
        if (session == null) return;

        switch (context) {
            case "COMMAND": session.setCommand(message.split(" ")[0]); break;
            case "PERMISSION": session.setPermission(message.equalsIgnoreCase("none") ? "" : message); break;
            case "LINK": session.setLink(message); break;
            case "HOVER": session.setHover(message); break;
        }

        Bukkit.getScheduler().runTask(plugin, () -> openEditor(player, session.getOriginalName()));
    }
}
