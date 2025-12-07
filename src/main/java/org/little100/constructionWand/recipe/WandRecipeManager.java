package org.little100.constructionWand.recipe;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;
import org.little100.constructionWand.wand.WandItemManager;
import org.little100.constructionWand.wand.WandType;

public class WandRecipeManager {

    private final Plugin plugin;
    private final WandItemManager wandItemManager;

    public WandRecipeManager(Plugin plugin, WandItemManager wandItemManager) {
        this.plugin = plugin;
        this.wandItemManager = wandItemManager;
    }

    public void registerAllRecipes() {
        for (WandType type : WandType.values()) {
            registerRecipe(type);
        }
        plugin.getLogger().info("已注册 " + WandType.values().length + " 个建筑之杖合成配方");
    }

    private void registerRecipe(WandType type) {
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
            plugin.getLogger().info("已注册合成配方: " + type.getDisplayName());
        } catch (Exception e) {
            plugin.getLogger().warning("注册合成配方失败: " + type.getDisplayName() + " - " + e.getMessage());
        }
    }

    public void unregisterAllRecipes() {
        for (WandType type : WandType.values()) {
            NamespacedKey key = new NamespacedKey(plugin, "wand_" + type.getId());
            Bukkit.removeRecipe(key);
        }
    }

    public void reloadRecipes() {
        unregisterAllRecipes();
        registerAllRecipes();
    }
}