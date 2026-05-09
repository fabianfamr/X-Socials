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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GUIManager implements Listener {

    private final XSocials plugin;
    private final Map<UUID, SocialNetworkEditorSession> editorSessions;
    private final Map<UUID, String> chatInputContext = new ConcurrentHashMap<>();
    private final Map<UUID, Long> closeConfirmTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> ignoreNextClose = new ConcurrentHashMap<>();

    public GUIManager(XSocials plugin) {
        this.plugin = plugin;
        this.editorSessions = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openMainGUI(Player player) {
        String title = plugin.getLanguageManager().getMessage("gui-main-title");
        Inventory gui = Bukkit.createInventory(null, 54, title);

        fillBorders(gui);

        int slot = 10;
        for (SocialNetwork social : plugin.getSocialManager().getAllSocialNetworks()) {
            if (!social.isEnabled())
                continue;

            while (isBorder(slot))
                slot++;
            if (slot >= 44)
                break;

            ItemStack item = getSocialItem(social);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + social.getName());
                meta.setLore(Arrays.asList(
                        plugin.getLanguageManager().getMessage("gui-item-social-command", social.getCommand()),
                        plugin.getLanguageManager().getMessage("gui-item-social-link", social.getLink()),
                        "",
                        plugin.getLanguageManager().getMessage("gui-item-social-edit")));
                item.setItemMeta(meta);
            }
            gui.setItem(slot++, item);
        }

        // Create New button at the bottom center
        gui.setItem(49,
                createItem(Material.EMERALD_BLOCK, plugin.getLanguageManager().getMessage("gui-item-create-name"),
                        plugin.getLanguageManager().getMessage("gui-item-create-lore")));

        player.openInventory(gui);
    }

    public void openEditor(Player player, String socialName) {
        SocialNetwork social = plugin.getSocialManager().getSocialNetworks().values().stream()
                .filter(s -> s.getName().equals(socialName))
                .findFirst().orElse(null);

        if (social == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("gui-error-not-found"));
            return;
        }

        File file = plugin.getSocialManager().getSocialFile(socialName);
        if (file == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("gui-error-file-not-found"));
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
            infoMeta.setLore(
                    Collections.singletonList(plugin.getLanguageManager().getMessage("gui-item-edit-info-lore")));
            infoItem.setItemMeta(infoMeta);
        }
        gui.setItem(4, infoItem);

        // Properties
        gui.setItem(10, createItem(Material.PAPER, plugin.getLanguageManager().getMessage("gui-item-edit-command-name"),
                plugin.getLanguageManager().getMessage("gui-item-edit-command-lore", session.getCommand())));

        gui.setItem(11, createItem(getSignMaterial(), plugin.getLanguageManager().getMessage("gui-item-edit-name-name"),
                plugin.getLanguageManager().getMessage("gui-item-edit-name-lore", session.getName())));

        gui.setItem(12,
                createItem(Material.NAME_TAG, plugin.getLanguageManager().getMessage("gui-item-edit-permission-name"),
                        plugin.getLanguageManager().getMessage("gui-item-edit-permission-lore",
                                (session.getPermission() == null || session.getPermission().isEmpty()) ? "None"
                                        : session.getPermission())));

        gui.setItem(13, createItem(Material.BOOK, plugin.getLanguageManager().getMessage("gui-item-edit-link-name"),
                plugin.getLanguageManager().getMessage("gui-item-edit-link-lore", session.getLink())));

        gui.setItem(14, createItem(Material.FEATHER, plugin.getLanguageManager().getMessage("gui-item-edit-hover-name"),
                plugin.getLanguageManager().getMessage("gui-item-edit-hover-lore",
                        session.getHover().isEmpty() ? "None" : session.getHover())));

        gui.setItem(15,
                createItem(Material.GOLD_INGOT, plugin.getLanguageManager().getMessage("gui-item-edit-rewards-name"),
                        plugin.getLanguageManager().getMessage("gui-item-edit-rewards-lore")));

        // Actions
        gui.setItem(22, createItem(Material.EMERALD, plugin.getLanguageManager().getMessage("gui-item-save-name"),
                plugin.getLanguageManager().getMessage("gui-item-save-lore")));

        gui.setItem(19, createItem(Material.ARROW, plugin.getLanguageManager().getMessage("gui-item-back-name")));

        gui.setItem(25, createItem(Material.BARRIER, plugin.getLanguageManager().getMessage("gui-item-delete-name"),
                plugin.getLanguageManager().getMessage("gui-item-delete-lore")));

        player.openInventory(gui);
    }

    private ItemStack getSocialItem(SocialNetwork social) {
        if ("HEAD".equalsIgnoreCase(social.getItemType())) {
            return getSkull(social.getItemValue());
        }
        Material mat = Material.matchMaterial(social.getItemValue());
        if (mat == null)
            mat = Material.PAPER;
        return new ItemStack(mat);
    }

    @SuppressWarnings("deprecation")
    private ItemStack getSkull(String value) {
        Material type = Material.getMaterial("PLAYER_HEAD");
        if (type == null)
            type = Material.getMaterial("SKULL_ITEM");

        ItemStack skull = new ItemStack(type != null ? type : Material.PAPER, 1, (short) 3);

        if (value == null || value.isEmpty())
            return skull;

        org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) skull.getItemMeta();
        if (meta == null)
            return skull;

        if (value.length() > 30) {
            try {
                com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(
                        java.util.UUID.randomUUID(), null);
                Object property = null;
                try {
                    property = com.mojang.authlib.properties.Property.class
                            .getConstructor(String.class, String.class)
                            .newInstance("textures", value);
                } catch (Exception ex) {
                    try {
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
            player.sendMessage(plugin.getLanguageManager().getMessage("gui-error-not-found"));
            player.closeInventory();
            return;
        }

        gui.setItem(11,
                createItem(Material.EMERALD_BLOCK, plugin.getLanguageManager().getMessage("gui-item-confirm-name")));

        ItemStack socialItem = getSocialItem(social);
        ItemMeta socialMeta = socialItem.getItemMeta();
        if (socialMeta != null) {
            socialMeta.setDisplayName(ChatColor.RED + socialName);
            socialItem.setItemMeta(socialMeta);
        }
        gui.setItem(13, socialItem);

        gui.setItem(15,
                createItem(Material.REDSTONE_BLOCK, plugin.getLanguageManager().getMessage("gui-item-cancel-name")));

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
            if (mat == null)
                mat = Material.BARRIER; // Emergency fallback
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
            for (String s : lore)
                l.add(ChatColor.translateAlternateColorCodes('&', s));
            meta.setLore(l);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material getSignMaterial() {
        Material mat = Material.getMaterial("OAK_SIGN");
        if (mat == null)
            mat = Material.getMaterial("SIGN");
        return mat != null ? mat : Material.PAPER;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals(plugin.getLanguageManager().getMessage("gui-main-title"))) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR)
                return;

            if (event.getRawSlot() == 49 && event.getCurrentItem().getType() == Material.EMERALD_BLOCK) {
                chatInputContext.put(player.getUniqueId(), "CREATE_SOCIAL");
                player.closeInventory();
                player.sendMessage(plugin.getLanguageManager().getPrefix() + " "
                        + plugin.getLanguageManager().getMessage("chat-input-create"));
                return;
            }

            if (isBorder(event.getRawSlot(), 54))
                return;
            if (event.getCurrentItem().getItemMeta() == null
                    || event.getCurrentItem().getItemMeta().getDisplayName() == null)
                return;

            String name = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            openEditor(player, name);

        } else if (editorSessions.containsKey(player.getUniqueId()) && plugin.getLanguageManager()
                .getMessage("gui-editor-title", editorSessions.get(player.getUniqueId()).getOriginalName())
                .equals(title)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR
                    || isBorder(event.getRawSlot(), 36))
                return;

            SocialNetworkEditorSession session = editorSessions.get(player.getUniqueId());

            Material type = event.getCurrentItem().getType();
            if (type == Material.PAPER) {
                chatInputContext.put(player.getUniqueId(), "COMMAND");
                player.closeInventory();
                player.sendMessage(plugin.getLanguageManager().getPrefix() + " "
                        + plugin.getLanguageManager().getMessage("chat-input-command"));
            } else if (type == getSignMaterial()) {
                chatInputContext.put(player.getUniqueId(), "NAME");
                player.closeInventory();
                player.sendMessage(plugin.getLanguageManager().getPrefix() + " "
                        + plugin.getLanguageManager().getMessage("chat-input-name"));
            } else if (type == Material.GOLD_INGOT) {
                player.sendMessage(plugin.getLanguageManager().getPrefix() + " "
                        + plugin.getLanguageManager().getMessage("chat-input-files-only"));
            } else if (type == Material.NAME_TAG) {
                chatInputContext.put(player.getUniqueId(), "PERMISSION");
                player.closeInventory();
                player.sendMessage(plugin.getLanguageManager().getPrefix() + " "
                        + plugin.getLanguageManager().getMessage("chat-input-permission"));
            } else if (type == Material.BOOK) {
                chatInputContext.put(player.getUniqueId(), "LINK");
                player.closeInventory();
                player.sendMessage(plugin.getLanguageManager().getPrefix() + " "
                        + plugin.getLanguageManager().getMessage("chat-input-link"));
            } else if (type == Material.FEATHER) {
                chatInputContext.put(player.getUniqueId(), "HOVER");
                player.closeInventory();
                player.sendMessage(plugin.getLanguageManager().getPrefix() + " "
                        + plugin.getLanguageManager().getMessage("chat-input-hover"));
            } else if (type == Material.EMERALD) {
                session.save();
                player.sendMessage(plugin.getLanguageManager().getPrefix() + " "
                        + plugin.getLanguageManager().getMessage("chat-input-saved"));
                closeConfirmTimestamps.remove(player.getUniqueId());
                editorSessions.remove(player.getUniqueId());
                openMainGUI(player);
            } else if (type == Material.ARROW) {
                if (session.hasChanges()) {
                    long now = System.currentTimeMillis();
                    Long lastClose = closeConfirmTimestamps.get(player.getUniqueId());
                    int delaySec = plugin.getConfig().getInt("editor.confirm-close-delay", 5);

                    if (lastClose != null && (now - lastClose) < delaySec * 1000L) {
                        // Second close within delay -> discard
                        closeConfirmTimestamps.remove(player.getUniqueId());
                        editorSessions.remove(player.getUniqueId());
                        player.sendMessage(plugin.getLanguageManager().getPrefix() + " "
                                + plugin.getLanguageManager().getMessage("gui-changes-discarded"));
                        openMainGUI(player);
                    } else {
                        // First close -> warn and stay
                        closeConfirmTimestamps.put(player.getUniqueId(), now);
                        player.sendMessage(plugin.getLanguageManager().getPrefix() + " "
                                + plugin.getLanguageManager().getMessage("gui-unsaved-warning"));
                    }
                } else {
                    closeConfirmTimestamps.remove(player.getUniqueId());
                    editorSessions.remove(player.getUniqueId());
                    openMainGUI(player);
                }
            } else if (type == Material.BARRIER) {
                ignoreNextClose.put(player.getUniqueId(), true);
                openConfirmDelete(player, session.getOriginalName());
            }
        } else if (title.equals(plugin.getLanguageManager().getMessage("gui-confirm-delete-title"))) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null)
                return;

            SocialNetworkEditorSession session = editorSessions.get(player.getUniqueId());
            if (session == null)
                return;

            if (event.getCurrentItem().getType() == Material.EMERALD_BLOCK) {
                File file = plugin.getSocialManager().getSocialFile(session.getOriginalName());
                if (file != null && file.delete()) {
                    player.sendMessage(plugin.getLanguageManager().getPrefix() + " "
                            + plugin.getLanguageManager().getMessage("gui-message-deleted"));
                    plugin.getSocialManager().reload();
                }
                editorSessions.remove(player.getUniqueId());
                openMainGUI(player);
            } else if (event.getCurrentItem().getType() == Material.REDSTONE_BLOCK) {
                openEditor(player, session.getOriginalName());
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!chatInputContext.containsKey(player.getUniqueId()))
            return;

        event.setCancelled(true);
        String message = event.getMessage();
        String context = chatInputContext.remove(player.getUniqueId());

        if (message.equalsIgnoreCase("cancel")) {
            com.fabian.xsocials.utils.SchedulerUtils.runEntity(plugin, player, () -> {
                SocialNetworkEditorSession s = editorSessions.get(player.getUniqueId());
                if (s != null)
                    openEditor(player, s.getOriginalName());
                else
                    openMainGUI(player);
            });
            return;
        }

        if (context.equals("CREATE_SOCIAL")) {
            if (!message.matches("^[a-zA-Z0-9_]+$")) {
                player.sendMessage(plugin.getLanguageManager().getMessage("chat-input-invalid-name"));
                com.fabian.xsocials.utils.SchedulerUtils.runEntity(plugin, player, () -> openMainGUI(player));
                return;
            }
            com.fabian.xsocials.utils.SchedulerUtils.runAsync(plugin, () -> {
                plugin.getSocialManager().createNewSocial(message.toLowerCase());
                String socialName = message.toLowerCase();
                com.fabian.xsocials.utils.SchedulerUtils.runEntity(plugin, player, () -> {
                    if (!player.isOnline())
                        return;
                    plugin.getSocialManager().reload();
                    openEditor(player, socialName);
                });
            });
            return;
        }

        SocialNetworkEditorSession session = editorSessions.get(player.getUniqueId());
        if (session == null)
            return;

        switch (context) {
            case "COMMAND":
                String cmd = message.split(" ")[0];
                if (cmd.startsWith("/"))
                    cmd = cmd.substring(1);
                session.setCommand(cmd);
                break;
            case "NAME":
                session.setName(message);
                break;
            case "PERMISSION":
                session.setPermission(message.equalsIgnoreCase("none") ? "" : message);
                break;
            case "LINK":
                session.setLink(message);
                break;
            case "HOVER":
                session.setHover(message);
                break;
        }

        com.fabian.xsocials.utils.SchedulerUtils.runEntity(plugin, player,
                () -> openEditor(player, session.getOriginalName()));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player))
            return;
        Player player = (Player) event.getPlayer();

        // Skip if the close was triggered by a chat input button
        if (chatInputContext.containsKey(player.getUniqueId()))
            return;

        // Skip if transitioning to another GUI internally (like confirm delete)
        if (ignoreNextClose.remove(player.getUniqueId()) != null)
            return;

        if (!editorSessions.containsKey(player.getUniqueId()))
            return;

        SocialNetworkEditorSession session = editorSessions.get(player.getUniqueId());
        if (session == null)
            return;

        String title = event.getView().getTitle();
        String sessionTitle = plugin.getLanguageManager().getMessage("gui-editor-title", session.getOriginalName());
        if (!title.equals(sessionTitle))
            return;

        if (session.hasChanges()) {
            long now = System.currentTimeMillis();
            Long lastClose = closeConfirmTimestamps.get(player.getUniqueId());
            int delaySec = plugin.getConfig().getInt("editor.confirm-close-delay", 5);

            if (lastClose != null && (now - lastClose) < delaySec * 1000L) {
                // Second close within delay -> discard for real
                closeConfirmTimestamps.remove(player.getUniqueId());
                editorSessions.remove(player.getUniqueId());
                player.sendMessage(plugin.getLanguageManager().getPrefix() + " "
                        + plugin.getLanguageManager().getMessage("gui-changes-discarded"));
            } else {
                // First close -> reopen editor
                closeConfirmTimestamps.put(player.getUniqueId(), now);
                player.sendMessage(plugin.getLanguageManager().getPrefix() + " "
                        + plugin.getLanguageManager().getMessage("gui-unsaved-warning"));
                com.fabian.xsocials.utils.SchedulerUtils.runEntity(plugin, player,
                        () -> openEditor(player, session.getOriginalName()));
            }
        } else {
            // No changes, just clean up
            closeConfirmTimestamps.remove(player.getUniqueId());
            editorSessions.remove(player.getUniqueId());
        }
    }
}
