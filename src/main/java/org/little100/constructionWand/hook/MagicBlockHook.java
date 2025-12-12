package org.little100.constructionWand.hook;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * MagicBlock 插件适配器
 * 用于检测和使用 MagicBlock 插件的魔法方块
 */
public class MagicBlockHook {

    private static boolean enabled = false;
    private static Plugin magicBlockPlugin = null;
    private static Object blockManager = null;
    private static Method isMagicBlockMethod = null;
    private static Method getUseTimesMethod = null;
    private static Method decrementUseTimesMethod = null;
    private static Method getMaxUseTimesMethod = null;

    /**
     * 初始化 MagicBlock 适配器
     * @param plugin 主插件实例
     * @return 是否成功初始化
     */
    public static boolean init(Plugin plugin) {
        try {
            magicBlockPlugin = Bukkit.getPluginManager().getPlugin("MagicBlock");
            if (magicBlockPlugin == null || !magicBlockPlugin.isEnabled()) {
                plugin.getLogger().info("MagicBlock 插件未找到或未启用，跳过适配");
                return false;
            }

            // 通过反射获取 BlockManager
            Method getBlockManagerMethod = magicBlockPlugin.getClass().getMethod("getBlockManager");
            blockManager = getBlockManagerMethod.invoke(magicBlockPlugin);

            if (blockManager == null) {
                plugin.getLogger().warning("无法获取 MagicBlock 的 BlockManager");
                return false;
            }

            // 获取需要的方法
            Class<?> blockManagerClass = blockManager.getClass();
            isMagicBlockMethod = blockManagerClass.getMethod("isMagicBlock", ItemStack.class);
            getUseTimesMethod = blockManagerClass.getMethod("getUseTimes", ItemStack.class);
            decrementUseTimesMethod = blockManagerClass.getMethod("decrementUseTimes", ItemStack.class);
            getMaxUseTimesMethod = blockManagerClass.getMethod("getMaxUseTimes", ItemStack.class);

            enabled = true;
            plugin.getLogger().info("MagicBlock 适配器已成功初始化！");
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "初始化 MagicBlock 适配器失败: " + e.getMessage());
            enabled = false;
            return false;
        }
    }

    /**
     * 检查是否已启用 MagicBlock 适配
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * 检查物品是否是魔法方块
     * @param item 要检查的物品
     * @return 是否是魔法方块
     */
    public static boolean isMagicBlock(ItemStack item) {
        if (!enabled || item == null || blockManager == null) {
            return false;
        }
        try {
            return (boolean) isMagicBlockMethod.invoke(blockManager, item);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取魔法方块的剩余使用次数
     * @param item 魔法方块物品
     * @return 剩余使用次数，-1 表示无限
     */
    public static int getUseTimes(ItemStack item) {
        if (!enabled || item == null || blockManager == null) {
            return 0;
        }
        try {
            int times = (int) getUseTimesMethod.invoke(blockManager, item);
            // MagicBlock 使用 Integer.MAX_VALUE - 100 表示无限
            if (times >= Integer.MAX_VALUE - 100) {
                return -1;
            }
            return times;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 获取魔法方块的最大使用次数
     * @param item 魔法方块物品
     * @return 最大使用次数
     */
    public static int getMaxUseTimes(ItemStack item) {
        if (!enabled || item == null || blockManager == null) {
            return 0;
        }
        try {
            int times = (int) getMaxUseTimesMethod.invoke(blockManager, item);
            if (times >= Integer.MAX_VALUE - 100) {
                return -1;
            }
            return times;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 减少魔法方块的使用次数
     * @param item 魔法方块物品
     * @return 剩余使用次数
     */
    public static int decrementUseTimes(ItemStack item) {
        if (!enabled || item == null || blockManager == null) {
            return 0;
        }
        try {
            return (int) decrementUseTimesMethod.invoke(blockManager, item);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 在玩家背包中查找指定材质的魔法方块
     * @param player 玩家
     * @param material 要查找的材质
     * @return 找到的魔法方块物品，如果没有则返回 null
     */
    public static ItemStack findMagicBlockInInventory(Player player, Material material) {
        if (!enabled || player == null || material == null) {
            return null;
        }

        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material && isMagicBlock(item)) {
                int useTimes = getUseTimes(item);
                // 检查是否有剩余使用次数(-1 表示无限)
                if (useTimes == -1 || useTimes > 0) {
                    return item;
                }
            }
        }
        return null;
    }

    /**
     * 统计玩家背包中指定材质的魔法方块可用次数
     * @param player 玩家
     * @param material 要统计的材质
     * @return 可用次数总和(无限返回 Integer.MAX_VALUE)
     */
    public static int countMagicBlockUsesInInventory(Player player, Material material) {
        if (!enabled || player == null || material == null) {
            return 0;
        }

        int totalUses = 0;
        PlayerInventory inventory = player.getInventory();
        
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material && isMagicBlock(item)) {
                int useTimes = getUseTimes(item);
                if (useTimes == -1) {
                    // 无限使用
                    return Integer.MAX_VALUE;
                }
                totalUses += useTimes;
            }
        }
        return totalUses;
    }

    /**
     * 消耗玩家背包中的魔法方块使用次数
     * @param player 玩家
     * @param material 要消耗的材质
     * @param amount 要消耗的数量
     * @return 实际消耗的数量
     */
    public static int consumeMagicBlockUses(Player player, Material material, int amount) {
        if (!enabled || player == null || material == null || amount <= 0) {
            return 0;
        }

        int consumed = 0;
        PlayerInventory inventory = player.getInventory();

        for (int i = 0; i < inventory.getSize() && consumed < amount; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() == material && isMagicBlock(item)) {
                int useTimes = getUseTimes(item);
                
                if (useTimes == -1) {
                    // 无限使用，直接消耗所需数量
                    consumed = amount;
                    break;
                }

                while (useTimes > 0 && consumed < amount) {
                    decrementUseTimes(item);
                    useTimes--;
                    consumed++;
                }

                // 如果使用次数耗尽，移除物品
                if (useTimes <= 0 && getUseTimes(item) <= 0) {
                    inventory.setItem(i, null);
                }
            }
        }

        return consumed;
    }

    /**
     * 检查玩家是否有 MagicBlock 使用权限
     * @param player 玩家
     * @return 是否有权限
     */
    public static boolean hasUsePermission(Player player) {
        if (player == null) {
            return false;
        }
        return player.hasPermission("magicblock.use");
    }
}