package org.little100.constructionWand.wand;

import org.bukkit.Material;

public enum WandType {

    STONE(
            "stone",
            "石制建筑之杖",
            Material.STONE_PICKAXE,
            Material.COBBLESTONE,
            131,
            9,
            101,
            "constructionwand.use.stone"),

    IRON(
            "iron",
            "铁制建筑之杖",
            Material.IRON_PICKAXE,
            Material.IRON_INGOT,
            250,
            27,
            101,
            "constructionwand.use.iron"),

    DIAMOND(
            "diamond",
            "钻石建筑之杖",
            Material.DIAMOND_PICKAXE,
            Material.DIAMOND,
            1561,
            128,
            101,
            "constructionwand.use.diamond"),

    NETHERITE(
            "netherite",
            "下界合金建筑之杖",
            Material.NETHERITE_PICKAXE,
            Material.NETHERITE_INGOT,
            2031,
            256,
            101,
            "constructionwand.use.netherite"),

    INFINITY(
            "infinity",
            "无限建筑之杖",
            Material.NETHERITE_PICKAXE,
            Material.NETHER_STAR,
            -1,
            1024,
            102,
            "constructionwand.use.infinity");

    private final String id;
    private final String displayName;
    private final Material baseMaterial;
    private final Material craftMaterial;
    private final int maxDurability;
    private final int maxBlocks;
    private final int customModelData;
    private final String permission;

    WandType(String id, String displayName, Material baseMaterial, Material craftMaterial,
            int maxDurability, int maxBlocks, int customModelData, String permission) {
        this.id = id;
        this.displayName = displayName;
        this.baseMaterial = baseMaterial;
        this.craftMaterial = craftMaterial;
        this.maxDurability = maxDurability;
        this.maxBlocks = maxBlocks;
        this.customModelData = customModelData;
        this.permission = permission;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getBaseMaterial() {
        return baseMaterial;
    }

    public Material getCraftMaterial() {
        return craftMaterial;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public int getMaxBlocks() {
        return maxBlocks;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isUnbreakable() {
        return maxDurability == -1;
    }

    public static WandType fromId(String id) {
        for (WandType type : values()) {
            if (type.getId().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }

    public static WandType fromMaterialAndModelData(Material material, int modelData) {
        for (WandType type : values()) {
            if (type.getBaseMaterial() == material && type.getCustomModelData() == modelData) {
                return type;
            }
        }
        return null;
    }
}