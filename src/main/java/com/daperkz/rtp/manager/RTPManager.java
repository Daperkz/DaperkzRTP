/*
* ==============================================================================
* DaperkzRTP - Minecraft Plugin
* Copyright (c) 2026 Daperkz
*
* RTPManager
* ==============================================================================
*/
package com.daperkz.rtp.manager;

import com.daperkz.rtp.RTPPlugin;
import com.daperkz.rtp.config.ConfigManager;
import com.daperkz.rtp.config.LanguageManager;
import com.daperkz.rtp.util.TaskScheduler;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class RTPManager {

    private final RTPPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private static final Set<Material> DANGEROUS_BLOCKS = EnumSet.of(
            Material.LAVA, Material.WATER, Material.CACTUS, Material.BEDROCK,
            Material.FIRE, Material.SOUL_FIRE, Material.MAGMA_BLOCK, Material.VOID_AIR, Material.AIR
    );

    private static final Set<Material> NETHER_VALID_FLOORS = EnumSet.of(
            Material.NETHERRACK, Material.SOUL_SAND, Material.SOUL_SOIL,
            Material.BASALT, Material.BLACKSTONE, Material.CRIMSON_NYLIUM, Material.WARPED_NYLIUM
    );

    public RTPManager(RTPPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean cancelRTP(Player player) {
        CooldownManager cd = plugin.getCooldownManager();
        if (cd.isTeleporting(player.getUniqueId())) {
            cd.setTeleporting(player.getUniqueId(), false);
            return true;
        }
        return false;
    }

    public void processRTP(Player player, ConfigManager.WorldBounds bounds) {
        ConfigManager cfg = plugin.getConfigManager();
        LanguageManager lang = plugin.getLanguageManager();
        CooldownManager cd = plugin.getCooldownManager();

        if (lang == null || cfg == null) {
            plugin.getLogger().severe("Configuration or Language manager not initialized!");
            return;
        }

        if (!bounds.enabled()) {
            player.sendMessage(lang.getPrefixedMessage("world-disabled"));
            return;
        }

        if (cd.isOnCooldown(player.getUniqueId(), cfg.getCooldownSeconds())) {
            long remaining = cd.getRemainingCooldown(player.getUniqueId(), cfg.getCooldownSeconds());
            player.sendMessage(lang.getPrefixedMessage("cooldown", "<time>", String.valueOf(remaining)));
            return;
        }

        if (cd.isTeleporting(player.getUniqueId())) {
            player.sendMessage(lang.getPrefixedMessage("already-teleporting"));
            return;
        }

        World world = Bukkit.getWorld(bounds.worldName());
        if (world == null) {
            player.sendMessage(lang.getPrefixedMessage("world-not-found"));
            return;
        }

        cd.setTeleporting(player.getUniqueId(), true);

        lang.sendNotification(player, "start", "start-warmup", null, "<seconds>", String.valueOf(cfg.getCountdownSeconds()));

        findSafeLocation(world, bounds, cfg.getMaxAttempts(), 0, player);
    }

    private void findSafeLocation(World world, ConfigManager.WorldBounds bounds, int maxAttempts, int currentAttempt, Player player) {
        if (!player.isOnline() || !plugin.getCooldownManager().isTeleporting(player.getUniqueId())) {
            plugin.getCooldownManager().setTeleporting(player.getUniqueId(), false);
            return;
        }

        if (currentAttempt >= maxAttempts) {
            plugin.getLanguageManager().sendNotification(player, "fail", "failed-find-location", null, "<attempts>", String.valueOf(maxAttempts));
            plugin.getCooldownManager().setTeleporting(player.getUniqueId(), false);
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int x = random.nextInt(bounds.maxX() - bounds.minX() + 1) + bounds.minX();
        int z = random.nextInt(bounds.maxZ() - bounds.minZ() + 1) + bounds.minZ();
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        world.addPluginChunkTicket(chunkX, chunkZ, plugin);

        world.getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk -> {
            TaskScheduler.runAsync(plugin, () -> {
                Location safeLoc = null;
                try {
                    ChunkSnapshot snapshot = chunk.getChunkSnapshot(true, false, false);
                    safeLoc = scanSnapshotForLocation(world, snapshot, x, z);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Error scanning chunk at [" + chunkX + ", " + chunkZ + "]: " + ex.getMessage());
                } finally {
                    world.removePluginChunkTicket(chunkX, chunkZ, plugin);
                }

                Location finalSafeLoc = safeLoc;
                TaskScheduler.runEntityTaskLater(plugin, player, () -> {
                    if (!player.isOnline() || !plugin.getCooldownManager().isTeleporting(player.getUniqueId())) {
                        plugin.getCooldownManager().setTeleporting(player.getUniqueId(), false);
                        return;
                    }

                    if (finalSafeLoc != null) {
                        startWarmup(player, finalSafeLoc);
                    } else {
                        findSafeLocation(world, bounds, maxAttempts, currentAttempt + 1, player);
                    }
                }, 0L);
            });
        }).exceptionally(ex -> {
            world.removePluginChunkTicket(chunkX, chunkZ, plugin);
            TaskScheduler.runEntityTaskLater(plugin, player, () -> {
                if (player.isOnline()) {
                    findSafeLocation(world, bounds, maxAttempts, currentAttempt + 1, player);
                } else {
                    plugin.getCooldownManager().setTeleporting(player.getUniqueId(), false);
                }
            }, 0L);
            return null;
        });
    }

    private Location scanSnapshotForLocation(World world, ChunkSnapshot snapshot, int x, int z) {
        return switch (world.getEnvironment()) {
            case NETHER -> scanNether(world, snapshot, x, z);
            case THE_END -> scanEnd(world, snapshot, x, z);
            default -> scanOverworld(world, snapshot, x, z);
        };
    }

    private Location scanOverworld(World world, ChunkSnapshot snapshot, int x, int z) {
        int relX = x & 15;
        int relZ = z & 15;
        int maxHeight = Math.min(world.getMaxHeight() - 1, 319);
        int minHeight = Math.max(world.getMinHeight(), -64);

        // Scan top-down safely without main-thread world getter calls
        for (int y = maxHeight; y >= minHeight; y--) {
            Material type = snapshot.getBlockType(relX, y, relZ);
            if (type != Material.AIR && type != Material.CAVE_AIR && type != Material.VOID_AIR) {
                if (!DANGEROUS_BLOCKS.contains(type)) {
                    return new Location(world, x + 0.5, y + 1, z + 0.5);
                }
                break;
            }
        }
        return null;
    }

    private Location scanNether(World world, ChunkSnapshot snapshot, int x, int z) {
        int relX = x & 15;
        int relZ = z & 15;
        for (int y = 115; y >= 35; y--) {
            Material floorMat = snapshot.getBlockType(relX, y, relZ);
            if (NETHER_VALID_FLOORS.contains(floorMat)) {
                Material feetMat = snapshot.getBlockType(relX, y + 1, relZ);
                Material headMat = snapshot.getBlockType(relX, y + 2, relZ);

                if (feetMat.isAir() && headMat.isAir()) {
                    return new Location(world, x + 0.5, y + 1, z + 0.5);
                }
            }
        }
        return null;
    }

    private Location scanEnd(World world, ChunkSnapshot snapshot, int x, int z) {
        int relX = x & 15;
        int relZ = z & 15;
        int highestY = snapshot.getHighestBlockYAt(relX, relZ);
        Material targetBlock = snapshot.getBlockType(relX, highestY, relZ);

        if (targetBlock == Material.END_STONE) {
            return new Location(world, x + 0.5, highestY + 1, z + 0.5);
        }
        return null;
    }

    private void startWarmup(Player player, Location targetLoc) {
        ConfigManager cfg = plugin.getConfigManager();
        LanguageManager lang = plugin.getLanguageManager();
        CooldownManager cd = plugin.getCooldownManager();
        Location initialLoc = player.getLocation().clone();

        ConfigManager.SoundConfig startSound = cfg.getSoundConfig("start");
        ConfigManager.SoundConfig countSound = cfg.getSoundConfig("countdown");
        ConfigManager.SoundConfig cancelSound = cfg.getSoundConfig("cancel-moved");
        ConfigManager.SoundConfig teleportSound = cfg.getSoundConfig("teleport-success");

        if (startSound.enabled()) {
            player.playSound(player.getLocation(), startSound.sound(), startSound.volume(), startSound.pitch());
        }

        final int initialSeconds = cfg.getCountdownSeconds();
        runCountdownTick(player, targetLoc, initialLoc, initialSeconds, initialSeconds, countSound, cancelSound, teleportSound);
    }

    private void runCountdownTick(Player player, Location targetLoc, Location initialLoc, int countdown, int initialSeconds,
                                  ConfigManager.SoundConfig countSound, ConfigManager.SoundConfig cancelSound, ConfigManager.SoundConfig teleportSound) {
        CooldownManager cd = plugin.getCooldownManager();
        LanguageManager lang = plugin.getLanguageManager();
        ConfigManager cfg = plugin.getConfigManager();

        if (!player.isOnline() || !cd.isTeleporting(player.getUniqueId())) {
            cd.setTeleporting(player.getUniqueId(), false);
            return;
        }

        if (!player.getWorld().equals(initialLoc.getWorld()) || player.getLocation().distanceSquared(initialLoc) > Math.pow(cfg.getMoveCancelDistance(), 2)) {
            lang.sendNotification(player, "cancel", "cancel-moved", "cancel-actionbar");
            if (cancelSound.enabled()) {
                player.playSound(player.getLocation(), cancelSound.sound(), cancelSound.volume(), cancelSound.pitch());
            }
            cd.setTeleporting(player.getUniqueId(), false);
            return;
        }

        if (countdown <= 0) {
            if (targetLoc.getWorld() != null) {
                targetLoc.getWorld().getChunkAtAsync(targetLoc).thenAccept(c -> {
                    TaskScheduler.runEntityTaskLater(plugin, player, () -> {
                        player.teleportAsync(targetLoc).thenAccept(success -> {
                            try {
                                if (success) {
                                    lang.sendNotification(player, "success", "success-message", "success-actionbar");
                                    if (teleportSound.enabled()) {
                                        player.playSound(player.getLocation(), teleportSound.sound(), teleportSound.volume(), teleportSound.pitch());
                                    }
                                    cd.setCooldown(player.getUniqueId());
                                } else {
                                    player.sendMessage(lang.getPrefixedMessage("failed-find-location", "<attempts>", "1"));
                                }
                            } finally {
                                cd.setTeleporting(player.getUniqueId(), false);
                            }
                        });
                    }, 0L);
                }).exceptionally(ex -> {
                    cd.setTeleporting(player.getUniqueId(), false);
                    return null;
                });
            } else {
                cd.setTeleporting(player.getUniqueId(), false);
            }
            return;
        }

        lang.sendNotification(player, "warmup", null, "warmup-actionbar", "<seconds>", String.valueOf(countdown));

        if (countSound.enabled()) {
            float currentPitch = countSound.pitch();
            if (countSound.pitchIncrease()) {
                float elapsedRatio = (float) (initialSeconds - countdown) / Math.max(1, initialSeconds);
                currentPitch += (elapsedRatio * 0.5f);
            }
            player.playSound(player.getLocation(), countSound.sound(), countSound.volume(), currentPitch);
        }

        TaskScheduler.runEntityTaskLater(plugin, player, () ->
                runCountdownTick(player, targetLoc, initialLoc, countdown - 1, initialSeconds, countSound, cancelSound, teleportSound), 20L);
    }
}
