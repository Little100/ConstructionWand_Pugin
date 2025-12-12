package org.little100.constructionWand.recipe;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.little100.constructionWand.utils.VersionHelper;
import org.little100.constructionWand.wand.WandItemManager;
import org.little100.constructionWand.wand.WandType;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Level;

public class WandRecipeManager {

    private final Plugin plugin;
    private final WandItemManager wandItemManager;
    private FileConfiguration craftConfig;
    private final Set<NamespacedKey> registeredRecipes = new HashSet<>();

    public WandRecipeManager(Plugin plugin, WandItemManager wandItemManager) {
        this.plugin = plugin;
        this.wandItemManager = wandItemManager;
        loadCraftConfig();
    }

    /**
     * 加载 craft.yml 配置文件
     */
    private void loadCraftConfig() {
        File craftFile = new File(plugin.getDataFolder(), "craft.yml");
        
        // 如果文件不存在，保存默认配置
        if (!craftFile.exists()) {
            plugin.saveResource("craft.yml", false);
        }
        
        craftConfig = YamlConfiguration.loadConfiguration(craftFile);
        
        // 检查并更新配置
        checkAndUpdateCraftConfig(craftFile);
    }

    /**
     * 检查并更新 craft.yml 配置
     */
    private void checkAndUpdateCraftConfig(File craftFile) {
        InputStream defaultStream = plugin.getResource("craft.yml");
        if (defaultStream == null) return;

        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream));

        boolean updated = false;
        for (String key : defaultConfig.getKeys(true)) {
            if (!craftConfig.contains(key)) {
                craftConfig.set(key, defaultConfig.get(key));
                updated = true;
            }
        }

        if (updated) {
            try {
                craftConfig.save(craftFile);
                plugin.getLogger().info("craft.yml 配置已更新");
            } catch (Exception e) {
                plugin.getLogger().warning("无法保存 craft.yml: " + e.getMessage());
            }
        }
    }

    /**
     * 注册所有合成配方
     */
    public void registerAllRecipes() {
        ConfigurationSection recipesSection = craftConfig.getConfigurationSection("recipes");
        if (recipesSection == null) {
            // 如果没有配置，使用默认的 WandType 注册
            registerDefaultRecipes();
            return;
        }

        int registered = 0;
        for (String recipeId : recipesSection.getKeys(false)) {
            ConfigurationSection recipeSection = recipesSection.getConfigurationSection(recipeId);
            if (recipeSection == null) continue;

            if (registerRecipeFromConfig(recipeId, recipeSection)) {
                registered++;
            }
        }

        plugin.getLogger().info("已从 craft.yml 注册 " + registered + " 个合成配方");
    }

    /**
     * 注册默认配方(当 craft.yml 不存在或为空时)
     */
    private void registerDefaultRecipes() {
        for (WandType type : WandType.values()) {
            registerDefaultRecipe(type);
        }
        plugin.getLogger().info("已注册 " + WandType.values().length + " 个默认建筑之杖合成配方");
    }

    /**
     * 从配置注册单个配方
     */
    private boolean registerRecipeFromConfig(String recipeId, ConfigurationSection config) {
        try {
            // 检查是否启用
            if (!config.getBoolean("enabled", true)) {
                plugin.getLogger().info("配方 " + recipeId + " 已禁用，跳过注册");
                return false;
            }

            // 获取输出物品配置
            ConfigurationSection outputSection = config.getConfigurationSection("output");
            if (outputSection == null) {
                plugin.getLogger().warning("配方 " + recipeId + " 缺少 output 配置");
                return false;
            }

            // 创建输出物品
            ItemStack outputItem = createOutputItem(recipeId, outputSection);
            if (outputItem == null) {
                return false;
            }

            // 创建配方
            NamespacedKey key = new NamespacedKey(plugin, "wand_" + recipeId);
            ShapedRecipe recipe = new ShapedRecipe(key, outputItem);

            // 设置配方形状
            List<String> shape = config.getStringList("shape");
            if (shape.isEmpty() || shape.size() != 3) {
                plugin.getLogger().warning("配方 " + recipeId + " 的 shape 配置无效");
                return false;
            }
            recipe.shape(shape.get(0), shape.get(1), shape.get(2));

            // 设置材料
            ConfigurationSection ingredientsSection = config.getConfigurationSection("ingredients");
            if (ingredientsSection == null) {
                plugin.getLogger().warning("配方 " + recipeId + " 缺少 ingredients 配置");
                return false;
            }

            for (String ingredientKey : ingredientsSection.getKeys(false)) {
                String materialName = null;
                
                // 支持两种格式：
                // 1. 简写格式: S: STICK
                // 2. 完整格式: S: { material: STICK }
                Object ingredientValue = ingredientsSection.get(ingredientKey);
                if (ingredientValue instanceof String) {
                    // 简写格式
                    materialName = (String) ingredientValue;
                } else {
                    // 完整格式
                    ConfigurationSection ingredientConfig = ingredientsSection.getConfigurationSection(ingredientKey);
                    if (ingredientConfig != null) {
                        materialName = ingredientConfig.getString("material");
                    }
                }
                
                if (materialName == null) continue;

                Material material = Material.getMaterial(materialName.toUpperCase());
                if (material == null) {
                    plugin.getLogger().warning("配方 " + recipeId + " 中的材料 " + materialName + " 无效");
                    continue;
                }

                char ingredientChar = ingredientKey.charAt(0);
                recipe.setIngredient(ingredientChar, material);
            }

            // 注册配方
            Bukkit.addRecipe(recipe);
            registeredRecipes.add(key);
            plugin.getLogger().info("已注册合成配方: " + recipeId);
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "注册配方 " + recipeId + " 失败", e);
            return false;
        }
    }

    /**
     * 创建输出物品
     */
    private ItemStack createOutputItem(String recipeId, ConfigurationSection outputConfig) {
        // 获取材质
        String materialName = outputConfig.getString("material");
        if (materialName == null) {
            plugin.getLogger().warning("配方 " + recipeId + " 缺少 material 配置");
            return null;
        }

        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material == null) {
            plugin.getLogger().warning("配方 " + recipeId + " 的材质 " + materialName + " 无效");
            return null;
        }

        // 检查是否是预定义的 WandType
        String wandTypeId = null;
        ConfigurationSection pdcSection = outputConfig.getConfigurationSection("pdc");
        if (pdcSection != null) {
            wandTypeId = pdcSection.getString("wand_type");
        }

        // 如果是预定义的 WandType，使用 WandItemManager 创建(使用配置中的材质)
        if (wandTypeId != null) {
            WandType wandType = WandType.fromId(wandTypeId);
            if (wandType != null) {
                // 获取自定义模型数据(如果配置中有的话)
                int customModelData = outputConfig.getInt("custom-model-data", wandType.getCustomModelData());
                return wandItemManager.createWandWithMaterial(wandType, material, customModelData);
            }
        }

        // 否则创建自定义物品
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // 设置名称
        String name = outputConfig.getString("name", "");
        if (!name.isEmpty()) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        }

        // 设置描述
        List<String> loreList = outputConfig.getStringList("lore");
        if (!loreList.isEmpty()) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : loreList) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(coloredLore);
        }

        // 设置自定义模型数据
        int customModelData = outputConfig.getInt("custom-model-data", 0);
        if (customModelData > 0) {
            VersionHelper.setCustomModelData(meta, customModelData);
        }

        // 设置 PDC 数据
        if (pdcSection != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            for (String pdcKey : pdcSection.getKeys(false)) {
                Object value = pdcSection.get(pdcKey);
                NamespacedKey namespacedKey = new NamespacedKey(plugin, pdcKey);
                
                if (value instanceof String) {
                    pdc.set(namespacedKey, PersistentDataType.STRING, (String) value);
                } else if (value instanceof Integer) {
                    pdc.set(namespacedKey, PersistentDataType.INTEGER, (Integer) value);
                } else if (value instanceof Double) {
                    pdc.set(namespacedKey, PersistentDataType.DOUBLE, (Double) value);
                } else if (value instanceof Boolean) {
                    pdc.set(namespacedKey, PersistentDataType.BYTE, (byte) ((Boolean) value ? 1 : 0));
                }
            }
        }

        item.setItemMeta(meta);
        
        // 设置组件(1.20.5+)
        if (customModelData > 0) {
            VersionHelper.setCustomModelDataComponent(item, customModelData);
        }

        return item;
    }

    /**
     * 注册默认配方
     */
    private void registerDefaultRecipe(WandType type) {
        ItemStack wandItem = wandItemManager.createWand(type);
        NamespacedKey key = new NamespacedKey(plugin, "wand_" + type.getId());

        ShapedRecipe recipe = new ShapedRecipe(key, wandItem);

        recipe.shape(
                "  M",
                " S ",
                "S  ");

        recipe.setIngredient('S', Material.STICK);
        recipe.setIngredient('M', type.getCraftMaterial());

        try {
            Bukkit.addRecipe(recipe);
            registeredRecipes.add(key);
            plugin.getLogger().info("已注册合成配方: " + type.getDisplayName());
        } catch (Exception e) {
            plugin.getLogger().warning("注册合成配方失败: " + type.getDisplayName() + " - " + e.getMessage());
        }
    }

    /**
     * 注销所有配方
     */
    public void unregisterAllRecipes() {
        for (NamespacedKey key : registeredRecipes) {
            Bukkit.removeRecipe(key);
        }
        registeredRecipes.clear();
    }

    /**
     * 重载配方
     */
    public void reloadRecipes() {
        unregisterAllRecipes();
        loadCraftConfig();
        registerAllRecipes();
    }

    /**
     * 获取配置
     */
    public FileConfiguration getCraftConfig() {
        return craftConfig;
    }

    /**
     * 根据 WandType 创建手杖物品（使用 craft.yml 中配置的材质）
     * @param type 手杖类型
     * @return 创建的手杖物品，如果配置中没有该类型则使用默认材质
     */
    public ItemStack createWandFromConfig(WandType type) {
        ConfigurationSection recipesSection = craftConfig.getConfigurationSection("recipes");
        if (recipesSection == null) {
            return wandItemManager.createWand(type);
        }

        ConfigurationSection recipeSection = recipesSection.getConfigurationSection(type.getId());
        if (recipeSection == null) {
            return wandItemManager.createWand(type);
        }

        ConfigurationSection outputSection = recipeSection.getConfigurationSection("output");
        if (outputSection == null) {
            return wandItemManager.createWand(type);
        }

        // 获取配置中的材质
        String materialName = outputSection.getString("material");
        if (materialName == null) {
            return wandItemManager.createWand(type);
        }

        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material == null) {
            return wandItemManager.createWand(type);
        }

        // 获取自定义模型数据
        int customModelData = outputSection.getInt("custom-model-data", type.getCustomModelData());

        return wandItemManager.createWandWithMaterial(type, material, customModelData);
    }

    /**
     * 获取 WandType 在配置中的材质
     * @param type 手杖类型
     * @return 配置中的材质，如果没有配置则返回默认材质
     */
    public Material getConfiguredMaterial(WandType type) {
        ConfigurationSection recipesSection = craftConfig.getConfigurationSection("recipes");
        if (recipesSection == null) {
            return type.getBaseMaterial();
        }

        ConfigurationSection recipeSection = recipesSection.getConfigurationSection(type.getId());
        if (recipeSection == null) {
            return type.getBaseMaterial();
        }

        ConfigurationSection outputSection = recipeSection.getConfigurationSection("output");
        if (outputSection == null) {
            return type.getBaseMaterial();
        }

        String materialName = outputSection.getString("material");
        if (materialName == null) {
            return type.getBaseMaterial();
        }

        Material material = Material.getMaterial(materialName.toUpperCase());
        return material != null ? material : type.getBaseMaterial();
    }

    /**
     * 获取 WandType 在配置中的自定义模型数据
     * @param type 手杖类型
     * @return 配置中的自定义模型数据，如果没有配置则返回默认值
     */
    public int getConfiguredCustomModelData(WandType type) {
        ConfigurationSection recipesSection = craftConfig.getConfigurationSection("recipes");
        if (recipesSection == null) {
            return type.getCustomModelData();
        }

        ConfigurationSection recipeSection = recipesSection.getConfigurationSection(type.getId());
        if (recipeSection == null) {
            return type.getCustomModelData();
        }

        ConfigurationSection outputSection = recipeSection.getConfigurationSection("output");
        if (outputSection == null) {
            return type.getCustomModelData();
        }

        return outputSection.getInt("custom-model-data", type.getCustomModelData());
    }
}