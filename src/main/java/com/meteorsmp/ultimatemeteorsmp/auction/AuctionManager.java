package com.meteorsmp.ultimatemeteorsmp.auction;

import com.meteorsmp.ultimatemeteorsmp.UltimateMeteorSMP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
    private final UltimateMeteorSMP plugin;
    private final Map<Integer, AuctionItem> listings = new ConcurrentHashMap<>();
    private final Map<UUID, List<Transaction>> transactions = new ConcurrentHashMap<>();
    private int counter = 0;

    public AuctionManager(UltimateMeteorSMP plugin) {
        this.plugin = plugin;
    }

    public int createListing(UUID sellerUuid, String sellerName, ItemStack item, double price) {
        counter++;
        String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? 
                item.getItemMeta().getDisplayName() : item.getType().name().toLowerCase().replace('_', ' ');
        
        AuctionItem auctionItem = new AuctionItem(counter, sellerUuid, sellerName, item, price, System.currentTimeMillis(), ChatColor.stripColor(itemName));
        listings.put(counter, auctionItem);
        return counter;
    }

    public AuctionItem getListing(int id) {
        return listings.get(id);
    }

    public void removeListing(int id) {
        listings.remove(id);
    }

    public Collection<AuctionItem> getAllListings() {
        return listings.values();
    }

    public void logTransaction(UUID uuid, String itemName, String buyer, String seller, double price) {
        transactions.computeIfAbsent(uuid, k -> new ArrayList<>()).add(new Transaction(itemName, buyer, seller, price, System.currentTimeMillis()));
        int maxCap = plugin.getConfig().getInt("auction.max-transaction-history", 100);
        List<Transaction> userTrans = transactions.get(uuid);
        while (userTrans.size() > maxCap) {
            userTrans.remove(0);
        }
    }

    public List<Transaction> getTransactions(UUID uuid) {
        return transactions.getOrDefault(uuid, Collections.emptyList());
    }

    public static double parsePrice(String text) {
        if (text == null) return -1;
        text = text.toLowerCase().trim();
        double multiplier = 1.0;
        
        if (text.endsWith("qt")) {
            multiplier = 1_000_000_000_000_000_000.0;
            text = text.replace("qt", "");
        } else if (text.endsWith("qd")) {
            multiplier = 1_000_000_000_000_000.0;
            text = text.replace("qd", "");
        } else if (text.endsWith("t")) {
            multiplier = 1_000_000_000_000.0;
            text = text.replace("t", "");
        } else if (text.endsWith("b")) {
            multiplier = 1_000_000_000.0;
            text = text.replace("b", "");
        } else if (text.endsWith("m")) {
            multiplier = 1_000_000.0;
            text = text.replace("m", "");
        } else if (text.endsWith("k")) {
            multiplier = 1_000.0;
            text = text.replace("k", "");
        }
        
        try {
            double val = Double.parseDouble(text);
            return val * multiplier;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String formatPrice(double n) {
        if (n >= 1_000_000_000_000_000_000.0) return (n / 1_000_000_000_000_000_000.0) + "qt";
        if (n >= 1_000_000_000_000_000.0) return (n / 1_000_000_000_000_000.0) + "qd";
        if (n >= 1_000_000_000_000.0) return (n / 1_000_000_000_000.0) + "t";
        if (n >= 1_000_000_000.0) return (n / 1_000_000_000.0) + "b";
        if (n >= 1_000_000.0) return (n / 1_000_000.0) + "m";
        if (n >= 1_000.0) return (n / 1_000.0) + "k";
        return String.format("%.0f", n);
    }

    public static class Transaction {
        private final String itemName;
        private final String buyer;
        private final String seller;
        private final double price;
        private final long time;

        public Transaction(String itemName, String buyer, String seller, double price, long time) {
            this.itemName = itemName;
            this.buyer = buyer;
            this.seller = seller;
            this.price = price;
            this.time = time;
        }

        public String getItemName() { return itemName; }
        public String getBuyer() { return buyer; }
        public String getSeller() { return seller; }
        public double getPrice() { return price; }
        public long getTime() { return time; }
    }
}
