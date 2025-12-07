package org.little100.constructionWand;

import org.bukkit.Color;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.little100.constructionWand.action.WandAction;
import org.little100.constructionWand.command.WandCommand;
import org.little100.constructionWand.enchant.EnchantmentManager;
import org.little100.constructionWand.i18n.I18nManager;
import org.little100.constructionWand.listener.ItemProtectionListener;
import org.little100.constructionWand.listener.WandListener;
import org.little100.constructionWand.preview.PreviewManager;
import org.little100.constructionWand.protection.ProtectionChecker;
import org.little100.constructionWand.recipe.WandRecipeManager;
import org.little100.constructionWand.utils.VersionHelper;
import org.little100.constructionWand.wand.WandItemManager;

public final class ConstructionWand extends JavaPlugin {

    private static ConstructionWand instance;

    private I18nManager i18nManager;
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
        wandItemManager = new WandItemManager(this, i18nManager);
        recipeManager = new WandRecipeManager(this, wandItemManager);
        protectionChecker = new ProtectionChecker(this);
        previewManager = new PreviewManager(this);
        wandAction = new WandAction(wandItemManager, protectionChecker);
        enchantmentManager = new EnchantmentManager(this, i18nManager);

        wandAction.setEnchantmentManager(enchantmentManager);

        enchantmentManager.setWandItemManager(wandItemManager);
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

        getLogger().info("配置已加载");
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
}