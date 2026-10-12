package com.epiclizard.orders;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class Order {
    private final String id;
    private final String ownerUuid;
    private final String ownerName;
    private final ItemStack item;
    private int amount;
    private int progress;
    private double price;
    private final long createdAt;

    public Order(String id, String ownerUuid, String ownerName, ItemStack item, int amount, int progress, double price, long createdAt) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName == null || ownerName.isBlank() ? "Server" : ownerName;
        this.item = item;
        this.amount = amount;
        this.progress = progress;
        this.price = price;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getOwnerUuid() { return ownerUuid; }
    public String getOwnerName() { return ownerName; }
    public ItemStack getItem() { return item; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public int getProgress() { return progress; }
    public void addProgress(int delta) { this.progress += delta; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public long getCreatedAt() { return createdAt; }
    public long getExpireAt() { return createdAt + (7L * 86400L); }
    public boolean isExpired(long nowSeconds) { return nowSeconds >= getExpireAt(); }

    public String getDisplayName() {
        return Utils.cleanName(item);
    }

    public void saveToConfig(ConfigurationSection section) {
        section.set("owner", ownerName);
        section.set("owner_uuid", ownerUuid);
        section.set("amount", amount);
        section.set("progress", progress);
        section.set("price", price);
        section.set("created_at", createdAt);
        section.set("item", serializeItem(item));
    }

    public static Order fromConfig(String id, ConfigurationSection section) {
        String owner = section.getString("owner", "Server");
        String ownerUuid = section.getString("owner_uuid", null);
        int amount = section.getInt("amount", 1);
        int progress = section.getInt("progress", 0);
        double price = section.getDouble("price", 0.0);
        long createdAt = section.getLong("created_at", System.currentTimeMillis() / 1000L);
        ItemStack item = deserializeItem(section.getString("item", null));
        if (item == null) {
            return null;
        }
        return new Order(id, ownerUuid, owner, item, amount, progress, price, createdAt);
    }

    public static String serializeItem(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(output)) {
            dataOutput.writeObject(stack);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ItemStack deserializeItem(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try (ByteArrayInputStream input = new ByteArrayInputStream(Base64.getDecoder().decode(encoded));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(input)) {
            Object obj = dataInput.readObject();
            if (obj instanceof ItemStack item) {
                return item;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
