package org.little100.constructionWand.protection;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardHook {

    private Object worldGuard;
    private boolean available = false;

    public WorldGuardHook() {
        try {

            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            worldGuard = worldGuardClass.getMethod("getInstance").invoke(null);
            available = true;
        } catch (Exception e) {
            available = false;
        }
    }

    public boolean canBuild(Player player, Location location) {
        if (!available || worldGuard == null) {
            return true;
        }

        try {

            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object platform = worldGuardClass.getMethod("getPlatform").invoke(worldGuard);
            Object regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);

            Object query = regionContainer.getClass().getMethod("createQuery").invoke(regionContainer);

            Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object weLocation = bukkitAdapterClass.getMethod("adapt", Location.class).invoke(null, location);

            Object localPlayer = bukkitAdapterClass.getMethod("adapt", Player.class).invoke(null, player);

            Class<?> flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags");
            Object buildFlag = flagsClass.getField("BUILD").get(null);

            Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
            java.lang.reflect.Method testStateMethod = query.getClass().getMethod(
                    "testState",
                    Class.forName("com.sk89q.worldedit.util.Location"),
                    Class.forName("com.sk89q.worldguard.protection.association.RegionAssociable"),
                    java.lang.reflect.Array.newInstance(stateFlagClass, 0).getClass());

            Object flagArray = java.lang.reflect.Array.newInstance(stateFlagClass, 1);
            java.lang.reflect.Array.set(flagArray, 0, buildFlag);

            Object result = testStateMethod.invoke(query, weLocation, localPlayer, flagArray);
            return (Boolean) result;

        } catch (Exception e) {

            return true;
        }
    }

    public boolean isAvailable() {
        return available;
    }
}