package com.meteorsmp.ultimatemeteorsmp;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class UltimateMeteorSMP extends JavaPlugin {

    private static UltimateMeteorSMP instance;
    private OrderManager orderManager;

    @Override
    public void onEnable() {
        instance = this;

        // Copy config.yml from the jar to the plugin data folder if it does not exist yet.
        saveDefaultConfig();

        // Ensure runtime orders.yml exists as well.
        File ordersFile = new File(getDataFolder(), "orders.yml");
        if (!ordersFile.exists()) {
            if (getResource("orders.yml") != null) {
                saveResource("orders.yml", false);
            } else {
                try {
                    if (ordersFile.createNewFile()) {
                        getLogger().warning("orders.yml was missing from the JAR; created an empty file instead.");
                    }
                } catch (IOException e) {
                    getLogger().severe("Failed to create orders.yml: " + e.getMessage());
                    setEnabled(false);
                    return;
                }
            }
        }

        this.orderManager = new OrderManager(this);

        getCommand("orders").setExecutor(new OrderCommand(this));
        getCommand("ordershelp").setExecutor(new OrdersHelpCommand(this));

        getServer().getPluginManager().registerEvents(new OrderGUIListener(this), this);

        getLogger().info("UltimateMeteorSMP has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (orderManager != null) {
            orderManager.saveOrders();
        }
        getLogger().info("UltimateMeteorSMP has been disabled.");
    }

    public static UltimateMeteorSMP getInstance() {
        return instance;
    }

    public OrderManager getOrderManager() {
        return orderManager;
    }
}

private AuctionManager auctionManager;
private AuctionGuiManager auctionGuiManager;

// In onEnable():
this.auctionManager = new AuctionManager(this);
this.auctionGuiManager = new AuctionGuiManager(this);
getCommand("ah").setExecutor(new AuctionCommand(this, auctionGuiManager));
getServer().getPluginManager().registerEvents(new AuctionListener(this, auctionGuiManager), this);

public AuctionManager getAuctionManager() { return auctionManager; }
