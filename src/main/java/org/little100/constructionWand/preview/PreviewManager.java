package org.little100.constructionWand.preview;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.little100.constructionWand.utils.VersionHelper;

import java.util.*;

public class PreviewManager {

    public enum PreviewMode {
        FULL,
        OUTLINE,
        CORNERS
    }

    private final Plugin plugin;
    private final Map<UUID, List<Location>> playerPreviews = new HashMap<>();
    private final Map<UUID, Integer> playerTaskIds = new HashMap<>();
    private final Map<UUID, Material> playerMaterials = new HashMap<>();
    private final Map<UUID, PreviewMode> playerPreviewModes = new HashMap<>();
    private final Map<UUID, Particle.DustOptions> playerDustOptions = new HashMap<>();

    private Particle.DustOptions dustOptions = new Particle.DustOptions(Color.RED, 0.8f);
    private PreviewMode defaultPreviewMode = PreviewMode.FULL;

    private static final int PARTICLE_DURATION_TICKS = 10;

    public PreviewManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void showPreview(Player player, List<Location> locations, Material material) {

        clearPreview(player);

        if (locations == null || locations.isEmpty()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        playerPreviews.put(playerId, new ArrayList<>(locations));
        playerMaterials.put(playerId, material);

        spawnPreviewParticles(player, locations);
    }

    public void updatePreview(Player player, List<Location> locations, Material material) {
        if (locations == null || locations.isEmpty()) {
            clearPreview(player);
            return;
        }

        UUID playerId = player.getUniqueId();
        List<Location> currentLocations = playerPreviews.get(playerId);

        boolean sameLocations = currentLocations != null &&
                currentLocations.size() == locations.size() &&
                locationsMatch(currentLocations, locations);

        if (!sameLocations) {

            playerPreviews.put(playerId, new ArrayList<>(locations));
            playerMaterials.put(playerId, material);
        }

        spawnPreviewParticles(player, locations);
    }

    private boolean locationsMatch(List<Location> list1, List<Location> list2) {
        if (list1.size() != list2.size())
            return false;
        for (int i = 0; i < list1.size(); i++) {
            Location loc1 = list1.get(i);
            Location loc2 = list2.get(i);
            if (loc1.getBlockX() != loc2.getBlockX() ||
                    loc1.getBlockY() != loc2.getBlockY() ||
                    loc1.getBlockZ() != loc2.getBlockZ()) {
                return false;
            }
        }
        return true;
    }

    public void refreshParticlePreview(Player player) {
        UUID playerId = player.getUniqueId();
        List<Location> locations = playerPreviews.get(playerId);

        if (locations == null || locations.isEmpty()) {
            return;
        }

        spawnPreviewParticles(player, locations);
    }

    private void spawnPreviewParticles(Player player, List<Location> locations) {
        PreviewMode mode = getPlayerPreviewMode(player);

        switch (mode) {
            case CORNERS:

                spawnAllCornerParticles(player, locations);
                break;
            case OUTLINE:

                spawnBoundingBoxParticles(player, locations);
                break;
            case FULL:
            default:

                spawnAllFullOutlineParticles(player, locations);
                break;
        }
    }

    private void spawnAllFullOutlineParticles(Player player, List<Location> locations) {
        if (locations == null || locations.isEmpty())
            return;

        World world = locations.get(0).getWorld();
        if (world == null)
            return;

        List<Location> allParticles = new ArrayList<>();
        double step = 0.5;

        for (Location loc : locations) {
            double x = loc.getBlockX();
            double y = loc.getBlockY();
            double z = loc.getBlockZ();

            for (double i = 0; i <= 1; i += step) {
                allParticles.add(new Location(world, x + i, y, z));
                allParticles.add(new Location(world, x + i, y, z + 1));
                allParticles.add(new Location(world, x, y, z + i));
                allParticles.add(new Location(world, x + 1, y, z + i));
            }

            for (double i = 0; i <= 1; i += step) {
                allParticles.add(new Location(world, x + i, y + 1, z));
                allParticles.add(new Location(world, x + i, y + 1, z + 1));
                allParticles.add(new Location(world, x, y + 1, z + i));
                allParticles.add(new Location(world, x + 1, y + 1, z + i));
            }

            for (double i = 0; i <= 1; i += step) {
                allParticles.add(new Location(world, x, y + i, z));
                allParticles.add(new Location(world, x + 1, y + i, z));
                allParticles.add(new Location(world, x, y + i, z + 1));
                allParticles.add(new Location(world, x + 1, y + i, z + 1));
            }
        }

        spawnParticlesBatch(player, allParticles);
    }

    private void spawnAllCornerParticles(Player player, List<Location> locations) {
        if (locations == null || locations.isEmpty())
            return;

        World world = locations.get(0).getWorld();
        if (world == null)
            return;

        List<Location> allParticles = new ArrayList<>();

        for (Location loc : locations) {
            double x = loc.getBlockX();
            double y = loc.getBlockY();
            double z = loc.getBlockZ();

            allParticles.add(new Location(world, x, y, z));
            allParticles.add(new Location(world, x + 1, y, z));
            allParticles.add(new Location(world, x, y, z + 1));
            allParticles.add(new Location(world, x + 1, y, z + 1));
            allParticles.add(new Location(world, x, y + 1, z));
            allParticles.add(new Location(world, x + 1, y + 1, z));
            allParticles.add(new Location(world, x, y + 1, z + 1));
            allParticles.add(new Location(world, x + 1, y + 1, z + 1));
        }

        spawnParticlesBatch(player, allParticles);
    }

    private void spawnBoundingBoxParticles(Player player, List<Location> locations) {
        if (locations == null || locations.isEmpty())
            return;

        World world = locations.get(0).getWorld();
        if (world == null)
            return;

        Set<String> blockSet = new HashSet<>();
        for (Location loc : locations) {
            blockSet.add(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        }

        Set<String> edgeSet = new HashSet<>();
        List<Location> particleLocations = new ArrayList<>();
        double step = 0.5;

        for (Location loc : locations) {
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();

            boolean hasNegX = blockSet.contains((x - 1) + "," + y + "," + z);
            boolean hasPosX = blockSet.contains((x + 1) + "," + y + "," + z);
            boolean hasNegY = blockSet.contains(x + "," + (y - 1) + "," + z);
            boolean hasPosY = blockSet.contains(x + "," + (y + 1) + "," + z);
            boolean hasNegZ = blockSet.contains(x + "," + y + "," + (z - 1));
            boolean hasPosZ = blockSet.contains(x + "," + y + "," + (z + 1));

            int edge_x0y0z = (!hasNegX ? 1 : 0) + (!hasNegY ? 1 : 0);
            if (edge_x0y0z >= 2) {
                addEdgeParticles(edgeSet, particleLocations, world, x, y, z, x, y, z + 1, step);
            }

            int edge_x1y0z = (!hasPosX ? 1 : 0) + (!hasNegY ? 1 : 0);
            if (edge_x1y0z >= 2) {
                addEdgeParticles(edgeSet, particleLocations, world, x + 1, y, z, x + 1, y, z + 1, step);
            }

            int edge_y0z0x = (!hasNegY ? 1 : 0) + (!hasNegZ ? 1 : 0);
            if (edge_y0z0x >= 2) {
                addEdgeParticles(edgeSet, particleLocations, world, x, y, z, x + 1, y, z, step);
            }

            int edge_y0z1x = (!hasNegY ? 1 : 0) + (!hasPosZ ? 1 : 0);
            if (edge_y0z1x >= 2) {
                addEdgeParticles(edgeSet, particleLocations, world, x, y, z + 1, x + 1, y, z + 1, step);
            }

            int edge_x0y1z = (!hasNegX ? 1 : 0) + (!hasPosY ? 1 : 0);
            if (edge_x0y1z >= 2) {
                addEdgeParticles(edgeSet, particleLocations, world, x, y + 1, z, x, y + 1, z + 1, step);
            }

            int edge_x1y1z = (!hasPosX ? 1 : 0) + (!hasPosY ? 1 : 0);
            if (edge_x1y1z >= 2) {
                addEdgeParticles(edgeSet, particleLocations, world, x + 1, y + 1, z, x + 1, y + 1, z + 1, step);
            }

            int edge_y1z0x = (!hasPosY ? 1 : 0) + (!hasNegZ ? 1 : 0);
            if (edge_y1z0x >= 2) {
                addEdgeParticles(edgeSet, particleLocations, world, x, y + 1, z, x + 1, y + 1, z, step);
            }

            int edge_y1z1x = (!hasPosY ? 1 : 0) + (!hasPosZ ? 1 : 0);
            if (edge_y1z1x >= 2) {
                addEdgeParticles(edgeSet, particleLocations, world, x, y + 1, z + 1, x + 1, y + 1, z + 1, step);
            }

            int edge_x0z0y = (!hasNegX ? 1 : 0) + (!hasNegZ ? 1 : 0);
            if (edge_x0z0y >= 2) {
                addEdgeParticles(edgeSet, particleLocations, world, x, y, z, x, y + 1, z, step);
            }

            int edge_x1z0y = (!hasPosX ? 1 : 0) + (!hasNegZ ? 1 : 0);
            if (edge_x1z0y >= 2) {
                addEdgeParticles(edgeSet, particleLocations, world, x + 1, y, z, x + 1, y + 1, z, step);
            }

            int edge_x0z1y = (!hasNegX ? 1 : 0) + (!hasPosZ ? 1 : 0);
            if (edge_x0z1y >= 2) {
                addEdgeParticles(edgeSet, particleLocations, world, x, y, z + 1, x, y + 1, z + 1, step);
            }

            int edge_x1z1y = (!hasPosX ? 1 : 0) + (!hasPosZ ? 1 : 0);
            if (edge_x1z1y >= 2) {
                addEdgeParticles(edgeSet, particleLocations, world, x + 1, y, z + 1, x + 1, y + 1, z + 1, step);
            }
        }

        spawnParticlesBatch(player, particleLocations);
    }

    private void addEdgeParticles(Set<String> edgeSet, List<Location> particles, World world,
            double x1, double y1, double z1, double x2, double y2, double z2, double step) {

        String edgeKey;
        if (x1 < x2 || (x1 == x2 && y1 < y2) || (x1 == x2 && y1 == y2 && z1 < z2)) {
            edgeKey = x1 + "," + y1 + "," + z1 + "-" + x2 + "," + y2 + "," + z2;
        } else {
            edgeKey = x2 + "," + y2 + "," + z2 + "-" + x1 + "," + y1 + "," + z1;
        }

        if (edgeSet.contains(edgeKey)) {
            return;
        }
        edgeSet.add(edgeKey);

        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (length == 0)
            return;

        int numParticles = (int) Math.ceil(length / step) + 1;
        for (int i = 0; i <= numParticles; i++) {
            double t = (double) i / numParticles;
            double px = x1 + dx * t;
            double py = y1 + dy * t;
            double pz = z1 + dz * t;
            particles.add(new Location(world, px, py, pz));
        }
    }

    private void spawnFullOutlineParticles(Player player, Location loc) {
        World world = loc.getWorld();
        if (world == null)
            return;

        double x = loc.getBlockX();
        double y = loc.getBlockY();
        double z = loc.getBlockZ();

        List<Location> particleLocations = new ArrayList<>();

        double step = 0.5;

        for (double i = 0; i <= 1; i += step) {
            particleLocations.add(new Location(world, x + i, y, z));
            particleLocations.add(new Location(world, x + i, y, z + 1));
            particleLocations.add(new Location(world, x, y, z + i));
            particleLocations.add(new Location(world, x + 1, y, z + i));
        }

        for (double i = 0; i <= 1; i += step) {
            particleLocations.add(new Location(world, x + i, y + 1, z));
            particleLocations.add(new Location(world, x + i, y + 1, z + 1));
            particleLocations.add(new Location(world, x, y + 1, z + i));
            particleLocations.add(new Location(world, x + 1, y + 1, z + i));
        }

        for (double i = 0; i <= 1; i += step) {
            particleLocations.add(new Location(world, x, y + i, z));
            particleLocations.add(new Location(world, x + 1, y + i, z));
            particleLocations.add(new Location(world, x, y + i, z + 1));
            particleLocations.add(new Location(world, x + 1, y + i, z + 1));
        }

        spawnParticlesBatch(player, particleLocations);
    }

    private void spawnCornerParticles(Player player, Location loc) {
        World world = loc.getWorld();
        if (world == null)
            return;

        double x = loc.getBlockX();
        double y = loc.getBlockY();
        double z = loc.getBlockZ();

        List<Location> particleLocations = new ArrayList<>();

        particleLocations.add(new Location(world, x, y, z));
        particleLocations.add(new Location(world, x + 1, y, z));
        particleLocations.add(new Location(world, x, y, z + 1));
        particleLocations.add(new Location(world, x + 1, y, z + 1));
        particleLocations.add(new Location(world, x, y + 1, z));
        particleLocations.add(new Location(world, x + 1, y + 1, z));
        particleLocations.add(new Location(world, x, y + 1, z + 1));
        particleLocations.add(new Location(world, x + 1, y + 1, z + 1));

        spawnParticlesBatch(player, particleLocations);
    }

    private void spawnParticlesBatch(Player player, List<Location> locations) {
        if (locations == null || locations.isEmpty())
            return;

        Particle.DustOptions playerOptions = getPlayerDustOptions(player);

        try {

            for (Location loc : locations) {
                player.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, playerOptions);
            }
        } catch (Exception e) {

            try {
                for (Location loc : locations) {
                    player.spawnParticle(Particle.valueOf("REDSTONE"), loc, 1, 0, 0, 0, 0, dustOptions);
                }
            } catch (Exception e2) {

                try {
                    for (Location loc : locations) {
                        player.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
                    }
                } catch (Exception e3) {

                }
            }
        }
    }

    private void spawnParticle(Player player, World world, double x, double y, double z) {
        Location particleLoc = new Location(world, x, y, z);
        try {

            player.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dustOptions);
        } catch (Exception e) {

            try {
                player.spawnParticle(Particle.valueOf("REDSTONE"), particleLoc, 1, 0, 0, 0, 0, dustOptions);
            } catch (Exception e2) {

                try {
                    player.spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
                } catch (Exception e3) {

                }
            }
        }
    }

    public void clearPreview(Player player) {
        UUID playerId = player.getUniqueId();

        playerPreviews.remove(playerId);
        playerMaterials.remove(playerId);

        Integer taskId = playerTaskIds.remove(playerId);
        if (taskId != null && !VersionHelper.isFolia()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    public void clearAllPreviews() {
        for (UUID playerId : new HashSet<>(playerPreviews.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                clearPreview(player);
            }
        }
        playerPreviews.clear();
        playerTaskIds.clear();
    }

    public List<Location> getPreviewLocations(Player player) {
        return playerPreviews.getOrDefault(player.getUniqueId(), Collections.emptyList());
    }

    public void setPreviewColor(Color color) {
        this.dustOptions = new Particle.DustOptions(color, 0.8f);
    }

    public void setPreviewMode(PreviewMode mode) {
        this.defaultPreviewMode = mode;
    }

    public PreviewMode getPreviewMode() {
        return defaultPreviewMode;
    }

    public void setPlayerPreviewMode(Player player, PreviewMode mode) {
        playerPreviewModes.put(player.getUniqueId(), mode);
    }

    public PreviewMode getPlayerPreviewMode(Player player) {
        return playerPreviewModes.getOrDefault(player.getUniqueId(), defaultPreviewMode);
    }

    public PreviewMode togglePlayerPreviewMode(Player player) {
        PreviewMode current = getPlayerPreviewMode(player);
        PreviewMode next;
        switch (current) {
            case FULL:
                next = PreviewMode.CORNERS;
                break;
            case CORNERS:
                next = PreviewMode.OUTLINE;
                break;
            case OUTLINE:
            default:
                next = PreviewMode.FULL;
                break;
        }
        setPlayerPreviewMode(player, next);
        return next;
    }

    public void setPlayerPreviewColor(Player player, Color color) {
        playerDustOptions.put(player.getUniqueId(), new Particle.DustOptions(color, 0.8f));
    }

    public Particle.DustOptions getPlayerDustOptions(Player player) {
        return playerDustOptions.getOrDefault(player.getUniqueId(), dustOptions);
    }

    public Color getPlayerPreviewColor(Player player) {
        Particle.DustOptions options = playerDustOptions.get(player.getUniqueId());
        return options != null ? options.getColor() : dustOptions.getColor();
    }
}