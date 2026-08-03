/*
* ==============================================================================
* MousseRTP - Minecraft Plugin
* Copyright (c) 2026 Daperkz
*
* RTPManager
* ==============================================================================
*/
package com.daperkz.rtp.manager;

import com.daperkz.rtp.RTPPlugin;
import com.daperkz.rtp.config.ConfigManager;
import com.daperkz.rtp.config.LanguageManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class RTPManager {

    private final RTPPlugin plugin;
    private final Random random = new Random();
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

        int x = random.nextInt(bounds.maxX() - bounds.minX() + 1) + bounds.minX();
        int z = random.nextInt(bounds.maxZ() - bounds.minZ() + 1) + bounds.minZ();

        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                Location safeLoc = scanChunkForLocation(world, chunk, x, z);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (safeLoc != null) {
                        startWarmup(player, safeLoc);
                    } else {
                        findSafeLocation(world, bounds, maxAttempts, currentAttempt + 1, player);
                    }
                });
            });
        });
    }
    private CompletableFuture<Location> findSafeLocationAsync(World world, ConfigManager.WorldBounds bounds, int maxAttempts) {
        CompletableFuture<Location> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int attempts = 0;

            while (attempts < maxAttempts) {
                attempts++;
                int x = random.nextInt(bounds.maxX() - bounds.minX() + 1) + bounds.minX();
                int z = random.nextInt(bounds.maxZ() - bounds.minZ() + 1) + bounds.minZ();

                CompletableFuture<Chunk> chunkFuture = world.getChunkAtAsync(x >> 4, z >> 4);
                Chunk chunk = chunkFuture.join();

                Location safeLoc = scanChunkForLocation(world, chunk, x, z);
                if (safeLoc != null) {
                    future.complete(safeLoc);
                    return;
                }
            }
            future.complete(null);
        });

        return future;
    }

    private Location scanChunkForLocation(World world, Chunk chunk, int x, int z) {
        return switch (world.getEnvironment()) {
            case NETHER -> scanNether(world, chunk, x, z);
            case THE_END -> scanEnd(world, chunk, x, z);
            default -> scanOverworld(world, chunk, x, z);
        };
    }

    private Location scanOverworld(World world, Chunk chunk, int x, int z) {
        int relX = x & 15;
        int relZ = z & 15;

        // Scan top-down safely without main-thread world getter calls
        for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
            Material type = chunk.getBlock(relX, y, relZ).getType();
            if (type != Material.AIR && type != Material.CAVE_AIR && type != Material.VOID_AIR) {
                if (!DANGEROUS_BLOCKS.contains(type)) {
                    return new Location(world, x + 0.5, y + 1, z + 0.5);
                }
                break;
            }
        }
        return null;
    }

    private Location scanNether(World world, Chunk chunk, int x, int z) {
        for (int y = 115; y >= 35; y--) {
            Material floorMat = chunk.getBlock(x & 15, y, z & 15).getType();
            if (NETHER_VALID_FLOORS.contains(floorMat)) {
                Material feetMat = chunk.getBlock(x & 15, y + 1, z & 15).getType();
                Material headMat = chunk.getBlock(x & 15, y + 2, z & 15).getType();

                if (feetMat.isAir() && headMat.isAir()) {
                    return new Location(world, x + 0.5, y + 1, z + 0.5);
                }
            }
        }
        return null;
    }

    private Location scanEnd(World world, Chunk chunk, int x, int z) {
        int highestY = world.getHighestBlockYAt(x, z);
        Material targetBlock = chunk.getBlock(x & 15, highestY, z & 15).getType();

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

        new BukkitRunnable() {
            int countdown = cfg.getCountdownSeconds();

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cd.setTeleporting(player.getUniqueId(), false);
                    cancel();
                    return;
                }

                if (player.getLocation().distanceSquared(initialLoc) > Math.pow(cfg.getMoveCancelDistance(), 2)) {
                    player.sendMessage(lang.getPrefixedMessage("cancel-moved"));
                    player.sendActionBar(lang.getMessage("cancel-actionbar"));
                    cd.setTeleporting(player.getUniqueId(), false);
                    cancel();
                    return;
                }

                if (countdown <= 0) {
                    player.teleportAsync(targetLoc).thenAccept(success -> {
                        if (success) {
                            player.sendActionBar(lang.getMessage("success-actionbar"));
                            player.sendMessage(lang.getPrefixedMessage("success-message"));
                            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                            cd.setCooldown(player.getUniqueId());
                        }
                        cd.setTeleporting(player.getUniqueId(), false);
                    });
                    cancel();
                    return;
                }

                player.sendActionBar(mm.deserialize(
                        lang.getRawMessage("warmup-actionbar").replace("<seconds>", String.valueOf(countdown))
                ));

                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
