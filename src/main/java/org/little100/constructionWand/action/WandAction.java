package org.little100.constructionWand.action;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.BoundingBox;
import org.little100.constructionWand.enchant.EnchantmentManager;
import org.little100.constructionWand.hook.MagicBlockHook;
import org.little100.constructionWand.protection.ProtectionChecker;
import org.little100.constructionWand.wand.WandConfigManager;
import org.little100.constructionWand.wand.WandItemManager;
import org.little100.constructionWand.wand.WandType;

import java.util.*;

public class WandAction {

    private final WandItemManager wandItemManager;
    private final ProtectionChecker protectionChecker;
    private EnchantmentManager enchantmentManager;
    private WandConfigManager wandConfigManager;
    
    // MagicBlock 配置
    private boolean useMagicBlockFirst = true;
    private boolean requireMagicBlockPermission = true;

    public WandAction(WandItemManager wandItemManager, ProtectionChecker protectionChecker) {
        this.wandItemManager = wandItemManager;
        this.protectionChecker = protectionChecker;
    }

    public void setEnchantmentManager(EnchantmentManager enchantmentManager) {
        this.enchantmentManager = enchantmentManager;
    }

    public void setWandConfigManager(WandConfigManager wandConfigManager) {
        this.wandConfigManager = wandConfigManager;
    }

    /**
     * 设置是否优先使用 MagicBlock 的方块
     */
    public void setUseMagicBlockFirst(boolean useMagicBlockFirst) {
        this.useMagicBlockFirst = useMagicBlockFirst;
    }

    /**
     * 设置是否需要 MagicBlock 使用权限
     */
    public void setRequireMagicBlockPermission(boolean requireMagicBlockPermission) {
        this.requireMagicBlockPermission = requireMagicBlockPermission;
    }

    public List<Location> calculatePlaceableLocations(Player player, Block clickedBlock,
            BlockFace clickedFace, ItemStack wandItem) {
        List<Location> locations = new ArrayList<>();

        WandType wandType = wandItemManager.getWandType(wandItem);
        if (wandType == null)
            return locations;

        // 检查手杖是否启用
        if (wandConfigManager != null && !wandConfigManager.isEnabled(wandType)) {
            return locations;
        }

        Material targetMaterial = clickedBlock.getType();
        if (!isPlaceableMaterial(targetMaterial)) {
            return locations;
        }

        int availableBlocks = countAvailableBlocks(player, targetMaterial);
        if (availableBlocks <= 0) {
            return locations;
        }

        // 从配置获取最大方块数
        int baseMaxBlocks = getConfigMaxBlocks(wandType);
        int maxBlocks;
        if (enchantmentManager != null) {
            maxBlocks = enchantmentManager.calculateBonusBlocks(wandItem, baseMaxBlocks);
        } else {
            maxBlocks = baseMaxBlocks;
        }
        maxBlocks = Math.min(maxBlocks, availableBlocks);

        Location startLocation = clickedBlock.getRelative(clickedFace).getLocation();

        if (!canReplace(startLocation.getBlock().getType())) {
            return locations;
        }

        locations = expandPlacementArea(player, clickedBlock, clickedFace, targetMaterial, maxBlocks);

        return locations;
    }

    private List<Location> expandPlacementArea(Player player, Block clickedBlock,
            BlockFace clickedFace, Material material, int maxBlocks) {
        List<Location> result = new ArrayList<>();
        Set<Location> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();

        BlockFace[] expandDirections = getExpandDirections(clickedFace);

        try {
            Block startBlock = clickedBlock.getRelative(clickedFace);
            Material startType = startBlock.getType();
            if (canReplace(startType)) {
                queue.add(startBlock);
                visited.add(startBlock.getLocation());
            }
        } catch (Exception e) {
            return result;
        }

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            Block current = queue.poll();
            if (current == null)
                continue;

            Location currentLoc = current.getLocation();

            try {
                if (!canPlaceAtPrecise(player, currentLoc, material, clickedFace)) {
                    continue;
                }
            } catch (Exception e) {
                continue;
            }

            result.add(currentLoc);

            for (BlockFace direction : expandDirections) {
                try {
                    Block neighbor = current.getRelative(direction);
                    if (neighbor == null)
                        continue;

                    Location neighborLoc = neighbor.getLocation();

                    if (!visited.contains(neighborLoc)) {
                        visited.add(neighborLoc);

                        Material neighborType = neighbor.getType();
                        if (canReplace(neighborType)) {
                            Block supportBlock = neighbor.getRelative(clickedFace.getOppositeFace());
                            if (supportBlock != null && supportBlock.getType() == material) {
                                queue.add(neighbor);
                            }
                        }
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        }

        return result;
    }

    private boolean canPlaceAtPrecise(Player player, Location location, Material material, BlockFace clickedFace) {
        try {
            Block block = location.getBlock();
            if (block == null)
                return false;

            Material blockType = block.getType();
            if (!canReplace(blockType)) {
                return false;
            }

            if (!protectionChecker.canPlace(player, location, material)) {
                return false;
            }

            // 检查是否有实体占据该位置
            if (hasBlockingEntity(location)) {
                return false;
            }

            Block supportBlock = block.getRelative(clickedFace.getOppositeFace());
            if (supportBlock == null)
                return false;

            return supportBlock.getType() == material;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean canPlaceAt(Player player, Location location, Material material, BlockFace face) {
        Block block = location.getBlock();

        if (!canReplace(block.getType())) {
            return false;
        }

        if (!protectionChecker.canPlace(player, location, material)) {
            return false;
        }

        return hasAdjacentBlock(block, material, face);
    }

    private boolean hasAdjacentBlock(Block block, Material material, BlockFace placeFace) {
        BlockFace supportFace = placeFace.getOppositeFace();
        if (block.getRelative(supportFace).getType() == material) {
            return true;
        }

        BlockFace[] expandDirections = getExpandDirections(placeFace);
        for (BlockFace face : expandDirections) {
            if (block.getRelative(face).getType() == material) {
                return true;
            }
        }

        return false;
    }

    private BlockFace[] getExpandDirections(BlockFace clickedFace) {
        switch (clickedFace) {
            case UP:
            case DOWN:
                return new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
            case NORTH:
            case SOUTH:
                return new BlockFace[] { BlockFace.UP, BlockFace.DOWN, BlockFace.EAST, BlockFace.WEST };
            case EAST:
            case WEST:
                return new BlockFace[] { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH };
            default:
                return new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
        }
    }

    public int placeBlocks(Player player, List<Location> locations, Material material, ItemStack wandItem) {
        if (locations == null || locations.isEmpty()) {
            return 0;
        }

        WandType wandType = wandItemManager.getWandType(wandItem);
        if (wandType == null)
            return 0;

        // 检查手杖是否启用
        if (wandConfigManager != null && !wandConfigManager.isEnabled(wandType)) {
            return 0;
        }

        int placed = 0;

        for (Location loc : locations) {
            if (!canReplace(loc.getBlock().getType())) {
                continue;
            }

            if (!hasEnoughBlocks(player, material, 1)) {
                break;
            }

            if (!protectionChecker.canPlace(player, loc, material)) {
                continue;
            }

            // 再次检查实体(因为实体可能在计算后移动到该位置)
            if (hasBlockingEntity(loc)) {
                continue;
            }

            loc.getBlock().setType(material);

            consumeBlocks(player, material, 1);

            placed++;
        }

        // 检查是否无限耐久
        boolean isUnbreakable = isWandUnbreakable(wandType);
        if (placed > 0 && !isUnbreakable) {
            boolean broken = wandItemManager.consumeDurability(wandItem, placed);
            if (broken) {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                wandItem.setAmount(0);
            }
        }

        if (placed > 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_STONE_PLACE, 1.0f, 1.0f);
        }

        return placed;
    }

    private boolean isPlaceableMaterial(Material material) {
        return material.isBlock() && material.isSolid() && !material.isAir();
    }

    /**
     * 从配置获取最大方块数
     */
    private int getConfigMaxBlocks(WandType type) {
        if (wandConfigManager != null) {
            return wandConfigManager.getMaxBlocks(type);
        }
        return type.getMaxBlocks();
    }

    /**
     * 检查手杖是否无限耐久
     */
    private boolean isWandUnbreakable(WandType type) {
        if (wandConfigManager != null) {
            return wandConfigManager.isUnbreakable(type);
        }
        return type.isUnbreakable();
    }

    /**
     * 检查方块是否可以被替换(空气或流体)
     */
    private boolean canReplace(Material material) {
        if (material == null) return false;
        if (material.isAir()) return true;
        // 检查是否是流体
        return isFluid(material);
    }

    /**
     * 检查材质是否是流体
     */
    private boolean isFluid(Material material) {
        if (material == null) return false;
        return material == Material.WATER ||
               material == Material.LAVA ||
               material.name().contains("WATER") ||
               material.name().contains("LAVA");
    }

    /**
     * 检查指定位置是否有阻挡放置的实体
     * @param location 要检查的位置
     * @return 如果有阻挡实体返回 true
     */
    private boolean hasBlockingEntity(Location location) {
        try {
            // 创建方块的碰撞箱
            BoundingBox blockBox = BoundingBox.of(
                location.getBlock().getLocation(),
                location.getBlock().getLocation().clone().add(1, 1, 1)
            );

            // 获取该位置附近的所有实体
            for (Entity entity : location.getWorld().getNearbyEntities(location, 1, 1, 1)) {
                // 跳过不需要检测的实体类型
                if (shouldIgnoreEntity(entity)) {
                    continue;
                }

                // 检查实体的碰撞箱是否与方块位置重叠
                BoundingBox entityBox = entity.getBoundingBox();
                if (blockBox.overlaps(entityBox)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            // 如果检测失败，默认允许放置
            return false;
        }
    }

    /**
     * 检查是否应该忽略该实体
     * @param entity 要检查的实体
     * @return 如果应该忽略返回 true
     */
    private boolean shouldIgnoreEntity(Entity entity) {
        if (entity == null) return true;

        String typeName = entity.getType().name();

        // 忽略的实体类型(使用字符串比较以兼容不同版本)
        switch (typeName) {
            // 掉落物(旧版: DROPPED_ITEM, 新版: ITEM)
            case "DROPPED_ITEM":
            case "ITEM":
            // 经验球
            case "EXPERIENCE_ORB":
            // 箭和投射物
            case "ARROW":
            case "SPECTRAL_ARROW":
            case "TRIDENT":
            case "SNOWBALL":
            case "EGG":
            case "ENDER_PEARL":
            case "FIREBALL":
            case "SMALL_FIREBALL":
            case "DRAGON_FIREBALL":
            case "WITHER_SKULL":
            case "SHULKER_BULLET":
            case "LLAMA_SPIT":
            // 烟花
            case "FIREWORK_ROCKET":
            case "FIREWORK": // 旧版名称
            // 钓鱼浮标(旧版: FISHING_HOOK, 新版: FISHING_BOBBER)
            case "FISHING_BOBBER":
            case "FISHING_HOOK":
            // 末影之眼
            case "EYE_OF_ENDER":
            case "ENDER_SIGNAL": // 旧版名称
            // 药水
            case "POTION":
            case "SPLASH_POTION":
            case "THROWN_EXP_BOTTLE":
            // 闪电
            case "LIGHTNING_BOLT":
            case "LIGHTNING": // 旧版名称
            // 区域效果云
            case "AREA_EFFECT_CLOUD":
            // 末影水晶
            case "END_CRYSTAL":
            case "ENDER_CRYSTAL": // 旧版名称
            // 画和物品展示框
            case "PAINTING":
            case "ITEM_FRAME":
            case "GLOW_ITEM_FRAME":
            // 拴绳结
            case "LEASH_KNOT":
            case "LEASH_HITCH": // 旧版名称
            // 标记实体
            case "MARKER":
            // 展示实体
            case "BLOCK_DISPLAY":
            case "ITEM_DISPLAY":
            case "TEXT_DISPLAY":
            case "INTERACTION":
                return true;
            default:
                // 对于其他实体，检查是否是活的生物
                if (entity instanceof LivingEntity) {
                    return false; // 不忽略活的生物
                }
                return true; // 忽略其他非生物实体
        }
    }

    /**
     * 统计玩家可用的方块数量(包括 MagicBlock 和普通方块)
     */
    public int countAvailableBlocks(Player player, Material material) {
        int magicBlockUses = 0;
        int normalBlocks = 0;

        // 检查 MagicBlock
        if (MagicBlockHook.isEnabled() && canUseMagicBlock(player)) {
            magicBlockUses = MagicBlockHook.countMagicBlockUsesInInventory(player, material);
            if (magicBlockUses == Integer.MAX_VALUE) {
                return Integer.MAX_VALUE; // 无限使用
            }
        }

        // 统计普通方块
        normalBlocks = countNormalMaterialInInventory(player, material);

        return magicBlockUses + normalBlocks;
    }

    /**
     * 检查玩家是否可以使用 MagicBlock
     */
    private boolean canUseMagicBlock(Player player) {
        if (!MagicBlockHook.isEnabled()) {
            return false;
        }
        if (requireMagicBlockPermission && !MagicBlockHook.hasUsePermission(player)) {
            return false;
        }
        return true;
    }

    /**
     * 统计玩家背包中的普通方块数量(排除 MagicBlock)
     */
    public int countNormalMaterialInInventory(Player player, Material material) {
        int count = 0;
        PlayerInventory inventory = player.getInventory();

        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material) {
                // 排除 MagicBlock
                if (MagicBlockHook.isEnabled() && MagicBlockHook.isMagicBlock(item)) {
                    continue;
                }
                count += item.getAmount();
            }
        }

        return count;
    }

    /**
     * 统计玩家背包中的所有方块数量(兼容旧方法)
     */
    public int countMaterialInInventory(Player player, Material material) {
        return countAvailableBlocks(player, material);
    }

    /**
     * 检查玩家是否有足够的方块
     */
    private boolean hasEnoughBlocks(Player player, Material material, int amount) {
        return countAvailableBlocks(player, material) >= amount;
    }

    private boolean hasEnoughMaterial(Player player, Material material, int amount) {
        return hasEnoughBlocks(player, material, amount);
    }

    /**
     * 消耗方块(优先使用 MagicBlock)
     */
    private void consumeBlocks(Player player, Material material, int amount) {
        int remaining = amount;

        // 如果启用了 MagicBlock 且配置为优先使用
        if (useMagicBlockFirst && MagicBlockHook.isEnabled() && canUseMagicBlock(player)) {
            int consumed = MagicBlockHook.consumeMagicBlockUses(player, material, remaining);
            remaining -= consumed;
        }

        // 如果还有剩余，使用普通方块
        if (remaining > 0) {
            removeNormalMaterialFromInventory(player, material, remaining);
        }
    }

    /**
     * 从背包中移除普通方块(排除 MagicBlock)
     */
    private void removeNormalMaterialFromInventory(Player player, Material material, int amount) {
        PlayerInventory inventory = player.getInventory();
        int remaining = amount;

        for (int i = 0; i < inventory.getSize() && remaining > 0; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() == material) {
                // 跳过 MagicBlock
                if (MagicBlockHook.isEnabled() && MagicBlockHook.isMagicBlock(item)) {
                    continue;
                }
                
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    inventory.setItem(i, null);
                    remaining -= itemAmount;
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
        }
    }

    private void removeMaterialFromInventory(Player player, Material material, int amount) {
        consumeBlocks(player, material, amount);
    }
}