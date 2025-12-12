package org.little100.constructionWand.wand;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * 手杖配置管理器
 * 用于从 config.yml 读取手杖配置并覆盖默认值
 */
public class WandConfigManager {

    private final Plugin plugin;
    
    // 配置缓存
    private final Map<String, Boolean> enabledCache = new HashMap<>();
    private final Map<String, Integer> maxBlocksCache = new HashMap<>();
    private final Map<String, Integer> durabilityCache = new HashMap<>();

    public WandConfigManager(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * 加载配置
     */
    public void loadConfig() {
        enabledCache.clear();
        maxBlocksCache.clear();
        durabilityCache.clear();

        FileConfiguration config = plugin.getConfig();
        ConfigurationSection wandsSection = config.getConfigurationSection("wands");
        
        if (wandsSection == null) {
            plugin.getLogger().info("未找到 wands 配置节，使用默认值");
            return;
        }

        for (String wandId : wandsSection.getKeys(false)) {
            ConfigurationSection wandConfig = wandsSection.getConfigurationSection(wandId);
            if (wandConfig == null) continue;

            // 读取 enabled
            if (wandConfig.contains("enabled")) {
                enabledCache.put(wandId, wandConfig.getBoolean("enabled", true));
            }

            // 读取 max-blocks
            if (wandConfig.contains("max-blocks")) {
                maxBlocksCache.put(wandId, wandConfig.getInt("max-blocks"));
            }

            // 读取 durability
            if (wandConfig.contains("durability")) {
                durabilityCache.put(wandId, wandConfig.getInt("durability"));
            }
        }

        plugin.getLogger().info("已加载 " + enabledCache.size() + " 个手杖配置");
    }

    /**
     * 重载配置
     */
    public void reloadConfig() {
        loadConfig();
    }

    /**
     * 检查手杖是否启用
     */
    public boolean isEnabled(WandType type) {
        return enabledCache.getOrDefault(type.getId(), true);
    }

    /**
     * 检查手杖是否启用(通过 ID)
     */
    public boolean isEnabled(String wandId) {
        return enabledCache.getOrDefault(wandId, true);
    }

    /**
     * 获取手杖的最大方块数
     */
    public int getMaxBlocks(WandType type) {
        return maxBlocksCache.getOrDefault(type.getId(), type.getMaxBlocks());
    }

    /**
     * 获取手杖的最大方块数(通过 ID)
     */
    public int getMaxBlocks(String wandId, int defaultValue) {
        return maxBlocksCache.getOrDefault(wandId, defaultValue);
    }

    /**
     * 获取手杖的耐久度
     */
    public int getDurability(WandType type) {
        return durabilityCache.getOrDefault(type.getId(), type.getMaxDurability());
    }

    /**
     * 获取手杖的耐久度(通过 ID)
     */
    public int getDurability(String wandId, int defaultValue) {
        return durabilityCache.getOrDefault(wandId, defaultValue);
    }

    /**
     * 检查手杖是否无限耐久
     */
    public boolean isUnbreakable(WandType type) {
        int durability = getDurability(type);
        return durability == -1;
    }
}