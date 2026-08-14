/*
* ==============================================================================
* DaperkzRTP - Minecraft Plugin
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
import org.bukkit.entity.Player;

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
        saveLangFile("lang/messages_en.yml");
        saveLangFile("lang/messages_fr.yml");

        String lang = plugin.getConfig().getString("language", "en");
        String fileName = "lang/messages_" + lang + ".yml";
        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            fileName = "lang/messages_en.yml";
            file = new File(plugin.getDataFolder(), fileName);
        }

        this.messagesConfig = YamlConfiguration.loadConfiguration(file);

        InputStream defStream = plugin.getResource(fileName);
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            this.messagesConfig.setDefaults(defConfig);
        }
    }

    private void saveLangFile(String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
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

    public void sendNotification(Player player, String toggleKey, String chatKey, String actionbarKey, String placeholderTarget, String replacement) {
        ConfigManager.MessageToggle toggle = plugin.getConfigManager().getMessageToggle(toggleKey);

        if (toggle.allowsChat() && chatKey != null && !chatKey.isEmpty()) {
            if (placeholderTarget != null && replacement != null) {
                player.sendMessage(getPrefixedMessage(chatKey, placeholderTarget, replacement));
            } else {
                player.sendMessage(getPrefixedMessage(chatKey));
            }
        }

        if (toggle.allowsActionbar() && actionbarKey != null && !actionbarKey.isEmpty()) {
            String raw = getRawMessage(actionbarKey);
            if (placeholderTarget != null && replacement != null) {
                raw = raw.replace(placeholderTarget, replacement);
            }
            player.sendActionBar(miniMessage.deserialize(raw));
        }
    }

    public void sendNotification(Player player, String toggleKey, String chatKey, String actionbarKey) {
        sendNotification(player, toggleKey, chatKey, actionbarKey, null, null);
    }
}
