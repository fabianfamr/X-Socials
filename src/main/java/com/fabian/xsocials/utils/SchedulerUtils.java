package com.fabian.xsocials.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

public class SchedulerUtils {

    private static Boolean isFolia;

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

    private static Object getAsyncScheduler() throws Exception {
        Server server = Bukkit.getServer();
        return server.getClass().getMethod("getAsyncScheduler").invoke(server);
    }

    private static Object getGlobalRegionScheduler() throws Exception {
        Server server = Bukkit.getServer();
        return server.getClass().getMethod("getGlobalRegionScheduler").invoke(server);
    }

    private static Object getRegionScheduler() throws Exception {
        Server server = Bukkit.getServer();
        return server.getClass().getMethod("getRegionScheduler").invoke(server);
    }

    private static void logFoliaError(String method, Exception e) {
        Bukkit.getLogger().log(Level.WARNING, "[X-Socials] Folia scheduler error in " + method + ": " + e.getMessage());
    }

    // ========================
    // Async Scheduler
    // ========================

    public static Object runAsync(Plugin plugin, Runnable task) {
        if (isFolia()) {
            try {
                Object scheduler = getAsyncScheduler();
                return scheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class)
                    .invoke(scheduler, plugin, (Consumer<Object>) t -> task.run());
            } catch (Exception e) {
                logFoliaError("runAsync", e);
                return null;
            }
        } else {
            return Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public static Object runAsyncLater(Plugin plugin, Runnable task, long delayTicks) {
        if (isFolia()) {
            try {
                Object scheduler = getAsyncScheduler();
                return scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class, TimeUnit.class)
                    .invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), delayTicks * 50L, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                logFoliaError("runAsyncLater", e);
                return null;
            }
        } else {
            return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
        }
    }

    public static Object runAsyncTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (isFolia()) {
            try {
                Object scheduler = getAsyncScheduler();
                return scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class)
                    .invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), Math.max(1, delayTicks) * 50L, periodTicks * 50L, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                logFoliaError("runAsyncTimer", e);
                return null;
            }
        } else {
            return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        }
    }

    // ========================
    // Global Region Scheduler
    // ========================

    public static Object runSync(Plugin plugin, Runnable task) {
        if (isFolia()) {
            try {
                Object scheduler = getGlobalRegionScheduler();
                scheduler.getClass().getMethod("execute", Plugin.class, Runnable.class)
                    .invoke(scheduler, plugin, task);
                return null;
            } catch (Exception e) {
                logFoliaError("runSync", e);
                return null;
            }
        } else {
            return Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static Object runLater(Plugin plugin, Runnable task, long delayTicks) {
        if (isFolia()) {
            try {
                Object scheduler = getGlobalRegionScheduler();
                return scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class)
                    .invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), delayTicks);
            } catch (Exception e) {
                logFoliaError("runLater", e);
                return null;
            }
        } else {
            return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static Object runTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (isFolia()) {
            try {
                Object scheduler = getGlobalRegionScheduler();
                return scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class)
                    .invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), Math.max(1, delayTicks), periodTicks);
            } catch (Exception e) {
                logFoliaError("runTimer", e);
                return null;
            }
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }

    // ========================
    // Entity Scheduler
    // ========================

    public static Object runEntity(Plugin plugin, Entity entity, Runnable task) {
        if (isFolia()) {
            try {
                Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                return scheduler.getClass().getMethod("run", Plugin.class, Consumer.class, Runnable.class)
                    .invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), null);
            } catch (Exception e) {
                return runRegion(plugin, entity.getLocation(), task);
            }
        } else {
            return Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static Object runEntityLater(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
        if (isFolia()) {
            try {
                Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                return scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class)
                    .invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), null, delayTicks);
            } catch (Exception e) {
                return runRegionLater(plugin, entity.getLocation(), task, delayTicks);
            }
        } else {
            return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static Object runEntityTimer(Plugin plugin, Entity entity, Runnable task, long delayTicks, long periodTicks) {
        if (isFolia()) {
            try {
                Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                return scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, Runnable.class, long.class, long.class)
                    .invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), null, Math.max(1, delayTicks), periodTicks);
            } catch (Exception e) {
                return runRegionTimer(plugin, entity.getLocation(), task, delayTicks, periodTicks);
            }
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }

    // ========================
    // Region Scheduler
    // ========================

    public static Object runRegion(Plugin plugin, Location location, Runnable task) {
        if (isFolia()) {
            try {
                Object scheduler = getRegionScheduler();
                scheduler.getClass().getMethod("execute", Plugin.class, Location.class, Runnable.class)
                    .invoke(scheduler, plugin, location, task);
                return null;
            } catch (Exception e) {
                logFoliaError("runRegion", e);
                return null;
            }
        } else {
            return Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static Object runRegionLater(Plugin plugin, Location location, Runnable task, long delayTicks) {
        if (isFolia()) {
            try {
                Object scheduler = getRegionScheduler();
                return scheduler.getClass().getMethod("runDelayed", Plugin.class, Location.class, Consumer.class, long.class)
                    .invoke(scheduler, plugin, location, (Consumer<Object>) t -> task.run(), delayTicks);
            } catch (Exception e) {
                logFoliaError("runRegionLater", e);
                return null;
            }
        } else {
            return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static Object runRegionTimer(Plugin plugin, Location location, Runnable task, long delayTicks, long periodTicks) {
        if (isFolia()) {
            try {
                Object scheduler = getRegionScheduler();
                return scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Location.class, Consumer.class, long.class, long.class)
                    .invoke(scheduler, plugin, location, (Consumer<Object>) t -> task.run(), Math.max(1, delayTicks), periodTicks);
            } catch (Exception e) {
                logFoliaError("runRegionTimer", e);
                return null;
            }
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }

    // ========================
    // Task Cancellation
    // ========================

    public static void cancelTask(Object task) {
        if (task == null) return;
        try {
            task.getClass().getMethod("cancel").invoke(task);
        } catch (Exception e) {
            // Already cancelled or incompatible task type
        }
    }
}




