/*
* ==============================================================================
* DaperkzRTP - Minecraft Plugin
* Copyright (c) 2026 Daperkz
*
* RTPGui
* ==============================================================================
*/
package com.daperkz.rtp.gui;

import com.daperkz.rtp.RTPPlugin;
import com.daperkz.rtp.config.ConfigManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class RTPGui implements InventoryHolder {

    private final Inventory inventory;

    public RTPGui(RTPPlugin plugin) {
        ConfigManager cfg = plugin.getConfigManager();
        this.inventory = Bukkit.createInventory(this, cfg.getGuiRows() * 9, cfg.getGuiTitle());
        initializeItems(cfg);
    }

    private void initializeItems(ConfigManager cfg) {
        if (cfg.isFillEmpty()) {
            ItemStack fillItem = new ItemStack(cfg.getFillMaterial());
            ItemMeta meta = fillItem.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(" "));
                fillItem.setItemMeta(meta);
            }
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, fillItem);
            }
        }

        for (ConfigManager.GuiItemHolder itemHolder : cfg.getGuiItems().values()) {
            if (itemHolder.slot() < inventory.getSize()) {
                ItemStack item = new ItemStack(itemHolder.material());
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(itemHolder.name());
                    meta.lore(itemHolder.lore());
                    item.setItemMeta(meta);
                }
                inventory.setItem(itemHolder.slot(), item);
            }
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
