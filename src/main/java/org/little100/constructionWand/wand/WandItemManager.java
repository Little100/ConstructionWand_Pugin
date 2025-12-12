package org.little100.constructionWand.wand;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.little100.constructionWand.i18n.I18nManager;
import org.little100.constructionWand.utils.VersionHelper;

import java.util.ArrayList;
import java.util.List;

public class WandItemManager {

    private final Plugin plugin;
    private final I18nManager i18n;
    private final NamespacedKey wandTypeKey;
    private WandConfigManager configManager;

    // 显示模式常量
    public static final String DISPLAY_MODE_NUMBER = "number";
    public static final String DISPLAY_MODE_LEGACY = "legacy";

    public WandItemManager(Plugin plugin, I18nManager i18n) {
        this.plugin = plugin;
        this.i18n = i18n;
        this.wandTypeKey = new NamespacedKey(plugin, "wand_type");
        this.configManager = null;
    }

    public WandItemManager(Plugin plugin, I18nManager i18n, WandConfigManager configManager) {
        this.plugin = plugin;
        this.i18n = i18n;
        this.wandTypeKey = new NamespacedKey(plugin, "wand_type");
        this.configManager = configManager;
    }

    public void setConfigManager(WandConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * 获取耐久度显示模式
     */
    private String getDurabilityDisplayMode() {
        return plugin.getConfig().getString("display.durability-display", DISPLAY_MODE_NUMBER);
    }

    /**
     * 获取进度条长度
     */
    private int getProgressBarLength() {
        return plugin.getConfig().getInt("display.progress-bar-length", 10);
    }

    /**
     * 生成进度条
     */
    private String generateProgressBar(int current, int max) {
        int length = getProgressBarLength();
        if (max <= 0) return "";
        
        float percent = (float) current / max;
        int filledBars = (int) (length * percent);
        
        String prefix = i18n.get("wand.lore.progress-bar-prefix");
        String suffix = i18n.get("wand.lore.progress-bar-suffix");
        String fullChar = i18n.get("wand.lore.progress-bar-full");
        String emptyChar = i18n.get("wand.lore.progress-bar-empty");
        
        StringBuilder bar = new StringBuilder();
        bar.append(prefix);
        for (int i = 0; i < length; i++) {
            if (i < filledBars) {
                bar.append(fullChar);
            } else {
                bar.append(emptyChar);
            }
        }
        bar.append(suffix);
        return bar.toString();
    }

    public ItemStack createWand(WandType type) {
        return createWandWithMaterial(type, type.getBaseMaterial(), type.getCustomModelData());
    }

    /**
     * 使用自定义材质创建手杖
     * @param type 手杖类型
     * @param material 自定义材质
     * @param customModelData 自定义模型数据
     * @return 创建的手杖物品
     */
    public ItemStack createWandWithMaterial(WandType type, Material material, int customModelData) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null)
            return item;

        String nameKey = "wand." + type.getId() + ".name";
        meta.setDisplayName(i18n.get(nameKey));

        // 从配置获取值
        int maxBlocks = getConfigMaxBlocks(type);
        int maxDurability = getConfigDurability(type);
        boolean isUnbreakable = maxDurability == -1;

        // 获取显示模式
        String displayMode = getDurabilityDisplayMode();

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━");
        lore.add(i18n.get("wand.lore.max-blocks", maxBlocks));
        
        // 根据显示模式添加耐久度信息
        if (isUnbreakable) {
            if (DISPLAY_MODE_LEGACY.equalsIgnoreCase(displayMode)) {
                lore.add(i18n.get("wand.lore.durability-legacy-infinite"));
            } else {
                lore.add(i18n.get("wand.lore.durability-infinite"));
            }
        } else {
            if (DISPLAY_MODE_LEGACY.equalsIgnoreCase(displayMode)) {
                // Legacy 模式：显示 当前/最大 + 进度条
                lore.add(i18n.get("wand.lore.durability-legacy", maxDurability, maxDurability));
                lore.add(generateProgressBar(maxDurability, maxDurability));
            } else {
                // Number 模式：只显示数字
                lore.add(i18n.get("wand.lore.durability", maxDurability));
            }
        }
        
        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━");
        lore.add(i18n.get("wand.lore.usage-1"));
        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━");
        meta.setLore(lore);

        VersionHelper.setCustomModelData(meta, customModelData);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(wandTypeKey, PersistentDataType.STRING, type.getId());

        if (isUnbreakable) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);

        VersionHelper.setCustomModelDataComponent(item, customModelData);

        return item;
    }

    /**
     * 从配置获取最大方块数
     */
    public int getConfigMaxBlocks(WandType type) {
        if (configManager != null) {
            return configManager.getMaxBlocks(type);
        }
        return type.getMaxBlocks();
    }

    /**
     * 从配置获取耐久度
     */
    public int getConfigDurability(WandType type) {
        if (configManager != null) {
            return configManager.getDurability(type);
        }
        return type.getMaxDurability();
    }

    /**
     * 检查手杖是否启用
     */
    public boolean isWandEnabled(WandType type) {
        if (configManager != null) {
            return configManager.isEnabled(type);
        }
        return true;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(wandTypeKey, PersistentDataType.STRING)) {
            return true;
        }

        int modelData = getCustomModelDataFromItem(item);
        return WandType.fromMaterialAndModelData(item.getType(), modelData) != null;
    }

    public WandType getWandType(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(wandTypeKey, PersistentDataType.STRING)) {
            String typeId = pdc.get(wandTypeKey, PersistentDataType.STRING);
            return WandType.fromId(typeId);
        }

        int modelData = getCustomModelDataFromItem(item);
        return WandType.fromMaterialAndModelData(item.getType(), modelData);
    }

    private int getCustomModelDataFromItem(ItemStack item) {

        return VersionHelper.getCustomModelDataFromItem(item);
    }

    public int getCurrentDurability(ItemStack item) {
        if (item == null)
            return 0;

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            WandType type = getWandType(item);
            if (type != null) {
                int maxDurability = getConfigDurability(type);
                if (maxDurability != -1) {
                    return maxDurability - damageable.getDamage();
                }
            }
        }
        return -1;
    }

    public boolean consumeDurability(ItemStack item, int amount) {
        WandType type = getWandType(item);
        if (type == null)
            return false;
        
        int maxDurability = getConfigDurability(type);
        if (maxDurability == -1)
            return false; // 无限耐久

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            int newDamage = damageable.getDamage() + amount;

            if (newDamage >= maxDurability) {
                return true;
            }

            damageable.setDamage(newDamage);
            item.setItemMeta(meta);
            
            // 更新 lore 显示(包括进度条)
            updateWandDisplay(item);
        }
        return false;
    }

    public void repairWand(ItemStack item, int amount) {
        WandType type = getWandType(item);
        if (type == null)
            return;
        
        int maxDurability = getConfigDurability(type);
        if (maxDurability == -1)
            return; // 无限耐久

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            int newDamage = Math.max(0, damageable.getDamage() - amount);
            damageable.setDamage(newDamage);
            item.setItemMeta(meta);
            
            // 更新 lore 显示(包括进度条)
            updateWandDisplay(item);
        }
    }

    public NamespacedKey getWandTypeKey() {
        return wandTypeKey;
    }

    public boolean updateWandDisplay(ItemStack item) {
        WandType type = getWandType(item);
        if (type == null)
            return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;

        String nameKey = "wand." + type.getId() + ".name";
        meta.setDisplayName(i18n.get(nameKey));

        // 从配置获取值
        int baseMaxBlocks = getConfigMaxBlocks(type);
        int maxDurability = getConfigDurability(type);
        boolean isUnbreakable = maxDurability == -1;

        // 获取当前耐久度
        int currentDurability = getCurrentDurability(item);
        if (currentDurability == -1) {
            currentDurability = maxDurability; // 无限耐久或新物品
        }

        // 获取显示模式
        String displayMode = getDurabilityDisplayMode();

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━");

        int maxBlocks = baseMaxBlocks;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey enchantKey = new NamespacedKey(plugin, "enchant_building_extension");
        if (pdc.has(enchantKey, PersistentDataType.INTEGER)) {
            int level = pdc.getOrDefault(enchantKey, PersistentDataType.INTEGER, 0);
            if (level > 0) {
                double bonus = level * 0.2;
                maxBlocks = (int) (maxBlocks * (1 + bonus));
            }
        }

        lore.add(i18n.get("wand.lore.max-blocks", maxBlocks));
        
        // 根据显示模式添加耐久度信息
        if (isUnbreakable) {
            if (DISPLAY_MODE_LEGACY.equalsIgnoreCase(displayMode)) {
                lore.add(i18n.get("wand.lore.durability-legacy-infinite"));
            } else {
                lore.add(i18n.get("wand.lore.durability-infinite"));
            }
        } else {
            if (DISPLAY_MODE_LEGACY.equalsIgnoreCase(displayMode)) {
                // Legacy 模式：显示 当前/最大 + 进度条
                lore.add(i18n.get("wand.lore.durability-legacy", currentDurability, maxDurability));
                lore.add(generateProgressBar(currentDurability, maxDurability));
            } else {
                // Number 模式：只显示最大耐久度
                lore.add(i18n.get("wand.lore.durability", maxDurability));
            }
        }
        
        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━");
        lore.add(i18n.get("wand.lore.usage-1"));
        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━");

        if (pdc.has(enchantKey, PersistentDataType.INTEGER)) {
            int level = pdc.getOrDefault(enchantKey, PersistentDataType.INTEGER, 0);
            if (level > 0) {
                String enchantName = i18n.get("enchant.building_extension.name");
                String levelStr = toRomanNumeral(level);
                lore.add(enchantName + " " + levelStr);
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return true;
    }

    private String toRomanNumeral(int number) {
        String[] numerals = { "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X" };
        if (number >= 1 && number <= 10) {
            return numerals[number];
        }
        return String.valueOf(number);
    }

    public int updateAllWandsInInventory(org.bukkit.entity.Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isWand(item)) {
                if (updateWandDisplay(item)) {
                    count++;
                }
            }
        }
        return count;
    }
}