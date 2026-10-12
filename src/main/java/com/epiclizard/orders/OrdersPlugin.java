package com.epiclizard.orders;

import com.epiclizard.orders.commands.OrdersCommand;
import com.epiclizard.orders.listeners.SignListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class OrdersPlugin extends JavaPlugin {

    private static OrdersPlugin instance;
    private OrderManager orderManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig(); // loads config.yml from resources if not present

        this.orderManager = new OrderManager(this);
        this.orderManager.load();

        // register commands
        getCommand("orders").setExecutor(new OrdersCommand(this));

        // register listeners
        getServer().getPluginManager().registerEvents(new SignListener(this), this);

        // schedule expiry check every 10 minutes
        long ticks = 20L * 60L * 10L;
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                orderManager.expireOrders();
            } catch (Exception e) {
                getLogger().severe("Error while expiring orders: " + e.getMessage());
                e.printStackTrace();
            }
        }, ticks, ticks);

        getLogger().info("OrdersPlugin v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (orderManager != null) {
            orderManager.save();
        }
        getLogger().info("OrdersPlugin disabled.");
    }

    public static OrdersPlugin getInstance() {
        return instance;
    }

    public OrderManager getOrderManager() {
        return orderManager;
    }
}
