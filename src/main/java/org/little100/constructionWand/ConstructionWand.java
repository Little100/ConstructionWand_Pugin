package org.little100.constructionWand;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.little100.constructionWand.action.WandAction;
import org.little100.constructionWand.command.WandCommand;
import org.little100.constructionWand.enchant.EnchantmentManager;
import org.little100.constructionWand.hook.MagicBlockHook;
import org.little100.constructionWand.i18n.I18nManager;
import org.little100.constructionWand.listener.ItemProtectionListener;
import org.little100.constructionWand.listener.WandListener;
import org.little100.constructionWand.preview.PreviewManager;
import org.little100.constructionWand.protection.ProtectionChecker;
import org.little100.constructionWand.recipe.WandRecipeManager;
import org.little100.constructionWand.utils.VersionHelper;
import org.little100.constructionWand.wand.WandConfigManager;
import org.little100.constructionWand.wand.WandItemManager;

public final class ConstructionWand extends JavaPlugin {

    private static ConstructionWand instance;

    private I18nManager i18nManager;
    private WandConfigManager wandConfigManager;
    private WandItemManager wandItemManager;
    private WandRecipeManager recipeManager;
    private ProtectionChecker protectionChecker;
    private PreviewManager previewManager;
    private WandAction wandAction;
    private EnchantmentManager enchantmentManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        getLogger().info("========================================");
        getLogger().info("Construction Wand v" + getDescription().getVersion());
        getLogger().info(VersionHelper.getVersionInfo());
        getLogger().info("========================================");

        i18nManager = new I18nManager(this);

        loadConfiguration();

        initManagers();

        recipeManager.registerAllRecipes();

        registerListeners();

        registerCommands();

        getLogger().info("Construction Wand 已成功启用！");
    }

    @Override
    public void onDisable() {

        if (previewManager != null) {
            previewManager.clearAllPreviews();
        }

        if (recipeManager != null) {
            recipeManager.unregisterAllRecipes();
        }

        getLogger().info("Construction Wand 已禁用！");
    }

    private void initManagers() {
        // 首先初始化配置管理器
        wandConfigManager = new WandConfigManager(this);
        
        wandItemManager = new WandItemManager(this, i18nManager, wandConfigManager);
        recipeManager = new WandRecipeManager(this, wandItemManager);
        protectionChecker = new ProtectionChecker(this);
        previewManager = new PreviewManager(this);
        wandAction = new WandAction(wandItemManager, protectionChecker);
        enchantmentManager = new EnchantmentManager(this, i18nManager);

        wandAction.setEnchantmentManager(enchantmentManager);
        wandAction.setWandConfigManager(wandConfigManager);

        enchantmentManager.setWandItemManager(wandItemManager);

        // 初始化 MagicBlock 适配器
        initMagicBlockHook();
    }

    private void initMagicBlockHook() {
        FileConfiguration config = getConfig();
        
        // 检查是否启用 MagicBlock 适配
        boolean enableMagicBlock = config.getBoolean("magicblock.enabled", true);
        if (!enableMagicBlock) {
            getLogger().info("MagicBlock 适配已在配置中禁用");
            return;
        }

        // 初始化 MagicBlock 适配器
        if (MagicBlockHook.init(this)) {
            // 配置 MagicBlock 选项
            boolean useMagicBlockFirst = config.getBoolean("magicblock.use-first", true);
            boolean requirePermission = config.getBoolean("magicblock.require-permission", true);
            
            wandAction.setUseMagicBlockFirst(useMagicBlockFirst);
            wandAction.setRequireMagicBlockPermission(requirePermission);
            
            getLogger().info("MagicBlock 适配已启用 - 优先使用: " + useMagicBlockFirst + ", 需要权限: " + requirePermission);
        }
    }

    private void registerListeners() {

        WandListener wandListener = new WandListener(wandItemManager, wandAction, previewManager, i18nManager);
        wandListener.setPlugin(this);
        getServer().getPluginManager().registerEvents(wandListener, this);

        ItemProtectionListener itemProtectionListener = new ItemProtectionListener(wandItemManager, i18nManager);
        itemProtectionListener.setEnchantmentManager(enchantmentManager);
        getServer().getPluginManager().registerEvents(itemProtectionListener, this);
    }

    private void registerCommands() {
        WandCommand wandCommand = new WandCommand(this, wandItemManager, i18nManager);
        getCommand("constructionwand").setExecutor(wandCommand);
        getCommand("constructionwand").setTabCompleter(wandCommand);
    }

    public void loadConfiguration() {
        FileConfiguration config = getConfig();

        String language = config.getString("language", "zh_CN");
        i18nManager.loadLanguage(language);

        if (previewManager != null) {

            String colorStr = config.getString("preview.particle-color", "RED");
            Color color = parseColor(colorStr);
            previewManager.setPreviewColor(color);

            String previewMode = config.getString("preview.mode", "full");
            previewManager.setPreviewMode(previewMode.equalsIgnoreCase("outline") ? PreviewManager.PreviewMode.OUTLINE
                    : PreviewManager.PreviewMode.FULL);
        }

        if (protectionChecker != null) {
            boolean useEventCheck = config.getBoolean("protection.use-event-check", true);
            boolean useWorldGuard = config.getBoolean("protection.use-worldguard", true);
            protectionChecker.setUseEventCheck(useEventCheck);
            protectionChecker.setUseWorldGuard(useWorldGuard);
        }

        // 重载手杖配置
        if (wandConfigManager != null) {
            wandConfigManager.reloadConfig();
        }

        // 更新所有在线玩家背包中的手杖显示
        updateAllOnlinePlayersWands();

        getLogger().info("配置已加载");
    }

    /**
     * 更新所有在线玩家背包中的手杖显示
     */
    public void updateAllOnlinePlayersWands() {
        if (wandItemManager == null) return;
        
        int totalUpdated = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            int updated = wandItemManager.updateAllWandsInInventory(player);
            totalUpdated += updated;
        }
        
        if (totalUpdated > 0) {
            getLogger().info("已更新 " + totalUpdated + " 个手杖的显示");
        }
    }

    private Color parseColor(String colorStr) {
        switch (colorStr.toUpperCase()) {
            case "RED":
                return Color.RED;
            case "GREEN":
                return Color.GREEN;
            case "BLUE":
                return Color.BLUE;
            case "YELLOW":
                return Color.YELLOW;
            case "ORANGE":
                return Color.ORANGE;
            case "PURPLE":
                return Color.PURPLE;
            case "WHITE":
                return Color.WHITE;
            case "AQUA":
                return Color.AQUA;
            case "LIME":
                return Color.LIME;
            default:

                try {
                    if (colorStr.startsWith("#")) {
                        colorStr = colorStr.substring(1);
                    }
                    int rgb = Integer.parseInt(colorStr, 16);
                    return Color.fromRGB(rgb);
                } catch (NumberFormatException e) {
                    return Color.RED;
                }
        }
    }

    public static ConstructionWand getInstance() {
        return instance;
    }

    public WandItemManager getWandItemManager() {
        return wandItemManager;
    }

    public WandRecipeManager getRecipeManager() {
        return recipeManager;
    }

    public ProtectionChecker getProtectionChecker() {
        return protectionChecker;
    }

    public PreviewManager getPreviewManager() {
        return previewManager;
    }

    public WandAction getWandAction() {
        return wandAction;
    }

    public I18nManager getI18nManager() {
        return i18nManager;
    }

    public EnchantmentManager getEnchantmentManager() {
        return enchantmentManager;
    }

    public WandConfigManager getWandConfigManager() {
        return wandConfigManager;
    }
}