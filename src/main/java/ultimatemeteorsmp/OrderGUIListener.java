package com.meteorsmp.ultimatemeteorsmp;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class OrderGUIListener implements Listener {
    private final UltimateMeteorSMP plugin;

    public OrderGUIListener(UltimateMeteorSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        if (title.contains("ǫʀᴅᴇʀѕ")) {
            event.setCancelled(true);
            // Handle GUI clicks here
        }
    }
}
