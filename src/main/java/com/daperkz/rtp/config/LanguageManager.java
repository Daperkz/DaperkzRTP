/*
* ==============================================================================
* MousseRTP - Minecraft Plugin
* Copyright (c) 2026 Daperkz
*
* LanguageManager
* ==============================================================================
*/
package com.daperkz.rtp.config;

import com.daperkz.rtp.RTPPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LanguageManager {

    private final RTPPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private YamlConfiguration messagesConfig;

    public LanguageManager(RTPPlugin plugin) {
        this.plugin = plugin;
        loadLanguage();
    }

    public void loadLanguage() {
        String lang = plugin.getConfig().getString("language", "en");
        String fileName = "lang/messages_" + lang + ".yml";
        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            plugin.saveResource("lang/messages_en.yml", false);
            plugin.saveResource("lang/messages_fr.yml", false);
        }

        this.messagesConfig = YamlConfiguration.loadConfiguration(file);

        InputStream defStream = plugin.getResource(fileName);
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            this.messagesConfig.setDefaults(defConfig);
        }
    }

    /**
     * Standard message without placeholder replacements
     */
    public Component getPrefixedMessage(String key) {
        String prefix = messagesConfig.getString("prefix", "");
        String msg = messagesConfig.getString(key, "");
        return miniMessage.deserialize(prefix + msg);
    }

    /**
     * Overloaded method with single string replacement
     */
    public Component getPrefixedMessage(String key, String target, String replacement) {
        String prefix = messagesConfig.getString("prefix", "");
        String msg = messagesConfig.getString(key, "").replace(target, replacement);
        return miniMessage.deserialize(prefix + msg);
    }

    public Component getMessage(String key) {
        String msg = messagesConfig.getString(key, "");
        return miniMessage.deserialize(msg);
    }

    public String getRawMessage(String key) {
        return messagesConfig.getString(key, "");
    }
}
