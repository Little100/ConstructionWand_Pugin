package org.little100.constructionWand.protection;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

public class ProtectionChecker {

    private final Plugin plugin;
    private WorldGuardHook worldGuardHook;
    private boolean useEventCheck = true;
    private boolean useWorldGuard = false;

    public ProtectionChecker(Plugin plugin) {
        this.plugin = plugin;
        initHooks();
    }

    private void initHooks() {

        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            try {
                worldGuardHook = new WorldGuardHook();
                useWorldGuard = true;
                plugin.getLogger().info("已检测到 WorldGuard，启用原生支持");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "WorldGuard 钩子初始化失败", e);
            }
        }

        String[] protectionPlugins = {
                "Residence", "GriefPrevention", "Towny", "Lands",
                "PlotSquared", "RedProtect", "Factions", "FactionsUUID"
        };

        for (String pluginName : protectionPlugins) {
            if (Bukkit.getPluginManager().getPlugin(pluginName) != null) {
                plugin.getLogger().info("已检测到 " + pluginName + "，将通过事件系统兼容");
            }
        }
    }

    public boolean canPlace(Player player, Location location, Material material) {

        if (player.hasPermission("constructionwand.bypass")) {
            return true;
        }

        if (useEventCheck) {
            if (!checkByEvent(player, location, material)) {
                return false;
            }
        }

        if (useWorldGuard && worldGuardHook != null) {
            if (!worldGuardHook.canBuild(player, location)) {
                return false;
            }
        }

        return true;
    }

    private boolean checkByEvent(Player player, Location location, Material material) {
        try {
            Block block = location.getBlock();
            BlockState replacedState = block.getState();

            Block placedAgainst = block.getRelative(org.bukkit.block.BlockFace.DOWN);
            if (placedAgainst.getType() == Material.AIR) {
                placedAgainst = block.getRelative(org.bukkit.block.BlockFace.NORTH);
            }

            ItemStack itemInHand = new ItemStack(material);

            BlockPlaceEvent event = new BlockPlaceEvent(
                    block,
                    replacedState,
                    placedAgainst,
                    itemInHand,
                    player,
                    true,
                    EquipmentSlot.HAND);

            Bukkit.getPluginManager().callEvent(event);

            return !event.isCancelled();

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "事件检查失败", e);

            return true;
        }
    }

    public Location[] filterAllowedLocations(Player player, Location[] locations, Material material) {
        return java.util.Arrays.stream(locations)
                .filter(loc -> canPlace(player, loc, material))
                .toArray(Location[]::new);
    }

    public void setUseEventCheck(boolean useEventCheck) {
        this.useEventCheck = useEventCheck;
    }

    public void setUseWorldGuard(boolean useWorldGuard) {
        this.useWorldGuard = useWorldGuard && worldGuardHook != null;
    }

    public boolean isWorldGuardAvailable() {
        return worldGuardHook != null;
    }
}