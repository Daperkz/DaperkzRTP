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
import net.kyori.adventure.title.Title;
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
        String msg = getChannelMessage(key, "chat");
        return miniMessage.deserialize(prefix + msg);
    }

    /**
     * Overloaded method with single string replacement
     */
    public Component getPrefixedMessage(String key, String target, String replacement) {
        String prefix = messagesConfig.getString("prefix", "");
        String msg = getChannelMessage(key, "chat").replace(target, replacement);
        return miniMessage.deserialize(prefix + msg);
    }

    public Component getMessage(String key) {
        String msg = getChannelMessage(key, "chat");
        return miniMessage.deserialize(msg);
    }

    public String getRawMessage(String key) {
        return getChannelMessage(key, "chat");
    }

    public void sendNotification(Player player, String key, String placeholderTarget, String replacement) {
        ConfigManager.MessageToggle toggle = plugin.getConfigManager().getMessageToggle(key);

        if (toggle.allowsChat()) {
            if (placeholderTarget != null && replacement != null) {
                player.sendMessage(getPrefixedMessage(key, placeholderTarget, replacement));
            } else {
                player.sendMessage(getPrefixedMessage(key));
            }
        }

        if (toggle.allowsActionbar()) {
            String raw = getMessageForChannel(key, "actionbar", placeholderTarget, replacement);
            player.sendActionBar(miniMessage.deserialize(raw));
        }

        if (toggle.allowsTitle()) {
            String title = getMessageForChannel(key, "title", placeholderTarget, replacement);
            String subtitle = getOptionalMessage(key + ".title.subtitle", placeholderTarget, replacement);
            player.showTitle(Title.title(miniMessage.deserialize(title), miniMessage.deserialize(subtitle)));
        }
    }

    public void sendNotification(Player player, String key) {
        sendNotification(player, key, null, null);
    }

    private String getMessageForChannel(String key, String channel, String placeholderTarget, String replacement) {
        String raw = getChannelMessage(key, channel);
        if (placeholderTarget != null && replacement != null) {
            raw = raw.replace(placeholderTarget, replacement);
        }
        return raw;
    }

    private String getOptionalMessage(String path, String placeholderTarget, String replacement) {
        String raw = messagesConfig.isString(path) ? messagesConfig.getString(path, "") : "";
        if (placeholderTarget != null && replacement != null) {
            raw = raw.replace(placeholderTarget, replacement);
        }
        return raw;
    }

    private String getChannelMessage(String key, String channel) {
        String channelPath = key + "." + channel;
        if (messagesConfig.isString(channelPath)) {
            return messagesConfig.getString(channelPath, "");
        }

        if ("title".equals(channel) && messagesConfig.isString(key + ".title.text")) {
            return messagesConfig.getString(key + ".title.text", "");
        }

        return messagesConfig.getString(key, "");
    }
}
