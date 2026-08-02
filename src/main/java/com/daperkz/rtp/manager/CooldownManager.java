/*
* ==============================================================================
* MousseRTP - Minecraft Plugin
* Copyright (c) 2026 Daperkz
*
* CooldownManager
* ==============================================================================
*/
package com.daperkz.rtp.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> activeTeleports = new ConcurrentHashMap<>();

    public boolean isOnCooldown(UUID uuid, int cooldownSeconds) {
        if (!cooldowns.containsKey(uuid)) return false;
        long elapsed = (System.currentTimeMillis() - cooldowns.get(uuid)) / 1000;
        return elapsed < cooldownSeconds;
    }

    public long getRemainingCooldown(UUID uuid, int cooldownSeconds) {
        if (!cooldowns.containsKey(uuid)) return 0;
        long elapsed = (System.currentTimeMillis() - cooldowns.get(uuid)) / 1000;
        return Math.max(0, cooldownSeconds - elapsed);
    }

    public void setCooldown(UUID uuid) {
        cooldowns.put(uuid, System.currentTimeMillis());
    }

    public boolean isTeleporting(UUID uuid) {
        return activeTeleports.getOrDefault(uuid, false);
    }

    public void setTeleporting(UUID uuid, boolean state) {
        if (state) {
            activeTeleports.put(uuid, true);
        } else {
            activeTeleports.remove(uuid);
        }
    }
}
