package com.meteorsmp.ultimatemeteorsmp;

import org.bukkit.inventory.ItemStack;

public class Order {
    private final String id;
    private final String owner;
    private final ItemStack item;
    private int amount;
    private double price;
    private int progress;
    private int collected;
    private final long timestamp;
    private boolean cancelled = false;

    public Order(String id, String owner, ItemStack item, int amount, double price, int progress, int collected, long timestamp) {
        this.id = id;
        this.owner = owner;
        this.item = item;
        this.amount = amount;
        this.price = price;
        this.progress = progress;
        this.collected = collected;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getOwner() { return owner; }
    public ItemStack getItem() { return item; }
    public int getAmount() { return amount; }
    public double getPrice() { return price; }
    public int getProgress() { return progress; }
    public int getCollected() { return collected; }
    public long getTimestamp() { return timestamp; }
    public boolean isCancelled() { return cancelled; }

    public void setProgress(int progress) { this.progress = progress; }
    public void setCollected(int collected) { this.collected = collected; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
