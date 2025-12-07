package org.little100.constructionWand.enchant;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.little100.constructionWand.i18n.I18nManager;
import org.little100.constructionWand.wand.WandItemManager;
import org.little100.constructionWand.wand.WandType;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class EnchantmentManager {

    private final Plugin plugin;
    private final I18nManager i18n;
    private final Map<WandEnchantment, NamespacedKey> enchantKeys = new HashMap<>();
    private WandItemManager wandItemManager;

    private FileConfiguration enchantConfig;
    private final Map<String, Map<Integer, Double>> enchantLevelBonuses = new HashMap<>();
    private final Map<String, Double> enchantDefaultIncrements = new HashMap<>();
    private final Map<String, Integer> enchantMaxLevels = new HashMap<>();
    private final Map<String, Boolean> enchantEnabled = new HashMap<>();

    private static final String ENCHANT_PREFIX = "§9";

    public EnchantmentManager(Plugin plugin, I18nManager i18n) {
        this.plugin = plugin;
        this.i18n = i18n;

        for (WandEnchantment enchant : WandEnchantment.values()) {
            enchantKeys.put(enchant, new NamespacedKey(plugin, "enchant_" + enchant.getId()));
        }

        loadEnchantmentConfig();
    }

    public void setWandItemManager(WandItemManager wandItemManager) {
        this.wandItemManager = wandItemManager;
    }

    public void loadEnchantmentConfig() {

        File enchantDir = new File(plugin.getDataFolder(), "enchant");
        if (!enchantDir.exists()) {
            enchantDir.mkdirs();
        }

        for (WandEnchantment enchant : WandEnchantment.values()) {
            loadEnchantmentSettings(enchant);
        }

        plugin.getLogger().info("附魔配置已加载");
    }

    private void loadEnchantmentSettings(WandEnchantment enchant) {
        String id = enchant.getId();
        String configPath = "enchant/" + id + ".yml";
        File configFile = new File(plugin.getDataFolder(), configPath);

        if (!configFile.exists()) {

            if (plugin.getResource(configPath) != null) {
                plugin.saveResource(configPath, false);
            } else {

                createDefaultEnchantConfig(enchant, configFile);
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        InputStream defaultStream = plugin.getResource(configPath);
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig);
        }

        enchantEnabled.put(id, config.getBoolean("enabled", true));
        enchantMaxLevels.put(id, config.getInt("max-level", enchant.getMaxLevel()));
        enchantDefaultIncrements.put(id, config.getDouble("default-increment", 0.15));

        Map<Integer, Double> bonuses = new HashMap<>();
        ConfigurationSection levelsSection = config.getConfigurationSection("levels");
        if (levelsSection != null) {
            for (String key : levelsSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    double bonus = levelsSection.getDouble(key);
                    bonuses.put(level, bonus);
                } catch (NumberFormatException e) {
                    plugin.getLogger().log(Level.WARNING, "无效的附魔等级: " + key + " (附魔: " + id + ")");
                }
            }
        }

        if (bonuses.isEmpty()) {
            for (int i = 1; i <= 10; i++) {
                bonuses.put(i, enchant.getBonusPercentage(i));
            }
        }

        enchantLevelBonuses.put(id, bonuses);

        plugin.getLogger().info("已加载附魔配置: " + id);
    }

    private void createDefaultEnchantConfig(WandEnchantment enchant, File configFile) {
        try {
            configFile.getParentFile().mkdirs();
            YamlConfiguration config = new YamlConfiguration();

            config.options().header("========================================\n" +
                    enchant.getChineseName() + " (" + enchant.getEnglishName() + ") 配置\n" +
                    "========================================");

            config.set("enabled", true);
            config.set("max-level", enchant.getMaxLevel());

            for (int i = 1; i <= 10; i++) {
                config.set("levels." + i, enchant.getBonusPercentage(i));
            }

            config.set("default-increment", 0.15);

            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "无法创建附魔配置文件: " + configFile.getName(), e);
        }
    }

    public void reloadConfig() {
        enchantLevelBonuses.clear();
        enchantDefaultIncrements.clear();
        enchantMaxLevels.clear();
        enchantEnabled.clear();
        loadEnchantmentConfig();
    }

    public boolean isEnchantmentEnabled(WandEnchantment enchant) {
        return enchantEnabled.getOrDefault(enchant.getId(), true);
    }

    public int getMaxLevel(WandEnchantment enchant) {
        return enchantMaxLevels.getOrDefault(enchant.getId(), enchant.getMaxLevel());
    }

    public double getBonusPercentage(WandEnchantment enchant, int level) {
        if (level <= 0)
            return 0;

        String id = enchant.getId();
        Map<Integer, Double> bonuses = enchantLevelBonuses.get(id);

        if (bonuses != null && bonuses.containsKey(level)) {
            return bonuses.get(level);
        }

        if (bonuses != null && !bonuses.isEmpty()) {
            int maxConfiguredLevel = bonuses.keySet().stream().max(Integer::compare).orElse(1);
            double maxBonus = bonuses.getOrDefault(maxConfiguredLevel, 0.0);
            double increment = enchantDefaultIncrements.getOrDefault(id, 0.15);
            return maxBonus + increment * (level - maxConfiguredLevel);
        }

        return enchant.getBonusPercentage(level);
    }

    public boolean addEnchantment(ItemStack item, WandEnchantment enchantment, int level) {
        if (item == null || enchantment == null || level <= 0) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = enchantKeys.get(enchantment);
        pdc.set(key, PersistentDataType.INTEGER, level);

        if (!meta.hasEnchants()) {
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        updateEnchantmentLore(meta, enchantment, level, item);

        item.setItemMeta(meta);
        return true;
    }

    public boolean removeEnchantment(ItemStack item, WandEnchantment enchantment) {
        if (item == null || enchantment == null) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = enchantKeys.get(enchantment);
        if (pdc.has(key, PersistentDataType.INTEGER)) {
            pdc.remove(key);

            removeEnchantmentFromLore(meta, enchantment);

            item.setItemMeta(meta);
            return true;
        }

        return false;
    }

    public int getEnchantmentLevel(ItemStack item, WandEnchantment enchantment) {
        if (item == null || enchantment == null) {
            return 0;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = enchantKeys.get(enchantment);

        if (pdc.has(key, PersistentDataType.INTEGER)) {
            Integer level = pdc.get(key, PersistentDataType.INTEGER);
            return level != null ? level : 0;
        }

        return 0;
    }

    public boolean hasEnchantment(ItemStack item, WandEnchantment enchantment) {
        return getEnchantmentLevel(item, enchantment) > 0;
    }

    public Map<WandEnchantment, Integer> getAllEnchantments(ItemStack item) {
        Map<WandEnchantment, Integer> enchantments = new HashMap<>();

        if (item == null) {
            return enchantments;
        }

        for (WandEnchantment enchant : WandEnchantment.values()) {
            int level = getEnchantmentLevel(item, enchant);
            if (level > 0) {
                enchantments.put(enchant, level);
            }
        }

        return enchantments;
    }

    public int calculateBonusBlocks(ItemStack item, int baseBlocks) {
        int level = getEnchantmentLevel(item, WandEnchantment.BUILDING_EXTENSION);
        if (level <= 0) {
            return baseBlocks;
        }

        double bonus = getBonusPercentage(WandEnchantment.BUILDING_EXTENSION, level);
        return (int) Math.ceil(baseBlocks * (1 + bonus));
    }

    private void updateEnchantmentLore(ItemMeta meta, WandEnchantment enchantment, int level, ItemStack item) {
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }

        String enchantName = i18n.get("enchant." + enchantment.getId() + ".name");
        String levelStr = WandEnchantment.toRomanNumeral(level);
        String enchantLine = ENCHANT_PREFIX + enchantName + " " + levelStr;

        boolean found = false;
        String searchPrefix = ENCHANT_PREFIX + enchantName;

        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            if (line.startsWith(searchPrefix)) {
                lore.set(i, enchantLine);
                found = true;
                break;
            }
        }

        if (!found) {

            int insertIndex = findEnchantInsertIndex(lore);
            lore.add(insertIndex, enchantLine);
        }

        if (enchantment == WandEnchantment.BUILDING_EXTENSION && wandItemManager != null) {
            WandType wandType = wandItemManager.getWandType(item);
            if (wandType != null) {
                int baseBlocks = wandType.getMaxBlocks();
                int bonusBlocks = calculateBonusBlocks(item, baseBlocks);

                String maxBlocksPrefix = ChatColor.stripColor(i18n.get("wand.lore.max-blocks", 0)).split("0")[0];
                for (int i = 0; i < lore.size(); i++) {
                    String line = ChatColor.stripColor(lore.get(i));
                    if (line.contains(maxBlocksPrefix) || line.contains("最大方块") || line.contains("Max Blocks")) {

                        lore.set(i, i18n.get("wand.lore.max-blocks", bonusBlocks));
                        break;
                    }
                }
            }
        }

        meta.setLore(lore);
    }

    private void removeEnchantmentFromLore(ItemMeta meta, WandEnchantment enchantment) {
        List<String> lore = meta.getLore();
        if (lore == null) {
            return;
        }

        String enchantName = i18n.get("enchant." + enchantment.getId() + ".name");
        String searchPrefix = ENCHANT_PREFIX + enchantName;

        lore.removeIf(line -> line.startsWith(searchPrefix));
        meta.setLore(lore);
    }

    private int findEnchantInsertIndex(List<String> lore) {
        for (int i = 0; i < lore.size(); i++) {
            String line = ChatColor.stripColor(lore.get(i));
            if (line.contains("━━━━")) {
                return i + 1;
            }
        }
        return 0;
    }

    public NamespacedKey getEnchantKey(WandEnchantment enchantment) {
        return enchantKeys.get(enchantment);
    }
}