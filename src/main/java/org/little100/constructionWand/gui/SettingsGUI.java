package org.little100.constructionWand.gui;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.little100.constructionWand.i18n.I18nManager;
import org.little100.constructionWand.preview.PreviewManager;

import java.util.*;

public class SettingsGUI implements InventoryHolder, Listener {

    private final PreviewManager previewManager;
    private final I18nManager i18n;
    private final Inventory inventory;
    private final Player player;

    private static final String GUI_TITLE_KEY = "gui.settings.title";
    private String guiTitle;

    private static final Map<Material, Color> DYE_COLORS = new LinkedHashMap<>();
    private static final Map<Material, String> DYE_NAMES = new LinkedHashMap<>();

    static {

        DYE_COLORS.put(Material.WHITE_DYE, Color.WHITE);
        DYE_COLORS.put(Material.ORANGE_DYE, Color.ORANGE);
        DYE_COLORS.put(Material.MAGENTA_DYE, Color.FUCHSIA);
        DYE_COLORS.put(Material.LIGHT_BLUE_DYE, Color.fromRGB(85, 255, 255));
        DYE_COLORS.put(Material.YELLOW_DYE, Color.YELLOW);
        DYE_COLORS.put(Material.LIME_DYE, Color.LIME);
        DYE_COLORS.put(Material.PINK_DYE, Color.fromRGB(255, 105, 180));
        DYE_COLORS.put(Material.GRAY_DYE, Color.GRAY);
        DYE_COLORS.put(Material.LIGHT_GRAY_DYE, Color.SILVER);
        DYE_COLORS.put(Material.CYAN_DYE, Color.TEAL);
        DYE_COLORS.put(Material.PURPLE_DYE, Color.PURPLE);
        DYE_COLORS.put(Material.BLUE_DYE, Color.BLUE);
        DYE_COLORS.put(Material.BROWN_DYE, Color.fromRGB(139, 69, 19));
        DYE_COLORS.put(Material.GREEN_DYE, Color.GREEN);
        DYE_COLORS.put(Material.RED_DYE, Color.RED);
        DYE_COLORS.put(Material.BLACK_DYE, Color.fromRGB(30, 30, 30));

        DYE_NAMES.put(Material.WHITE_DYE, "白色");
        DYE_NAMES.put(Material.ORANGE_DYE, "橙色");
        DYE_NAMES.put(Material.MAGENTA_DYE, "品红色");
        DYE_NAMES.put(Material.LIGHT_BLUE_DYE, "淡蓝色");
        DYE_NAMES.put(Material.YELLOW_DYE, "黄色");
        DYE_NAMES.put(Material.LIME_DYE, "黄绿色");
        DYE_NAMES.put(Material.PINK_DYE, "粉红色");
        DYE_NAMES.put(Material.GRAY_DYE, "灰色");
        DYE_NAMES.put(Material.LIGHT_GRAY_DYE, "淡灰色");
        DYE_NAMES.put(Material.CYAN_DYE, "青色");
        DYE_NAMES.put(Material.PURPLE_DYE, "紫色");
        DYE_NAMES.put(Material.BLUE_DYE, "蓝色");
        DYE_NAMES.put(Material.BROWN_DYE, "棕色");
        DYE_NAMES.put(Material.GREEN_DYE, "绿色");
        DYE_NAMES.put(Material.RED_DYE, "红色");
        DYE_NAMES.put(Material.BLACK_DYE, "黑色");
    }

    private static final Map<UUID, Color> playerColors = new HashMap<>();

    public SettingsGUI(PreviewManager previewManager, I18nManager i18n, Player player) {
        this.previewManager = previewManager;
        this.i18n = i18n;
        this.player = player;
        this.guiTitle = i18n.get(GUI_TITLE_KEY);

        this.inventory = Bukkit.createInventory(this, 27, guiTitle);

        initializeItems();
    }

    private void initializeItems() {

        inventory.clear();

        inventory.setItem(1, createPreviewModeItem(PreviewManager.PreviewMode.FULL, Material.GLASS));

        inventory.setItem(4, createPreviewModeItem(PreviewManager.PreviewMode.CORNERS, Material.END_ROD));

        inventory.setItem(7, createPreviewModeItem(PreviewManager.PreviewMode.OUTLINE, Material.ITEM_FRAME));

        int slot = 9;
        for (Map.Entry<Material, Color> entry : DYE_COLORS.entrySet()) {
            if (slot >= 27)
                break;
            inventory.setItem(slot, createColorItem(entry.getKey()));
            slot++;
        }

        ItemStack filler = createFillerItem();
        for (int i = 0; i < 27; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    private ItemStack createPreviewModeItem(PreviewManager.PreviewMode mode, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PreviewManager.PreviewMode currentMode = previewManager.getPlayerPreviewMode(player);
            boolean isSelected = currentMode == mode;

            String modeName;
            String modeDesc;
            switch (mode) {
                case FULL:
                    modeName = i18n.get("gui.settings.mode.full.name");
                    modeDesc = i18n.get("gui.settings.mode.full.desc");
                    break;
                case CORNERS:
                    modeName = i18n.get("gui.settings.mode.corners.name");
                    modeDesc = i18n.get("gui.settings.mode.corners.desc");
                    break;
                case OUTLINE:
                    modeName = i18n.get("gui.settings.mode.outline.name");
                    modeDesc = i18n.get("gui.settings.mode.outline.desc");
                    break;
                default:
                    modeName = mode.name();
                    modeDesc = "";
            }

            if (isSelected) {
                modeName = "§a✔ " + modeName;
            }

            meta.setDisplayName(modeName);

            List<String> lore = new ArrayList<>();
            lore.add(modeDesc);
            if (isSelected) {
                lore.add("");
                lore.add(i18n.get("gui.settings.current-selection"));
            } else {
                lore.add("");
                lore.add(i18n.get("gui.settings.click-to-select"));
            }
            meta.setLore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createColorItem(Material dyeMaterial) {
        ItemStack item = new ItemStack(dyeMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String colorName = DYE_NAMES.getOrDefault(dyeMaterial, dyeMaterial.name());
            Color currentColor = getPlayerColor(player);
            Color dyeColor = DYE_COLORS.get(dyeMaterial);
            boolean isSelected = currentColor != null && currentColor.equals(dyeColor);

            String displayName = i18n.get("gui.settings.color.name", colorName);
            if (isSelected) {
                displayName = "§a✔ " + displayName;
            }

            meta.setDisplayName(displayName);

            List<String> lore = new ArrayList<>();
            lore.add(i18n.get("gui.settings.color.desc"));
            if (isSelected) {
                lore.add("");
                lore.add(i18n.get("gui.settings.current-selection"));
            } else {
                lore.add("");
                lore.add(i18n.get("gui.settings.click-to-select"));
            }
            meta.setLore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFillerItem() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        return filler;
    }

    public void openInventory(Player player) {

        initializeItems();
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getInventory().getHolder() instanceof SettingsGUI)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null ||
                !(event.getClickedInventory().getHolder() instanceof SettingsGUI)) {
            return;
        }

        Player clickPlayer = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        int slot = event.getSlot();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        if (clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        if (slot < 9) {
            handlePreviewModeClick(clickPlayer, slot, clickedItem);
        }

        else {
            handleColorClick(clickPlayer, clickedItem);
        }
    }

    private void handlePreviewModeClick(Player player, int slot, ItemStack item) {
        PreviewManager.PreviewMode newMode = null;

        switch (slot) {
            case 1:
                newMode = PreviewManager.PreviewMode.FULL;
                break;
            case 4:
                newMode = PreviewManager.PreviewMode.CORNERS;
                break;
            case 7:
                newMode = PreviewManager.PreviewMode.OUTLINE;
                break;
        }

        if (newMode != null) {

            PreviewManager.PreviewMode currentMode = previewManager.getPlayerPreviewMode(player);
            if (currentMode == newMode) {
                return;
            }

            previewManager.setPlayerPreviewMode(player, newMode);

            String modeName;
            switch (newMode) {
                case FULL:
                    modeName = i18n.get("preview.mode.full");
                    break;
                case CORNERS:
                    modeName = i18n.get("preview.mode.corners");
                    break;
                case OUTLINE:
                    modeName = i18n.get("preview.mode.outline");
                    break;
                default:
                    modeName = newMode.name();
            }

            player.sendMessage(i18n.get("message.preview-mode-changed", modeName));

            initializeItems();
        }
    }

    private void handleColorClick(Player player, ItemStack item) {
        Material dyeMaterial = item.getType();
        Color color = DYE_COLORS.get(dyeMaterial);

        if (color != null) {

            Color currentColor = getPlayerColor(player);
            if (currentColor != null && currentColor.equals(color)) {
                return;
            }

            setPlayerColor(player, color);

            previewManager.setPlayerPreviewColor(player, color);

            String colorName = DYE_NAMES.getOrDefault(dyeMaterial, dyeMaterial.name());
            player.sendMessage(i18n.get("message.color-changed", colorName));

            initializeItems();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SettingsGUI) {
            event.setCancelled(true);
        }
    }

    public String getGuiTitle() {
        return guiTitle;
    }

    public static Color getPlayerColor(Player player) {
        return playerColors.getOrDefault(player.getUniqueId(), Color.RED);
    }

    public static void setPlayerColor(Player player, Color color) {
        playerColors.put(player.getUniqueId(), color);
    }
}