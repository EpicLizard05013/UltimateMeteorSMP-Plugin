package com.meteorsmp.ultimatemeteorsmp.auction;

import org.bukkit.inventory.ItemStack;
import java.util.UUID;

public class AuctionItem {
    private final int id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack item;
    private final double price;
    private final long timestamp;
    private final String itemName;

    public AuctionItem(int id, UUID sellerUuid, String sellerName, ItemStack item, double price, long timestamp, String itemName) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.item = item;
        this.price = price;
        this.timestamp = timestamp;
        this.itemName = itemName;
    }

    public int getId() { return id; }
    public UUID getSellerUuid() { return sellerUuid; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItem() { return item; }
    public double getPrice() { return price; }
    public long getTimestamp() { return timestamp; }
    public String getItemName() { return itemName; }
}
