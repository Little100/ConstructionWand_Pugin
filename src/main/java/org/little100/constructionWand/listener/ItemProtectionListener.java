package org.little100.constructionWand.listener;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.little100.constructionWand.enchant.EnchantmentManager;
import org.little100.constructionWand.enchant.WandEnchantment;
import org.little100.constructionWand.i18n.I18nManager;
import org.little100.constructionWand.utils.VersionHelper;
import org.little100.constructionWand.wand.WandItemManager;
import org.little100.constructionWand.wand.WandType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ItemProtectionListener implements Listener {

    private final WandItemManager wandItemManager;
    private final I18nManager i18n;
    private EnchantmentManager enchantmentManager;
    private final Random random = new Random();

    public ItemProtectionListener(WandItemManager wandItemManager, I18nManager i18n) {
        this.wandItemManager = wandItemManager;
        this.i18n = i18n;
    }

    public void setEnchantmentManager(EnchantmentManager enchantmentManager) {
        this.enchantmentManager = enchantmentManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEnchantItem(EnchantItemEvent event) {
        ItemStack item = event.getItem();

        if (!wandItemManager.isWand(item)) {
            return;
        }

        if (enchantmentManager == null) {
            return;
        }

        int expLevelCost = event.getExpLevelCost();

        int enchantLevel = calculateEnchantLevel(expLevelCost);

        final int finalLevel = enchantLevel;
        org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.Bukkit.getPluginManager().getPlugin("ConstructionWand"),
                () -> {

                    enchantmentManager.addEnchantment(item, WandEnchantment.BUILDING_EXTENSION, finalLevel);
                },
                1L);
    }

    private int calculateEnchantLevel(int expLevelCost) {

        if (expLevelCost <= 10) {

            return random.nextInt(100) < 80 ? 1 : 2;
        } else if (expLevelCost <= 20) {

            int roll = random.nextInt(100);
            if (roll < 40)
                return 1;
            else if (roll < 80)
                return 2;
            else
                return 3;
        } else if (expLevelCost <= 25) {

            int roll = random.nextInt(100);
            if (roll < 20)
                return 1;
            else if (roll < 50)
                return 2;
            else if (roll < 80)
                return 3;
            else
                return 4;
        } else {

            int roll = random.nextInt(100);
            if (roll < 10)
                return 1;
            else if (roll < 30)
                return 2;
            else if (roll < 60)
                return 3;
            else if (roll < 85)
                return 4;
            else
                return 5;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory anvil = event.getInventory();
        ItemStack firstItem = anvil.getItem(0);
        ItemStack secondItem = anvil.getItem(1);
        ItemStack result = event.getResult();

        if (firstItem != null && wandItemManager.isWand(firstItem)) {
            WandType firstType = wandItemManager.getWandType(firstItem);

            if (firstType != null) {

                if (secondItem != null && secondItem.getType() != Material.AIR) {

                    if (wandItemManager.isWand(secondItem)) {
                        WandType secondType = wandItemManager.getWandType(secondItem);

                        if (secondType != firstType) {
                            event.setResult(null);
                            return;
                        }

                        if (firstType.isUnbreakable()) {
                            event.setResult(null);
                            return;
                        }

                        ItemStack mergedWand = createMergedWand(firstItem, secondItem, firstType);
                        event.setResult(mergedWand);
                        return;
                    }

                    if (secondItem.getType() == Material.ENCHANTED_BOOK) {

                        if (result != null && result.getType() != Material.AIR) {
                            preserveWandData(result, firstType);
                            event.setResult(result);
                        }
                        return;
                    }

                    event.setResult(null);
                    return;
                }

                if (result != null && result.getType() != Material.AIR) {
                    preserveWandData(result, firstType);
                    event.setResult(result);
                }
            }
        }

        if (secondItem != null && wandItemManager.isWand(secondItem)) {

            if (firstItem == null || !wandItemManager.isWand(firstItem)) {
                event.setResult(null);
            }
        }
    }

    private ItemStack createMergedWand(ItemStack first, ItemStack second, WandType type) {
        ItemStack result = first.clone();
        ItemMeta resultMeta = result.getItemMeta();

        if (resultMeta instanceof Damageable && !type.isUnbreakable()) {
            Damageable resultDamageable = (Damageable) resultMeta;

            int firstDamage = resultDamageable.getDamage();
            int firstRemaining = type.getMaxDurability() - firstDamage;

            ItemMeta secondMeta = second.getItemMeta();
            int secondRemaining = type.getMaxDurability();
            if (secondMeta instanceof Damageable) {
                int secondDamage = ((Damageable) secondMeta).getDamage();
                secondRemaining = type.getMaxDurability() - secondDamage;
            }

            int totalRemaining = firstRemaining + secondRemaining;
            int bonus = (int) (type.getMaxDurability() * 0.05);
            totalRemaining = Math.min(totalRemaining + bonus, type.getMaxDurability());

            int newDamage = type.getMaxDurability() - totalRemaining;
            resultDamageable.setDamage(Math.max(0, newDamage));

            if (secondMeta != null) {
                Map<Enchantment, Integer> secondEnchants = secondMeta.getEnchants();
                for (Map.Entry<Enchantment, Integer> entry : secondEnchants.entrySet()) {
                    Enchantment enchant = entry.getKey();
                    int level = entry.getValue();

                    if (resultMeta.hasEnchant(enchant)) {
                        int currentLevel = resultMeta.getEnchantLevel(enchant);
                        if (level == currentLevel && level < enchant.getMaxLevel()) {

                            resultMeta.addEnchant(enchant, level + 1, true);
                        } else if (level > currentLevel) {

                            resultMeta.addEnchant(enchant, level, true);
                        }
                    } else {

                        resultMeta.addEnchant(enchant, level, true);
                    }
                }
            }
        }

        preserveWandData(resultMeta, type);
        result.setItemMeta(resultMeta);

        return result;
    }

    private void preserveWandData(ItemStack item, WandType type) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            preserveWandData(meta, type);
            item.setItemMeta(meta);
        }
    }

    private void preserveWandData(ItemMeta meta, WandType type) {

        VersionHelper.setCustomModelData(meta, type.getCustomModelData());

        meta.getPersistentDataContainer().set(
                wandItemManager.getWandTypeKey(),
                org.bukkit.persistence.PersistentDataType.STRING,
                type.getId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();

        List<ItemStack> wands = new ArrayList<>();
        WandType wandType = null;
        boolean hasOtherItems = false;

        for (ItemStack item : matrix) {
            if (item != null && item.getType() != Material.AIR) {
                if (wandItemManager.isWand(item)) {
                    WandType type = wandItemManager.getWandType(item);
                    if (wandType == null) {
                        wandType = type;
                    } else if (type != wandType) {

                        inventory.setResult(null);
                        return;
                    }
                    wands.add(item);
                } else {
                    hasOtherItems = true;
                }
            }
        }

        if (!wands.isEmpty() && hasOtherItems) {
            inventory.setResult(null);
            return;
        }

        if (wands.size() == 2 && wandType != null && !wandType.isUnbreakable()) {
            ItemStack mergedWand = createMergedWand(wands.get(0), wands.get(1), wandType);
            inventory.setResult(mergedWand);
            return;
        }

        if (wands.size() == 1 || wands.size() > 2) {
            inventory.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getType() == InventoryType.GRINDSTONE) {

            ItemStack item = event.getCurrentItem();
            if (item != null && wandItemManager.isWand(item)) {

                if (event.getClickedInventory() != null &&
                        event.getClickedInventory().getType() == InventoryType.GRINDSTONE) {

                    if (event.getSlot() < 2) {

                        ItemStack cursor = event.getCursor();
                        if (cursor != null && wandItemManager.isWand(cursor)) {
                            event.setCancelled(true);
                            if (event.getWhoClicked() instanceof Player) {
                                ((Player) event.getWhoClicked()).sendMessage(
                                        i18n.get("message.cannot-grindstone"));
                            }
                        }
                    }
                }
            }

            ItemStack cursor = event.getCursor();
            if (cursor != null && wandItemManager.isWand(cursor)) {
                if (event.getClickedInventory() != null &&
                        event.getClickedInventory().getType() == InventoryType.GRINDSTONE &&
                        event.getSlot() < 2) {
                    event.setCancelled(true);
                    if (event.getWhoClicked() instanceof Player) {
                        ((Player) event.getWhoClicked()).sendMessage(
                                i18n.get("message.cannot-grindstone"));
                    }
                }
            }
        }

        if (event.getInventory().getType() == InventoryType.SMITHING) {
            ItemStack cursor = event.getCursor();

            if (cursor != null && wandItemManager.isWand(cursor)) {
                if (event.getClickedInventory() != null &&
                        event.getClickedInventory().getType() == InventoryType.SMITHING) {
                    event.setCancelled(true);
                }
            }
        }
    }
}