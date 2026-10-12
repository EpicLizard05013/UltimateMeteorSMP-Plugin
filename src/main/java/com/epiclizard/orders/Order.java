package com.epiclizard.orders;

import org.bukkit.inventory.ItemStack;

public class Order {
    private final String id;
    private final String owner; // player UUID string or "Server"
    private final ItemStack item;
    private int amount;
    private double price;
    private int progress;
    private int collected;
    private final long timestamp;

    public Order(String id, String owner, ItemStack item, int amount, double price, int progress, int collected, long timestamp) {
        this.id = id;
        this.owner = owner;
        this.item = item.clone();
        this.amount = Math.max(1, amount);
        this.price = Math.max(0.0, price);
        this.progress = Math.max(0, progress);
        this.collected = Math.max(0, collected);
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public ItemStack getItem() {
        return item.clone();
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = Math.max(0, amount);
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = Math.max(0.0, price);
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, progress);
    }

    public int getCollected() {
        return collected;
    }

    public void setCollected(int collected) {
        this.collected = Math.max(0, collected);
    }

    public long getTimestamp() {
        return timestamp;
    }
}
