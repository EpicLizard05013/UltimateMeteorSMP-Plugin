package com.meteorsmp.ultimatemeteorsmp;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class OrderManager {
    private final UltimateMeteorSMP plugin;
    private final Map<String, Order> activeOrders = new LinkedHashMap<>();
    private final Map<UUID, Double> balances = new HashMap<>();
    private File ordersFile;
    private FileConfiguration ordersConfig;

    public OrderManager(UltimateMeteorSMP plugin) {
        this.plugin = plugin;
        loadOrders();
    }

    public boolean isIllegalItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return true;
        String name = item.getType().name().toLowerCase();
        return name.contains("spawn_egg") || name.contains("warden") || name.contains("dragon_egg") 
                || name.contains("bedrock") || name.contains("barrier") || name.contains("command_block") 
                || name.contains("structure") || name.contains("debug") || name.contains("portal") 
                || name.contains("light") || name.contains("allow") || name.contains("deny") || name.contains("border");
    }

    public void createOrder(String id, String owner, ItemStack item, int amount, double price) {
        Order order = new Order(id, owner, item, amount, price, 0, 0, System.currentTimeMillis());
        activeOrders.put(id, order);
        saveOrders();
    }

    public void loadOrders() {
        ordersFile = new File(plugin.getDataFolder(), "orders.yml");
        if (!ordersFile.exists()) {
            plugin.saveResource("orders.yml", false);
        }
        ordersConfig = YamlConfiguration.loadConfiguration(ordersFile);
    }

    public void saveOrders() {
        try {
            ordersConfig.save(ordersFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save orders.yml!");
        }
    }

    public Map<String, Order> getActiveOrders() {
        return activeOrders;
    }
    
    public double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, 0.0);
    }

    public void addBalance(UUID uuid, double amount) {
        balances.put(uuid, getBalance(uuid) + amount);
    }

    public void removeBalance(UUID uuid, double amount) {
        balances.put(uuid, Math.max(0, getBalance(uuid) - amount));
    }
}
