package com.epiclizard.orders;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class OrderManager {
    private final OrdersPlugin plugin;
    private final File ordersFile;
    private final YamlConfiguration ordersConfig;
    private final File balancesFile;
    private final YamlConfiguration balancesConfig;

    private final Map<String, Order> orders = new LinkedHashMap<>();
    private final Map<UUID, Integer> viewerPage = new HashMap<>();
    private final Map<UUID, ItemStack> setupItem = new HashMap<>();
    private final Map<UUID, Integer> setupAmount = new HashMap<>();
    private final Map<UUID, Double> setupPrice = new HashMap<>();
    private final Map<UUID, Boolean> awaitingAmount = new HashMap<>();
    private final Map<UUID, Boolean> awaitingPrice = new HashMap<>();
    private final Map<UUID, String> fulfillTarget = new HashMap<>();

    public OrderManager(OrdersPlugin plugin) {
        this.plugin = plugin;
        this.ordersFile = new File(plugin.getDataFolder(), "orders.yml");
        this.ordersConfig = YamlConfiguration.loadConfiguration(this.ordersFile);
        this.balancesFile = new File(plugin.getDataFolder(), "balances.yml");
        this.balancesConfig = YamlConfiguration.loadConfiguration(this.balancesFile);
        load();
    }

    public synchronized void load() {
        if (!ordersFile.exists()) {
            ordersFile.getParentFile().mkdirs();
            try {
                ordersFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!balancesFile.exists()) {
            balancesFile.getParentFile().mkdirs();
            try {
                balancesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        orders.clear();
        if (ordersConfig.isConfigurationSection("active")) {
            for (String id : Objects.requireNonNull(ordersConfig.getConfigurationSection("active")).getKeys(false)) {
                Order order = Order.fromConfig(id, Objects.requireNonNull(ordersConfig.getConfigurationSection("active." + id)));
                if (order != null) {
                    orders.put(id, order);
                }
            }
        }
    }

    public synchronized void save() {
        try {
            ordersConfig.set("active", null);
            for (Order order : orders.values()) {
                var section = ordersConfig.createSection("active." + order.getId());
                order.saveToConfig(section);
            }
            ordersConfig.set("system.id_counter", orders.size());
            ordersConfig.save(ordersFile);
            balancesConfig.save(balancesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized List<Order> getActiveOrders() {
        return new ArrayList<>(orders.values());
    }

    public synchronized Order getOrder(String id) {
        return orders.get(id);
    }

    public synchronized void clearAllOrders() {
        orders.clear();
        save();
    }

    public synchronized void createDebugOrder(ItemStack item, int amount, double price, String ownerName) {
        if (item == null || item.getType() == Material.AIR) {
            item = new ItemStack(Material.DIAMOND);
        }
        int count = ordersConfig.getInt("system.id_counter", 0) + 1;
        ordersConfig.set("system.id_counter", count);
        String id = "ORD_" + count;
        orders.put(id, new Order(id, null, ownerName, item.clone(), amount, 0, price, System.currentTimeMillis() / 1000L));
        save();
    }

    public synchronized void checkExpiry() {
        long now = System.currentTimeMillis() / 1000L;
        List<String> remove = new ArrayList<>();
        for (Order order : new ArrayList<>(orders.values())) {
            if (order.getPrice() <= 0 || !order.isExpired(now)) {
                continue;
            }
            int remaining = order.getAmount() - order.getProgress();
            if (remaining > 0 && order.getOwnerUuid() != null && !order.getOwnerName().equalsIgnoreCase("Server")) {
                addBalance(order.getOwnerUuid(), remaining * order.getPrice());
            }
            order.setAmount(order.getProgress());
            order.setPrice(0);
            if (order.getProgress() <= 0) {
                remove.add(order.getId());
            }
        }
        for (String id : remove) {
            orders.remove(id);
        }
        save();
    }

    public synchronized void addOrder(Order order) {
        orders.put(order.getId(), order);
        save();
    }

    public synchronized List<Order> getOrdersByOwner(String uuid) {
        List<Order> result = new ArrayList<>();
        for (Order order : orders.values()) {
            if (uuid.equals(order.getOwnerUuid())) {
                result.add(order);
            }
        }
        return result;
    }

    public synchronized void setViewerPage(Player player, int page) {
        viewerPage.put(player.getUniqueId(), page);
    }

    public synchronized int getViewerPage(Player player) {
        return viewerPage.getOrDefault(player.getUniqueId(), 1);
    }

    public synchronized void setSetupItem(Player player, ItemStack stack) {
        setupItem.put(player.getUniqueId(), stack);
    }

    public synchronized ItemStack getSetupItem(Player player) {
        return setupItem.get(player.getUniqueId());
    }

    public synchronized void clearSetup(Player player) {
        setupItem.remove(player.getUniqueId());
        setupAmount.remove(player.getUniqueId());
        setupPrice.remove(player.getUniqueId());
    }

    public synchronized int getSetupAmount(Player player) {
        return setupAmount.getOrDefault(player.getUniqueId(), 1);
    }

    public synchronized void setSetupAmount(Player player, int amount) {
        setupAmount.put(player.getUniqueId(), amount);
    }

    public synchronized double getSetupPrice(Player player) {
        return setupPrice.getOrDefault(player.getUniqueId(), 10.0);
    }

    public synchronized void setSetupPrice(Player player, double price) {
        setupPrice.put(player.getUniqueId(), price);
    }

    public synchronized void setAwaitingAmount(Player player, boolean value) {
        awaitingAmount.put(player.getUniqueId(), value);
    }

    public synchronized boolean isAwaitingAmount(Player player) {
        return Boolean.TRUE.equals(awaitingAmount.get(player.getUniqueId()));
    }

    public synchronized void setAwaitingPrice(Player player, boolean value) {
        awaitingPrice.put(player.getUniqueId(), value);
    }

    public synchronized boolean isAwaitingPrice(Player player) {
        return Boolean.TRUE.equals(awaitingPrice.get(player.getUniqueId()));
    }

    public synchronized boolean finalizeOrderCreation(Player player) {
        ItemStack item = getSetupItem(player);
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        int amount = Math.max(1, getSetupAmount(player));
        double price = Math.max(1.0, getSetupPrice(player));
        double totalCost = amount * price;

        double balance = balancesConfig.getDouble(player.getUniqueId().toString(), 0.0);
        if (balance < totalCost) {
            player.sendMessage(ChatColor.RED + "You don't have enough money! Required: " + ChatColor.YELLOW + "$" + totalCost + ChatColor.RED + ".");
            return false;
        }

        balancesConfig.set(player.getUniqueId().toString(), balance - totalCost);
        int counter = ordersConfig.getInt("system.id_counter", 0) + 1;
        String id = "ORD_" + counter;
        ordersConfig.set("system.id_counter", counter);

        orders.put(id, new Order(id, player.getUniqueId().toString(), player.getName(), item.clone(), amount, 0, price, System.currentTimeMillis() / 1000L));
        save();
        clearSetup(player);
        return true;
    }

    public synchronized void setFulfillTarget(Player player, String id) {
        fulfillTarget.put(player.getUniqueId(), id);
    }

    public synchronized String getFulfillTarget(Player player) {
        return fulfillTarget.get(player.getUniqueId());
    }

    public synchronized void clearFulfillTarget(Player player) {
        fulfillTarget.remove(player.getUniqueId());
    }

    public synchronized int depositFromPlayerForOrder(Player player, Order order) {
        int needed = order.getAmount() - order.getProgress();
        if (needed <= 0) {
            return 0;
        }

        Inventory inv = player.getInventory();
        int deposited = 0;

        for (int slot = 0; slot < inv.getSize() && deposited < needed; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            if (stack.getType() == order.getItem().getType()) {
                int take = Math.min(stack.getAmount(), needed - deposited);
                if (take <= 0) continue;
                deposited += take;
                if (stack.getAmount() > take) {
                    stack.setAmount(stack.getAmount() - take);
                    inv.setItem(slot, stack);
                } else {
                    inv.setItem(slot, null);
                }
            } else if (stack.getItemMeta() instanceof BlockStateMeta blockStateMeta) {
                try {
                    var state = blockStateMeta.getBlockState();
                    if (state instanceof ShulkerBox box) {
                        ItemStack[] contents = box.getInventory().getContents();
                        boolean modified = false;
                        for (int index = 0; index < contents.length && deposited < needed; index++) {
                            ItemStack inner = contents[index];
                            if (inner == null || inner.getType() == Material.AIR) continue;
                            if (inner.getType() == order.getItem().getType()) {
                                int take = Math.min(inner.getAmount(), needed - deposited);
                                if (take <= 0) continue;
                                deposited += take;
                                if (inner.getAmount() > take) {
                                    inner.setAmount(inner.getAmount() - take);
                                    contents[index] = inner;
                                } else {
                                    contents[index] = null;
                                }
                                modified = true;
                            }
                        }
                        if (modified) {
                            box.getInventory().setContents(contents);
                            blockStateMeta.setBlockState(state);
                            stack.setItemMeta(blockStateMeta);
                            inv.setItem(slot, stack);
                        }
                    }
                } catch (Exception ignored) {
                    // ignore invalid block state cases
                }
            }
        }

        return deposited;
    }

    public synchronized void addBalance(String uuid, double amount) {
        double current = balancesConfig.getDouble(uuid, 0.0);
        balancesConfig.set(uuid, current + amount);
        try {
            balancesConfig.save(balancesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
