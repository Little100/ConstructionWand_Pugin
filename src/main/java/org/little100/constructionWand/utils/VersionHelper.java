package org.little100.constructionWand.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;

public class VersionHelper {

    private static Boolean isFolia = null;
    private static int[] serverVersion = null;
    private static Boolean supportsNewCustomModelData = null;

    public static boolean isFolia() {
        if (isFolia == null) {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                isFolia = true;
            } catch (ClassNotFoundException e) {
                isFolia = false;
            }
        }
        return isFolia;
    }

    public static int[] getServerVersion() {
        if (serverVersion == null) {
            String version = Bukkit.getBukkitVersion();

            String[] parts = version.split("-")[0].split("\\.");
            serverVersion = new int[3];
            for (int i = 0; i < Math.min(parts.length, 3); i++) {
                try {
                    serverVersion[i] = Integer.parseInt(parts[i]);
                } catch (NumberFormatException e) {
                    serverVersion[i] = 0;
                }
            }
        }
        return serverVersion;
    }

    public static boolean isVersionAtLeast(int major, int minor, int patch) {
        int[] version = getServerVersion();
        if (version[0] > major)
            return true;
        if (version[0] < major)
            return false;
        if (version[1] > minor)
            return true;
        if (version[1] < minor)
            return false;
        return version[2] >= patch;
    }

    public static boolean supportsNewCustomModelData() {
        if (supportsNewCustomModelData == null) {

            supportsNewCustomModelData = isVersionAtLeast(1, 20, 5);
        }
        return supportsNewCustomModelData;
    }

    private static Boolean supportsNewCustomModelDataApi = null;

    public static boolean supportsNewCustomModelDataComponent() {
        if (supportsNewCustomModelDataApi == null) {
            try {
                Class.forName("org.bukkit.inventory.meta.components.CustomModelDataComponent");
                supportsNewCustomModelDataApi = true;
            } catch (ClassNotFoundException e) {
                supportsNewCustomModelDataApi = false;
            }
        }
        return supportsNewCustomModelDataApi;
    }

    public static void setCustomModelData(ItemMeta meta, int value) {

        try {
            meta.setCustomModelData(value);
        } catch (Exception e) {

        }
    }

    public static void setCustomModelDataComponent(ItemStack item, int value) {
        if (item == null)
            return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        if (value > 0) {
            try {
                meta.setCustomModelData(value);
            } catch (Exception e) {

            }
        }

        if (supportsNewCustomModelDataComponent()) {
            try {
                setNewCustomModelDataStrings(meta, String.valueOf(value));
            } catch (Throwable e) {

            }
        }

        item.setItemMeta(meta);
    }

    private static void setNewCustomModelDataStrings(ItemMeta meta, String stringValue) {
        try {

            Class<?> itemMetaInterface = Class.forName("org.bukkit.inventory.meta.ItemMeta");
            Method getComponentMethod = itemMetaInterface.getMethod("getCustomModelDataComponent");
            getComponentMethod.setAccessible(true);

            Object component = getComponentMethod.invoke(meta);
            if (component == null) {
                return;
            }

            Class<?> componentInterface = Class
                    .forName("org.bukkit.inventory.meta.components.CustomModelDataComponent");
            Method setStringsMethod = componentInterface.getMethod("setStrings", java.util.List.class);
            setStringsMethod.setAccessible(true);

            setStringsMethod.invoke(component, java.util.Collections.singletonList(stringValue));

            Method setComponentMethod = itemMetaInterface.getMethod("setCustomModelDataComponent", componentInterface);
            setComponentMethod.setAccessible(true);

            setComponentMethod.invoke(meta, component);

        } catch (Exception e) {

        }
    }

    public static int getCustomModelData(ItemMeta meta) {

        try {
            if (meta.hasCustomModelData()) {
                return meta.getCustomModelData();
            }
        } catch (Exception e) {

        }
        return 0;
    }

    public static int getCustomModelDataFromItem(ItemStack item) {
        if (item == null)
            return 0;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return 0;

        try {
            if (meta.hasCustomModelData()) {
                return meta.getCustomModelData();
            }
        } catch (IllegalStateException e) {

        } catch (Exception e) {

        }

        if (supportsNewCustomModelDataComponent()) {
            try {
                List<String> strings = getNewCustomModelDataStringsFromMeta(meta);
                if (!strings.isEmpty()) {
                    String firstString = strings.get(0);
                    try {
                        return Integer.parseInt(firstString);
                    } catch (NumberFormatException e) {

                    }
                }
            } catch (Exception e) {

            }
        }

        return 0;
    }

    public static List<String> getCustomModelDataStrings(ItemStack item) {
        if (item == null)
            return List.of();

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return List.of();

        if (supportsNewCustomModelDataComponent()) {
            try {
                List<String> strings = getNewCustomModelDataStringsFromMeta(meta);
                if (!strings.isEmpty()) {
                    return strings;
                }
            } catch (Exception e) {

            }
        }

        try {
            if (meta.hasCustomModelData()) {
                return List.of(String.valueOf(meta.getCustomModelData()));
            }
        } catch (Exception e) {

        }

        return List.of();
    }

    private static List<String> getNewCustomModelDataStringsFromMeta(ItemMeta meta) {
        try {

            Method getComponentMethod = meta.getClass().getMethod("getCustomModelDataComponent");
            Object component = getComponentMethod.invoke(meta);

            if (component == null) {
                return List.of();
            }

            Method getStringsMethod = component.getClass().getMethod("getStrings");
            @SuppressWarnings("unchecked")
            List<String> strings = (List<String>) getStringsMethod.invoke(component);

            return strings != null ? strings : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    public static void runTask(Plugin plugin, Runnable task) {
        if (isFolia()) {

            try {
                Object globalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method runMethod = globalRegionScheduler.getClass().getMethod("run", Plugin.class,
                        java.util.function.Consumer.class);
                runMethod.invoke(globalRegionScheduler, plugin,
                        (java.util.function.Consumer<Object>) (task1) -> task.run());
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to schedule task on Folia", e);
            }
        } else {

            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void runTaskLater(Plugin plugin, Runnable task, long delayTicks) {
        if (isFolia()) {
            try {
                Object globalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method runDelayedMethod = globalRegionScheduler.getClass().getMethod("runDelayed",
                        Plugin.class, java.util.function.Consumer.class, long.class);
                runDelayedMethod.invoke(globalRegionScheduler, plugin,
                        (java.util.function.Consumer<Object>) (task1) -> task.run(), delayTicks);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to schedule delayed task on Folia", e);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static void runAtLocation(Plugin plugin, Location location, Runnable task) {
        if (isFolia()) {
            try {
                Object regionScheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
                Method runMethod = regionScheduler.getClass().getMethod("run",
                        Plugin.class, Location.class, java.util.function.Consumer.class);
                runMethod.invoke(regionScheduler, plugin, location,
                        (java.util.function.Consumer<Object>) (task1) -> task.run());
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to schedule location task on Folia", e);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void runAtEntity(Plugin plugin, Entity entity, Runnable task) {
        if (isFolia()) {
            try {
                Method getSchedulerMethod = Entity.class.getMethod("getScheduler");
                Object entityScheduler = getSchedulerMethod.invoke(entity);
                Method runMethod = entityScheduler.getClass().getMethod("run",
                        Plugin.class, java.util.function.Consumer.class, Runnable.class);
                runMethod.invoke(entityScheduler, plugin,
                        (java.util.function.Consumer<Object>) (task1) -> task.run(), null);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to schedule entity task on Folia", e);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void runTaskTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (isFolia()) {
            try {
                Object globalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method runAtFixedRateMethod = globalRegionScheduler.getClass().getMethod("runAtFixedRate",
                        Plugin.class, java.util.function.Consumer.class, long.class, long.class);
                runAtFixedRateMethod.invoke(globalRegionScheduler, plugin,
                        (java.util.function.Consumer<Object>) (task1) -> task.run(),
                        Math.max(1, delayTicks), periodTicks);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to schedule timer task on Folia", e);
            }
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }

    public static String getVersionInfo() {
        int[] version = getServerVersion();
        return String.format("Server: %d.%d.%d, Folia: %s, NewCustomModelData: %s",
                version[0], version[1], version[2],
                isFolia() ? "Yes" : "No",
                supportsNewCustomModelData() ? "Yes" : "No");
    }
}