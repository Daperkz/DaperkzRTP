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
        player.sendMessage(lang.getPrefixedMessage("start-warmup", "<seconds>", String.valueOf(cfg.getCountdownSeconds())));

        findSafeLocation(world, bounds, cfg.getMaxAttempts(), 0, player);
    }

    private void findSafeLocation(World world, ConfigManager.WorldBounds bounds, int maxAttempts, int currentAttempt, Player player) {
        if (currentAttempt >= maxAttempts) {
            player.sendMessage(plugin.getLanguageManager().getPrefixedMessage("failed-find-location", "<attempts>", String.valueOf(maxAttempts)));
            plugin.getCooldownManager().setTeleporting(player.getUniqueId(), false);
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int x = random.nextInt(bounds.maxX() - bounds.minX() + 1) + bounds.minX();
        int z = random.nextInt(bounds.maxZ() - bounds.minZ() + 1) + bounds.minZ();

        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            ChunkSnapshot snapshot = chunk.getChunkSnapshot(true, false, false);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                Location safeLoc = scanSnapshotForLocation(world, snapshot, x, z);
                TaskScheduler.runRegionTask(plugin, new Location(world, x, 64, z), () -> {
                    if (safeLoc != null) {
                        startWarmup(player, safeLoc);
                    } else {
                        findSafeLocation(world, bounds, maxAttempts, currentAttempt + 1, player);
                    }
                });
            });
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

        // Scan top-down safely without main-thread world getter calls
        for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
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

        if (!player.isOnline()) {
            cd.setTeleporting(player.getUniqueId(), false);
            return;
        }

        if (player.getLocation().distanceSquared(initialLoc) > Math.pow(cfg.getMoveCancelDistance(), 2)) {
            player.sendMessage(lang.getPrefixedMessage("cancel-moved"));
            player.sendActionBar(lang.getMessage("cancel-actionbar"));
            if (cancelSound.enabled()) {
                player.playSound(player.getLocation(), cancelSound.sound(), cancelSound.volume(), cancelSound.pitch());
            }
            cd.setTeleporting(player.getUniqueId(), false);
            return;
        }

        if (countdown <= 0) {
            player.teleportAsync(targetLoc).thenAccept(success -> {
                if (success) {
                    player.sendActionBar(lang.getMessage("success-actionbar"));
                    player.sendMessage(lang.getPrefixedMessage("success-message"));
                    if (teleportSound.enabled()) {
                        player.playSound(player.getLocation(), teleportSound.sound(), teleportSound.volume(), teleportSound.pitch());
                    }
                    cd.setCooldown(player.getUniqueId());
                }
                cd.setTeleporting(player.getUniqueId(), false);
            });
            return;
        }

        player.sendActionBar(mm.deserialize(
                lang.getRawMessage("warmup-actionbar").replace("<seconds>", String.valueOf(countdown))
        ));

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
