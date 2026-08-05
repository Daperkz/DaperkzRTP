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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Seul un joueur peut exécuter cette commande.");
            return true;
        }

        ConfigManager cfg = plugin.getConfigManager();

        if (args.length == 0) {
            player.openInventory(new RTPGui(plugin).getInventory());
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("Daperkz.rtp.admin")) {
                sender.sendMessage(plugin.getLanguageManager().getPrefixedMessage("no-permission"));
                return true;
            }
            plugin.reloadPluginConfig();
            sender.sendMessage(plugin.getLanguageManager().getPrefixedMessage("reload-success"));
            return true;
        }

        String targetArg = args[0].toLowerCase();
        ConfigManager.WorldBounds bounds = switch (targetArg) {
            case "overworld", "world" -> cfg.getBounds("overworld");
            case "nether" -> cfg.getBounds("nether");
            case "end", "the_end" -> cfg.getBounds("end");
            default -> null;
        };

        if (bounds == null) {
            player.sendMessage(plugin.getLanguageManager().getPrefixedMessage("unknown-world"));
            return true;
        }

        plugin.getRTPManager().processRTP(player, bounds);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>(List.of("overworld", "nether", "end"));
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
