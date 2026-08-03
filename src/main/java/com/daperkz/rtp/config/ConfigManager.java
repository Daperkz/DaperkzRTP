/*
* ==============================================================================
* MousseRTP - Minecraft Plugin
* Copyright (c) 2026 Daperkz
*
* ConfigManager
* ==============================================================================
*/
package com.daperkz.rtp.config;

import com.daperkz.rtp.RTPPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class ConfigManager {

    private final RTPPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<Integer, GuiItemHolder> guiItems = new HashMap<>();

    public ConfigManager(RTPPlugin plugin) {
        this.plugin = plugin;
        loadConfigData();
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public void loadConfigData() {
        guiItems.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("gui.items");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String path = "gui.items." + key;
                int slot = getConfig().getInt(path + ".slot");
                Material material = Material.matchMaterial(getConfig().getString(path + ".material", "STONE"));
                Component name = miniMessage.deserialize(getConfig().getString(path + ".name", ""));
                List<Component> lore = new ArrayList<>();
                for (String line : getConfig().getStringList(path + ".lore")) {
                    lore.add(miniMessage.deserialize(line));
                }
                String dimension = getConfig().getString(path + ".dimension", "");
                guiItems.put(slot, new GuiItemHolder(slot, material != null ? material : Material.STONE, name, lore, dimension));
            }
        }
    }

    public int getCooldownSeconds() {
        return getConfig().getInt("cooldown-seconds", 30);
    }
    public int getCountdownSeconds() {
        return getConfig().getInt("countdown-seconds", 3);
    }
    public double getMoveCancelDistance() {
        return getConfig().getDouble("move-cancel-distance", 1.0);
    }
    public int getMaxAttempts() {
        return getConfig().getInt("max-location-attempts", 80);
    }

    public Component getGuiTitle() {
        return miniMessage.deserialize(getConfig().getString("gui.title", "<blue>RTP</blue>"));
    }
    public int getGuiRows() {
        return Math.min(6, Math.max(1, getConfig().getInt("gui.rows", 3)));
    }
    public boolean isFillEmpty() {
        return getConfig().getBoolean("gui.fill-empty", true);
    }
    public Material getFillMaterial() {
        Material mat = Material.matchMaterial(getConfig().getString("gui.fill-item", "GRAY_STAINED_GLASS_PANE"));
        return mat != null ? mat : Material.GRAY_STAINED_GLASS_PANE;
    }

    public Map<Integer, GuiItemHolder> getGuiItems() { return guiItems; }

    public record SoundConfig(boolean enabled, Sound sound, float volume, float pitch, boolean pitchIncrease) {}
    public record WorldBounds(boolean enabled, String worldName, int minX, int maxX, int minZ, int maxZ) {}
    public record GuiItemHolder(int slot, Material material, Component name, List<Component> lore, String dimension) {}

    public SoundConfig getSoundConfig(String soundKey) {
        String path = "sounds." + soundKey;
        boolean enabled = getConfig().getBoolean(path + ".enabled", true);
        String soundName = getConfig().getString(path + ".sound", "");
        Sound sound;
        try {
            sound = Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            sound = Sound.BLOCK_NOTE_BLOCK_PLING;
        }
        float volume = (float) getConfig().getDouble(path + ".volume", 1.0);
        float pitch = (float) getConfig().getDouble(path + ".pitch", 1.0);
        boolean pitchIncrease = getConfig().getBoolean(path + ".pitch-increase", false);

        return new SoundConfig(enabled, sound, volume, pitch, pitchIncrease);
    }

    public WorldBounds getBounds(String dimensionType) {
        String path = "dimensions." + dimensionType.toLowerCase();
        return new WorldBounds(
                getConfig().getBoolean(path + ".enabled", true),
                getConfig().getString(path + ".world-name", "world"),
                getConfig().getInt(path + ".min-x", -10000),
                getConfig().getInt(path + ".max-x", 10000),
                getConfig().getInt(path + ".min-z", -10000),
                getConfig().getInt(path + ".max-z", 10000)
        );
    }
}
