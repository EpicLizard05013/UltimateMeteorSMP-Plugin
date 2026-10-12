package com.epiclizard.orders;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.Base64;

public class Order {
    private final String id;
    private final String ownerUuid; // may be null for Server
    private final String ownerName;
    private final ItemStack item;
    private int amount;
    private int progress;
    private double price;
    private int collected;
    private final long time;

    public Order(String id, String ownerUuid, String ownerName, ItemStack item, int amount, int progress, double price, int collected, long time) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName == null ? "Server" : ownerName;
        this.item = item;
        this.amount = amount;
        this.progress = progress;
        this.price = price;
        this.collected = collected;
        this.time = time;
    }

    public String getId() { return id; }
    public String getOwnerUuid() { return ownerUuid; }
    public String getOwnerName() { return ownerName; }
    public ItemStack getItem() { return item; }
    public int getAmount() { return amount; }
    public void setAmount(int a) { amount = a; }
    public int getProgress() { return progress; }
    public void addProgress(int v) { progress += v; }
    public double getPrice() { return price; }
    public void setPrice(double p) { price = p; }
    public int getCollected() { return collected; }
    public void setCollected(int c) { collected = c; }
    public long getTime() { return time; }
    public long getExpireTime() { return time + (7L * 86400L); }

    public String getItemDisplay() {
        return Utils.cleanName(item);
    }

    public void saveToConfig(ConfigurationSection section) {
        section.set("owner", ownerName);
        section.set("owner_uuid", ownerUuid);
        section.set("amount", amount);
        section.set("progress", progress);
        section.set("price", price);
        section.set("collected", collected);
        section.set("time", time);
        section.set("item", serializeItem(item));
    }

    public static Order fromConfig(String id, ConfigurationSection section) {
        try {
            String owner = section.getString("owner", "Server");
            String ownerUuid = section.getString("owner_uuid", null);
            int amount = section.getInt("amount", 1);
            int progress = section.getInt("progress", 0);
            double price = section.getDouble("price", 0.0);
            int collected = section.getInt("collected", 0);
            long time = section.getLong("time", System.currentTimeMillis() / 1000L);
            ItemStack item = deserializeItem(section.getString("item", null));
            if (item == null) return null;
            return new Order(id, ownerUuid, owner, item, amount, progress, price, collected, time);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // item serialization helpers
    public static String serializeItem(ItemStack item) {
        if (item == null) return null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos);
            boos.writeObject(item);
            boos.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ItemStack deserializeItem(String data) {
        if (data == null) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            BukkitObjectInputStream bois = new BukkitObjectInputStream(new ByteArrayInputStream(bytes));
            Object o = bois.readObject();
            bois.close();
            if (o instanceof ItemStack) return (ItemStack) o;
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
