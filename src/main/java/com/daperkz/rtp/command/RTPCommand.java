/*
* ==============================================================================
* DaperkzRTP - Minecraft Plugin
* Copyright (c) 2026 Daperkz
*
* RTPCommand
* ==============================================================================
*/
package com.daperkz.rtp.command;

import com.daperkz.rtp.RTPPlugin;
import com.daperkz.rtp.config.ConfigManager;
import com.daperkz.rtp.gui.RTPGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RTPCommand implements CommandExecutor, TabCompleter {

    private final RTPPlugin plugin;

    public RTPCommand(RTPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        ConfigManager cfg = plugin.getConfigManager();

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("Daperkz.rtp.admin")) {
                sendConfiguredMessage(sender, "no-permission");
                return true;
            }
            plugin.reloadPluginConfig();
            sendConfiguredMessage(sender, "reload");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sendConfiguredMessage(sender, "player-only");
            return true;
        }

        String targetArg;

        if (args.length == 0) {
            if (cfg.getGuiEnabled()) {
                player.openInventory(new RTPGui(plugin).getInventory());
                return true;
            } else {
                String currentWorldName = player.getWorld().getName();
                targetArg = cfg.getDimensionKeyByWorldName(currentWorldName);
                if (targetArg == null) {
                    plugin.getLanguageManager().sendNotification(player, "world-disabled");
                    return true;
                }
            }
        } else {
            if (args[0].equalsIgnoreCase("cancel")) {
                boolean cancelled = plugin.getRTPManager().cancelRTP(player);
                if (cancelled) {
                    plugin.getLanguageManager().sendNotification(player, "cancel");
                } else {
                    plugin.getLanguageManager().sendNotification(player, "not-teleporting");
                }
                return true;
            }
            targetArg = args[0].toLowerCase();
        }

        ConfigManager.WorldBounds bounds = cfg.getBounds(targetArg);

        if (bounds == null) {
            plugin.getLanguageManager().sendNotification(player, "unknown-world");
            return true;
        }

        plugin.getRTPManager().processRTP(player, bounds);
        return true;
    }

    private void sendConfiguredMessage(CommandSender sender, String messageKey) {
        if (sender instanceof Player player) {
            plugin.getLanguageManager().sendNotification(player, messageKey);
        } else {
            sender.sendMessage(plugin.getLanguageManager().getPrefixedMessage(messageKey));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>(List.of("overworld", "nether", "end", "cancel"));
            if (sender.hasPermission("Daperkz.rtp.admin")) {
                suggestions.add("reload");
            }
            return suggestions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
