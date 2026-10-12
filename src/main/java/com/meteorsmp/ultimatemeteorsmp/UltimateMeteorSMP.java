package com.meteorsmp.ultimatemeteorsmp;

import com.meteorsmp.ultimatemeteorsmp.auction.AuctionCommand;
import com.meteorsmp.ultimatemeteorsmp.auction.AuctionGuiManager;
import com.meteorsmp.ultimatemeteorsmp.auction.AuctionListener;
import com.meteorsmp.ultimatemeteorsmp.auction.AuctionManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

public final class UltimateMeteorSMP extends JavaPlugin {

    private static UltimateMeteorSMP instance;
    private OrderManager orderManager;

    // Auction support
    private AuctionManager auctionManager;
    private AuctionGuiManager auctionGuiManager;

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

        // Initialize auction subsystem (if present)
        try {
            this.auctionManager = new AuctionManager(this);
            this.auctionGuiManager = new AuctionGuiManager(this);
            if (getCommand("ah") != null) {
                getCommand("ah").setExecutor(new AuctionCommand(this, auctionGuiManager));
            }
            getServer().getPluginManager().registerEvents(new AuctionListener(this, auctionGuiManager), this);
        } catch (NoClassDefFoundError | Exception e) {
            // If auction classes are missing or fail to initialize, don't break the whole plugin.
            getLogger().warning("Auction subsystem failed to initialize: " + e.getMessage());
            auctionManager = null;
            auctionGuiManager = null;
        }

        // Register manual update check command
        if (getCommand("checkupdate") != null) {
            getCommand("checkupdate").setExecutor(new CommandExecutor() {
                @Override
                public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                    sender.sendMessage("Checking for updates...");
                    try {
                        Class<?> uc = Class.forName("com.meteorsmp.ultimatemeteorsmp.UpdateChecker");
                        Method m = uc.getMethod("checkForUpdatesAsync", JavaPlugin.class, boolean.class);
                        m.invoke(null, UltimateMeteorSMP.this, true);
                    } catch (ClassNotFoundException | NoSuchMethodException e) {
                        sender.sendMessage("UpdateChecker not available.");
                    } catch (Exception e) {
                        sender.sendMessage("Update check failed: " + e.getMessage());
                    }
                    return true;
                }
            });
        }

        // Asynchronously check for updates on startup (console + ops)
        try {
            Class<?> uc = Class.forName("com.meteorsmp.ultimatemeteorsmp.UpdateChecker");
            Method m = uc.getMethod("checkForUpdatesAsync", JavaPlugin.class, boolean.class);
            m.invoke(null, this, true);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            getLogger().info("UpdateChecker not present; skipping update check.");
        } catch (Exception e) {
            getLogger().warning("Update check failed: " + e.getMessage());
        }

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

    // Auction getter (may be null if initialization failed)
    public AuctionManager getAuctionManager() {
        return auctionManager;
    }
}
