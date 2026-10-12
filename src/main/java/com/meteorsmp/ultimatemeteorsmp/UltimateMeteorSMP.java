package com.meteorsmp.ultimatemeteorsmp;

import org.bukkit.plugin.java.JavaPlugin;

public final class UltimateMeteorSMP extends JavaPlugin {

    private static UltimateMeteorSMP instance;
    private OrderManager orderManager;

    @Override
    public void onEnable() {
        instance = this;

        // Copy config.yml from the jar to the plugin data folder if it does not exist yet.
        saveDefaultConfig();

        // Ensure runtime orders.yml exists as well.
        if (!new java.io.File(getDataFolder(), "orders.yml").exists()) {
            saveResource("orders.yml", false);
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
