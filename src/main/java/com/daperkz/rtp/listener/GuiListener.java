/*
* ==============================================================================
* MousseRTP - Minecraft Plugin
* Copyright (c) 2026 Daperkz
*
* GuiListener
* ==============================================================================
*/
package com.daperkz.rtp.listener;

import com.daperkz.rtp.RTPPlugin;
import com.daperkz.rtp.config.ConfigManager;
import com.daperkz.rtp.gui.RTPGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiListener implements Listener {

    private final RTPPlugin plugin;

    public GuiListener(RTPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RTPGui))
            return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        int slot = event.getRawSlot();
        ConfigManager cfg = plugin.getConfigManager();
        ConfigManager.GuiItemHolder clickedItem = cfg.getGuiItems().get(slot);

        if (clickedItem != null && !clickedItem.dimension().isEmpty()) {
            player.closeInventory();
            plugin.getRTPManager().processRTP(player, cfg.getBounds(clickedItem.dimension()));
        }
    }
}
