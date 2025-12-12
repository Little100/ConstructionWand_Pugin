package org.little100.constructionWand.gui;

import org.bukkit.Bukkit;
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
import org.little100.constructionWand.wand.WandConfigManager;
import org.little100.constructionWand.wand.WandItemManager;
import org.little100.constructionWand.wand.WandType;

public class WandGUI implements InventoryHolder, Listener {

    private final WandItemManager wandItemManager;
    private final I18nManager i18n;
    private final Inventory inventory;
    private WandConfigManager wandConfigManager;

    private static final String GUI_TITLE_KEY = "gui.wand.title";
    private String guiTitle;

    public WandGUI(WandItemManager wandItemManager, I18nManager i18n) {
        this.wandItemManager = wandItemManager;
        this.i18n = i18n;
        this.guiTitle = i18n.get(GUI_TITLE_KEY);

        this.inventory = Bukkit.createInventory(this, 9, guiTitle);

        initializeItems();
    }

    public void setWandConfigManager(WandConfigManager wandConfigManager) {
        this.wandConfigManager = wandConfigManager;
    }

    private void initializeItems() {

        inventory.clear();

        int slot = 0;
        for (WandType type : WandType.values()) {
            if (slot >= 9)
                break;

            // 检查手杖是否启用
            if (wandConfigManager != null && !wandConfigManager.isEnabled(type)) {
                continue; // 跳过禁用的手杖
            }

            ItemStack wand = wandItemManager.createWand(type);
            inventory.setItem(slot, wand);
            slot++;
        }

        ItemStack filler = createFillerItem();
        for (int i = slot; i < 9; i++) {
            inventory.setItem(i, filler);
        }
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

        if (!(event.getInventory().getHolder() instanceof WandGUI)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null ||
                !(event.getClickedInventory().getHolder() instanceof WandGUI)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        if (clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        if (wandItemManager.isWand(clickedItem)) {
            WandType type = wandItemManager.getWandType(clickedItem);
            if (type != null) {
                // 检查手杖是否启用
                if (wandConfigManager != null && !wandConfigManager.isEnabled(type)) {
                    player.sendMessage(i18n.get("message.wand-disabled"));
                    return;
                }

                if (!player.hasPermission("constructionwand.give") &&
                        !player.hasPermission(type.getPermission())) {
                    player.sendMessage(i18n.get("message.no-permission"));
                    return;
                }

                ItemStack newWand = wandItemManager.createWand(type);
                player.getInventory().addItem(newWand);

                String wandName = i18n.get("wand." + type.getId() + ".name");
                player.sendMessage(i18n.get("message.wand-received", wandName));
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof WandGUI) {
            event.setCancelled(true);
        }
    }

    public String getGuiTitle() {
        return guiTitle;
    }
}