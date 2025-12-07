package org.little100.constructionWand.action;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.little100.constructionWand.enchant.EnchantmentManager;
import org.little100.constructionWand.protection.ProtectionChecker;
import org.little100.constructionWand.wand.WandItemManager;
import org.little100.constructionWand.wand.WandType;

import java.util.*;

public class WandAction {

    private final WandItemManager wandItemManager;
    private final ProtectionChecker protectionChecker;
    private EnchantmentManager enchantmentManager;

    public WandAction(WandItemManager wandItemManager, ProtectionChecker protectionChecker) {
        this.wandItemManager = wandItemManager;
        this.protectionChecker = protectionChecker;
    }

    public void setEnchantmentManager(EnchantmentManager enchantmentManager) {
        this.enchantmentManager = enchantmentManager;
    }

    public List<Location> calculatePlaceableLocations(Player player, Block clickedBlock,
            BlockFace clickedFace, ItemStack wandItem) {
        List<Location> locations = new ArrayList<>();

        WandType wandType = wandItemManager.getWandType(wandItem);
        if (wandType == null)
            return locations;

        Material targetMaterial = clickedBlock.getType();
        if (!isPlaceableMaterial(targetMaterial)) {
            return locations;
        }

        int availableBlocks = countMaterialInInventory(player, targetMaterial);
        if (availableBlocks <= 0) {
            return locations;
        }

        int baseMaxBlocks = wandType.getMaxBlocks();
        int maxBlocks;
        if (enchantmentManager != null) {
            maxBlocks = enchantmentManager.calculateBonusBlocks(wandItem, baseMaxBlocks);
        } else {
            maxBlocks = baseMaxBlocks;
        }
        maxBlocks = Math.min(maxBlocks, availableBlocks);

        Location startLocation = clickedBlock.getRelative(clickedFace).getLocation();

        if (startLocation.getBlock().getType() != Material.AIR) {
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
            if (startType == Material.AIR) {
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
                        if (neighborType == Material.AIR) {
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
            if (blockType != Material.AIR) {
                return false;
            }

            if (!protectionChecker.canPlace(player, location, material)) {
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

        if (block.getType() != Material.AIR) {
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

        int placed = 0;

        for (Location loc : locations) {
            if (loc.getBlock().getType() != Material.AIR) {
                continue;
            }

            if (!hasEnoughMaterial(player, material, 1)) {
                break;
            }

            if (!protectionChecker.canPlace(player, loc, material)) {
                continue;
            }

            loc.getBlock().setType(material);

            removeMaterialFromInventory(player, material, 1);

            placed++;
        }

        if (placed > 0 && !wandType.isUnbreakable()) {
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

    public int countMaterialInInventory(Player player, Material material) {
        int count = 0;
        PlayerInventory inventory = player.getInventory();

        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }

        return count;
    }

    private boolean hasEnoughMaterial(Player player, Material material, int amount) {
        return countMaterialInInventory(player, material) >= amount;
    }

    private void removeMaterialFromInventory(Player player, Material material, int amount) {
        PlayerInventory inventory = player.getInventory();
        int remaining = amount;

        for (int i = 0; i < inventory.getSize() && remaining > 0; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() == material) {
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
}