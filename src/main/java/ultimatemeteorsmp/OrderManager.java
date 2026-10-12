package ultimatemeteorsmp;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class OrderManager {
    private final UltimateMeteorSMP plugin;
    private final Map<String, Order> activeOrders = new LinkedHashMap<>();
    private final List<String> orderList = new ArrayList<>();
    private final Map<UUID, Double> balances = new HashMap<>();
    
    // User session states
    public final Map<UUID, ItemStack> setupItem = new HashMap<>();
    public final Map<UUID, Integer> setupAmount = new HashMap<>();
    public final Map<UUID, Double> setupPrice = new HashMap<>();
    public final Map<UUID, String> sortMode = new HashMap<>();
    public final Map<UUID, String> searchQuery = new HashMap<>();
    public final Map<UUID, Boolean> awaitingAmount = new HashMap<>();
    public final Map<UUID, Boolean> awaitingPrice = new HashMap<>();
    public final Map<UUID, Boolean> awaitingSearch = new HashMap<>();
    public final Map<UUID, Integer> guiPage = new HashMap<>();
    public final Map<UUID, Integer> pickerPage = new HashMap<>();
    public final Map<UUID, Integer> collectPage = new HashMap<>();
    public final Map<UUID, String> viewTarget = new HashMap<>();
    public final Map<UUID, String> collectTarget = new HashMap<>();
    public final Map<UUID, String> fulfillTarget = new HashMap<>();
    public final Map<UUID, Map<Integer, String>> guiMap = new HashMap<>();

    private int idCounter = 0;
    private File ordersFile;
    FileConfiguration ordersConfig;

    public static final String TAG = "§8[§6MeteorSMP§8]";
    public static final long ORDER_DURATION_SECONDS = 7L * 24L * 60L * 60L;

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

    public void checkOrderExpiry(String id) {
        Order order = activeOrders.get(id);
        if (order == null || order.getPrice() <= 0) return;

        long now = System.currentTimeMillis() / 1000L;
        long expireTime = order.getTimestamp() + ORDER_DURATION_SECONDS;

        if (now >= expireTime) {
            int remaining = order.getAmount() - order.getProgress();
            if (remaining > 0 && !order.getOwner().equals("Server")) {
                double refund = remaining * order.getPrice();
                Player ownerPlayer = Bukkit.getPlayer(order.getOwner());
                UUID ownerUUID = ownerPlayer != null ? ownerPlayer.getUniqueId() : null;
                
                // If offline, attempt parse if stored as UUID string or fallback to name lookup
                if (ownerPlayer != null) {
                    addBalance(ownerPlayer.getUniqueId(), refund);
                    ownerPlayer.sendMessage(TAG + " §cYour order for §e" + order.getItem().getType().name() + " §chas expired! Refunded §e$" + refund + "§c.");
                }
            }
            order.setAmount(order.getProgress());
            order.setPrice(0.0); // Marked cancelled/expired
            if (order.getProgress() <= 0) {
                orderList.remove(id);
                activeOrders.remove(id);
            }
            saveOrders();
        }
    }

    public List<String> sortOrdersList(Player p, List<String> orders) {
        String mode = sortMode.getOrDefault(p.getUniqueId(), "newest");
        if (mode.equals("newest")) return orders;

        List<String> sorted = new ArrayList<>(orders);
        sorted.sort((id1, id2) -> {
            Order o1 = activeOrders.get(id1);
            Order o2 = activeOrders.get(id2);
            if (o1 == null || o2 == null) return 0;
            if (mode.equals("price_low")) {
                return Double.compare(o1.getPrice(), o2.getPrice());
            } else if (mode.equals("price_high")) {
                return Double.compare(o2.getPrice(), o1.getPrice());
            }
            return 0;
        });
        return sorted;
    }

    public void createOrder(String owner, ItemStack item, int amount, double price) {
        idCounter++;
        String id = "ORD_" + idCounter;
        Order order = new Order(id, owner, item, amount, price, 0, 0, System.currentTimeMillis() / 1000L);
        activeOrders.put(id, order);
        orderList.add(0, id);
        saveOrders();
    }

    public void loadOrders() {
        ordersFile = new File(plugin.getDataFolder(), "orders.yml");
        if (!ordersFile.exists()) {
            plugin.saveResource("orders.yml", false);
        }
        ordersConfig = YamlConfiguration.loadConfiguration(ordersFile);
        idCounter = ordersConfig.getInt("system.id_counter", 0);
        
        if (ordersConfig.contains("active")) {
            for (String id : ordersConfig.getConfigurationSection("active").getKeys(false)) {
                String owner = ordersConfig.getString("active." + id + ".owner");
                ItemStack item = ordersConfig.getItemStack("active." + id + ".item");
                int amount = ordersConfig.getInt("active." + id + ".amount");
                double price = ordersConfig.getDouble("active." + id + ".price");
                int progress = ordersConfig.getInt("active." + id + ".progress");
                int collected = ordersConfig.getInt("active." + id + ".collected");
                long time = ordersConfig.getLong("active." + id + ".time");
                activeOrders.put(id, new Order(id, owner, item, amount, price, progress, collected, time));
            }
        }
        if (ordersConfig.contains("list")) {
            orderList.clear();
            orderList.addAll(ordersConfig.getStringList("list"));
        }
        if (ordersConfig.contains("balances")) {
            for (String uuidStr : ordersConfig.getConfigurationSection("balances").getKeys(false)) {
                balances.put(UUID.fromString(uuidStr), ordersConfig.getDouble("balances." + uuidStr));
            }
        }
    }

    public void saveOrders() {
        try {
            ordersConfig.set("system.id_counter", idCounter);
            ordersConfig.set("active", null);
            for (Map.Entry<String, Order> entry : activeOrders.entrySet()) {
                String id = entry.getKey();
                Order o = entry.getValue();
                ordersConfig.set("active." + id + ".owner", o.getOwner());
                ordersConfig.set("active." + id + ".item", o.getItem());
                ordersConfig.set("active." + id + ".amount", o.getAmount());
                ordersConfig.set("active." + id + ".price", o.getPrice());
                ordersConfig.set("active." + id + ".progress", o.getProgress());
                ordersConfig.set("active." + id + ".collected", o.getCollected());
                ordersConfig.set("active." + id + ".time", o.getTimestamp());
            }
            ordersConfig.set("list", orderList);
            for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
                ordersConfig.set("balances." + entry.getKey().toString(), entry.getValue());
            }
            ordersConfig.save(ordersFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save orders.yml!");
        }
    }

    public Map<String, Order> getActiveOrders() { return activeOrders; }
    public List<String> getOrderList() { return orderList; }
    
    public double getBalance(UUID uuid) { return balances.getOrDefault(uuid, 0.0); }
    public void addBalance(UUID uuid, double amount) { balances.put(uuid, getBalance(uuid) + amount); saveOrders(); }
    public void removeBalance(UUID uuid, double amount) { balances.put(uuid, Math.max(0, getBalance(uuid) - amount)); saveOrders(); }
}
