/*
* ==============================================================================
* MousseRTP - Minecraft Plugin
* Copyright (c) 2026 Daperkz
*
* RTPPlugin
* ==============================================================================
*/
package com.daperkz.rtp;

import com.daperkz.rtp.command.RTPCommand;
import com.daperkz.rtp.config.ConfigManager;
import com.daperkz.rtp.config.LanguageManager;
import com.daperkz.rtp.listener.GuiListener;
import com.daperkz.rtp.manager.CooldownManager;
import com.daperkz.rtp.manager.RTPManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class RTPPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private CooldownManager cooldownManager;
    private RTPManager rtpManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.cooldownManager = new CooldownManager();
        this.rtpManager = new RTPManager(this);

        if (getCommand("rtp") != null) {
            RTPCommand executor = new RTPCommand(this);
            getCommand("rtp").setExecutor(executor);
            getCommand("rtp").setTabCompleter(executor);
        }
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getLogger().info("MousseRTP has been enabled!");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        this.configManager.loadConfigData();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
    public RTPManager getRTPManager() {
        return rtpManager;
    }
}
