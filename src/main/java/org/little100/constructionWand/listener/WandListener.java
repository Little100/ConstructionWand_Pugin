package org.little100.constructionWand.listener;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.little100.constructionWand.action.WandAction;
import org.little100.constructionWand.i18n.I18nManager;
import org.little100.constructionWand.preview.PreviewManager;
import org.little100.constructionWand.utils.VersionHelper;
import org.little100.constructionWand.wand.WandItemManager;
import org.little100.constructionWand.wand.WandType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WandListener implements Listener {

    private Plugin plugin;
    private final WandItemManager wandItemManager;
    private final WandAction wandAction;
    private final PreviewManager previewManager;
    private final I18nManager i18n;

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private long cooldownTicks = 10; // 默认 10 tick = 0.5 秒
    private long cooldownMs = cooldownTicks * 50;

    private int previewTaskId = -1;

    public WandListener(WandItemManager wandItemManager, WandAction wandAction, PreviewManager previewManager,
            I18nManager i18n) {
        this.wandItemManager = wandItemManager;
        this.wandAction = wandAction;
        this.previewManager = previewManager;
        this.i18n = i18n;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
        loadCooldownConfig();
        startPreviewUpdateTask();
    }

    /**
     * 从配置加载使用间隔
     */
    public void loadCooldownConfig() {
        if (plugin != null) {
            cooldownTicks = plugin.getConfig().getLong("cooldown.ticks", 10);
            cooldownMs = cooldownTicks * 50;
        }
    }

    private void startPreviewUpdateTask() {
        if (plugin == null)
            return;

        Runnable previewTask = () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (VersionHelper.isFolia()) {

                    VersionHelper.runAtEntity(plugin, player, () -> {
                        try {
                            updatePlayerPreview(player);
                        } catch (Exception e) {

                        }
                    });
                } else {
                    updatePlayerPreview(player);
                }
            }
        };

        if (VersionHelper.isFolia()) {
            VersionHelper.runTaskTimer(plugin, previewTask, 1L, 10L);
        } else {
            previewTaskId = Bukkit.getScheduler().runTaskTimer(plugin, previewTask, 1L, 10L).getTaskId();
        }
    }

    private void updatePlayerPreview(Player player) {
        try {

            if (player == null || !player.isOnline()) {
                return;
            }

            ItemStack itemInHand = player.getInventory().getItemInMainHand();

            if (!wandItemManager.isWand(itemInHand)) {

                if (!previewManager.getPreviewLocations(player).isEmpty()) {
                    previewManager.clearPreview(player);
                }
                return;
            }

            if (!player.hasPermission("constructionwand.use")) {
                previewManager.clearPreview(player);
                return;
            }

            WandType wandType = wandItemManager.getWandType(itemInHand);
            if (wandType == null) {
                return;
            }

            if (!player.hasPermission(wandType.getPermission())) {
                return;
            }

            // 首先尝试不穿透流体的射线追踪
            RayTraceResult rayTrace = null;
            Block targetBlock = null;
            BlockFace targetFace = null;
            
            try {
                // 先尝试穿透流体，找到实体方块
                rayTrace = player.rayTraceBlocks(5.0, FluidCollisionMode.ALWAYS);
                
                if (rayTrace != null && rayTrace.getHitBlock() != null) {
                    Block hitBlock = rayTrace.getHitBlock();
                    Material hitMaterial = hitBlock.getType();
                    
                    // 如果击中的是流体，继续穿透找实体方块
                    if (isFluid(hitMaterial)) {
                        // 使用 SOURCE_ONLY 模式，只检测流体源方块
                        RayTraceResult solidTrace = player.rayTraceBlocks(5.0, FluidCollisionMode.NEVER);
                        if (solidTrace != null && solidTrace.getHitBlock() != null) {
                            targetBlock = solidTrace.getHitBlock();
                            targetFace = solidTrace.getHitBlockFace();
                        }
                    } else {
                        targetBlock = hitBlock;
                        targetFace = rayTrace.getHitBlockFace();
                    }
                }
            } catch (Exception e) {
                previewManager.clearPreview(player);
                return;
            }

            if (targetBlock == null || targetFace == null) {
                previewManager.clearPreview(player);
                return;
            }

            Material targetMaterial = null;
            try {
                targetMaterial = targetBlock.getType();
            } catch (Exception e) {
                previewManager.clearPreview(player);
                return;
            }

            if (targetMaterial == null || targetMaterial.isAir() || isFluid(targetMaterial)) {
                previewManager.clearPreview(player);
                return;
            }

            List<Location> locations;
            try {
                locations = wandAction.calculatePlaceableLocations(
                        player, targetBlock, targetFace, itemInHand);
            } catch (Exception e) {

                previewManager.clearPreview(player);
                return;
            }

            if (locations == null || locations.isEmpty()) {
                previewManager.clearPreview(player);
                return;
            }

            previewManager.updatePreview(player, locations, targetMaterial);
        } catch (Exception e) {

        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (!wandItemManager.isWand(itemInHand)) {
            return;
        }

        if (!player.hasPermission("constructionwand.use")) {
            player.sendMessage(i18n.get("message.no-permission"));
            return;
        }

        WandType wandType = wandItemManager.getWandType(itemInHand);
        if (wandType == null) {
            return;
        }

        if (!player.hasPermission(wandType.getPermission())) {
            player.sendMessage(i18n.get("message.no-permission-wand-type",
                    i18n.get("wand." + wandType.getId() + ".name")));
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        BlockFace clickedFace = event.getBlockFace();

        if (clickedBlock == null) {
            return;
        }

        event.setCancelled(true);

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastUse = cooldowns.get(playerId);
        if (lastUse != null && (currentTime - lastUse) < cooldownMs) {

            return;
        }

        handlePlacement(player, clickedBlock, clickedFace, itemInHand);

        cooldowns.put(playerId, currentTime);
    }

    private void handlePlacement(Player player, Block clickedBlock, BlockFace clickedFace, ItemStack wandItem) {
        Material targetMaterial = clickedBlock.getType();

        List<Location> locations = wandAction.calculatePlaceableLocations(
                player, clickedBlock, clickedFace, wandItem);

        if (locations.isEmpty()) {
            player.sendMessage(i18n.get("message.no-place"));
            return;
        }

        int placed = wandAction.placeBlocks(player, locations, targetMaterial, wandItem);

        if (placed > 0) {
            player.sendMessage(i18n.get("message.place-success", placed));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (wandItemManager.isWand(itemInHand)) {

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        previewManager.clearPreview(player);
        cooldowns.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        previewManager.clearPreview(player);
        cooldowns.remove(player.getUniqueId());
    }

    public void stopPreviewTask() {
        if (previewTaskId != -1 && !VersionHelper.isFolia()) {
            Bukkit.getScheduler().cancelTask(previewTaskId);
            previewTaskId = -1;
        }
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
}