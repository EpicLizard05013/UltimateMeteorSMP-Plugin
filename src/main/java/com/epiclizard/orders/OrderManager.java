package com.epiclizard.orders;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class OrderManager {
    private final OrdersPlugin plugin;
    private final File ordersFile;
    private final YamlConfiguration ordersConfig;
    private final File balancesFile;
    private final YamlConfiguration balancesConfig;

    private final Map<String, Order> orders = new LinkedHashMap<>();

    // viewer state + setup
    private final Map<java.util.UUID, Integer> viewerPage = new HashMap<>();
    private final Map<java.util.UUID, ItemStack> setupItem = new HashMap<>();
    private final Map<java.util.UUID, Integer> setupAmount = new HashMap<>();
    private final Map<java.util.UUID, Double> setupPrice = new HashMap<>();
    private final Map<java.util.UUID, Boolean> awaitingAmount = new HashMap<>();
    private final Map<java.util.UUID, Boolean> awaitingPrice = new HashMap<>();
    private final Map<java.util.UUID, String> fulfillTarget = new HashMap<>();

    public OrderManager(OrdersPlugin plugin) {
        this.plugin = plugin;
        ordersFile = new File(plugin.getDataFolder(), "orders.yml");
        ordersConfig = YamlConfiguration.loadConfiguration(ordersFile);
        balancesFile = new File(plugin.getDataFolder(), "balances.yml");
        balancesConfig = YamlConfiguration.loadConfiguration(balancesFile);
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
        ordersConfig.options().copyDefaults(true);
        ordersConfig.addDefault("system.id_counter", 0);
        try {
            ordersConfig.save(ordersFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // load orders
        orders.clear();
        if (ordersConfig.isConfigurationSection("active")) {
            for (String id : ordersConfig.getConfigurationSection("active").getKeys(false)) {
                Order o = Order.fromConfig(id, ordersConfig.getConfigurationSection("active." + id));
                if (o != null) orders.put(id, o);
            }
        }
        // load balances
        balancesConfig.options().copyDefaults(true);
        try { balancesConfig.save(balancesFile); } catch (IOException ignored) {}
    }

    public synchronized void save() {
        try {
            // clear
            ordersConfig.set("active", null);
            for (Order o : new ArrayList<>(orders.values())) {
                ordersConfig.createSection("active." + o.getId());
                o.saveToConfig(ordersConfig.createSection("active." + o.getId()));
            }
            // id counter
            ordersConfig.set("system.id_counter", orders.size());
            ordersConfig.save(ordersFile);

            balancesConfig.save(balancesFile);
        } catch (Exception e) {
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

    public synchronized void checkExpiry() {
        long now = System.currentTimeMillis() / 1000L;
        List<String> toRemove = new ArrayList<>();
        for (Order o : new ArrayList<>(orders.values())) {
            if (o.getPrice() <= 0) continue;
            if (o.getExpireTime() <= now) {
                int remaining = o.getAmount() - o.getProgress();
                if (remaining > 0 && o.getOwnerUuid() != null && !o.getOwnerName().equals("Server")) {
                    double refund = remaining * o.getPrice();
                    addBalance(o.getOwnerUuid(), refund);
                }
                // set amount to progress and remove price
                o.setAmount(o.getProgress());
                o.setPrice(0);
                if (o.getProgress() <= 0) {
                    toRemove.add(o.getId());
                }
            }
        }
        for (String id : toRemove) orders.remove(id);
        save();
    }

    public synchronized void createDebugOrder(ItemStack item, int amount, double price, String ownerName) {
        int idc = ordersConfig.getInt("system.id_counter", 0) + 1;
        String id = "ORD_" + idc;
        ordersConfig.set("system.id_counter", idc);
        Order o = new Order(id, ownerName.equals("Server") ? null : null, ownerName, item.clone(), amount, 0, price, 0, System.currentTimeMillis() / 1000L);
        orders.put(id, o);
        save();
    }

    public synchronized void addOrder(Order o) {
        orders.put(o.getId(), o);
        save();
    }

    public synchronized List<Order> getOrdersByOwner(String uuid) {
        return orders.values().stream().filter(o -> uuid.equals(o.getOwnerUuid())).collect(Collectors.toList());
    }

    public synchronized void setViewerPage(Player p, int page) { viewerPage.put(p.getUniqueId(), page); }
    public synchronized int getViewerPage(Player p) { return viewerPage.getOrDefault(p.getUniqueId(), 1); }

    public synchronized void setSetupItem(Player p, ItemStack item) { setupItem.put(p.getUniqueId(), item); }
    public synchronized ItemStack getSetupItem(Player p) { return setupItem.get(p.getUniqueId()); }
    public synchronized void clearSetup(Player p) { setupItem.remove(p.getUniqueId()); setupAmount.remove(p.getUniqueId()); setupPrice.remove(p.getUniqueId()); }

    public synchronized int getSetupAmount(Player p) { return setupAmount.getOrDefault(p.getUniqueId(), 1); }
    public synchronized void setSetupAmount(Player p, int v) { setupAmount.put(p.getUniqueId(), v); }
    public synchronized double getSetupPrice(Player p) { return setupPrice.getOrDefault(p.getUniqueId(), 10.0); }
    public synchronized void setSetupPrice(Player p, double v) { setupPrice.put(p.getUniqueId(), v); }

    public synchronized void setAwaitingAmount(Player p, boolean v) { awaitingAmount.put(p.getUniqueId(), v); }
    public synchronized boolean isAwaitingAmount(Player p) { return awaitingAmount.getOrDefault(p.getUniqueId(), false); }
    public synchronized void setAwaitingPrice(Player p, boolean v) { awaitingPrice.put(p.getUniqueId(), v); }
    public synchronized boolean isAwaitingPrice(Player p) { return awaitingPrice.getOrDefault(p.getUniqueId(), false); }

    public synchronized boolean finalizeOrderCreation(Player p) {
        ItemStack item = getSetupItem(p);
        if (item == null) return false;
        int amount = getSetupAmount(p);
        if (amount <= 0) amount = 1;
        double price = getSetupPrice(p);
        if (price <= 0) price = 1;
        double totalCost = amount * price;
        double bal = balancesConfig.getDouble(p.getUniqueId().toString(), 0.0);
        if (bal < totalCost) {
            p.sendMessage("\u00A7cYou don't have enough money! Required: \u00A7e$" + totalCost + "\u00A7c.");
            return false;
        }
        // subtract
        balancesConfig.set(p.getUniqueId().toString(), bal - totalCost);
        int idc = ordersConfig.getInt("system.id_counter", 0) + 1;
        String id = "ORD_" + idc;
        ordersConfig.set("system.id_counter", idc);
        Order o = new Order(id, p.getUniqueId().toString(), p.getName(), item.clone(), amount, 0, price, 0, System.currentTimeMillis() / 1000L);
        addOrder(o);
        clearSetup(p);
        return true;
    }

    public synchronized void setFulfillTarget(Player p, String id) { fulfillTarget.put(p.getUniqueId(), id); }
    public synchronized String getFulfillTarget(Player p) { return fulfillTarget.get(p.getUniqueId()); }
    public synchronized void clearFulfillTarget(Player p) { fulfillTarget.remove(p.getUniqueId()); }

    public synchronized int depositFromPlayerForOrder(Player p, Order o) {
        int deposited = 0;
        MaterialMatcher matcher = new MaterialMatcher(o.getItem().getType());
        // scan player inventory (slots 0-35)
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is == null) continue;
            if (is.getType() == o.getItem().getType()) {
                int need = o.getAmount() - o.getProgress() - deposited;
                if (need <= 0) break;
                int take = Math.min(is.getAmount(), need);
                deposited += take;
                if (is.getAmount() > take) {
                    is.setAmount(is.getAmount() - take);
                    p.getInventory().setItem(i, is);
                } else {
                    p.getInventory().setItem(i, null);
                }
            } else if (is.getItemMeta() instanceof BlockStateMeta) {
                // shulker box support
                BlockStateMeta bm = (BlockStateMeta) is.getItemMeta();
                try {
                    org.bukkit.block.ShulkerBox box = (org.bukkit.block.ShulkerBox) bm.getBlockState();
                    ItemStack[] arr = box.getInventory().getContents();
                    boolean modified = false;
                    for (int j = 0; j < arr.length; j++) {
                        ItemStack s = arr[j];
                        if (s == null) continue;
                        if (s.getType() == o.getItem().getType()) {
                            int need = o.getAmount() - o.getProgress() - deposited;
                            if (need <= 0) break;
                            int take = Math.min(s.getAmount(), need);
                            deposited += take;
                            if (s.getAmount() > take) {
                                s.setAmount(s.getAmount() - take);
                                arr[j] = s;
                            } else {
                                arr[j] = null;
                            }
                            modified = true;
                        }
                    }
                    if (modified) {
                        box.getInventory().setContents(arr);
                        bm.setBlockState(box);
                        is.setItemMeta(bm);
                        p.getInventory().setItem(i, is);
                    }
                } catch (ClassCastException ignored) {}
            }
        }
        return deposited;
    }

    // balances
    public synchronized void addBalance(String uuid, double amount) {
        double cur = balancesConfig.getDouble(uuid, 0.0);
        balancesConfig.set(uuid, cur + amount);
        try { balancesConfig.save(balancesFile); } catch (Exception ignored) {}
    }

    public synchronized void setAwaitingAmount(org.bukkit.entity.Player p, boolean b) { setAwaitingAmount((org.bukkit.entity.Player) p, b); }

}
