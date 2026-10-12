package com.meteorsmp.ultimatemeteorsmp.auction;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AuctionItem {
    private final int id;
    private final ItemStack item;
    private final UUID sellerUuid;
    private final String sellerName;
    private final long price;
    private final long timeListed;
    private boolean sold;

    public AuctionItem(int id, ItemStack item, UUID sellerUuid, String sellerName, long price, long timeListed) {
        this.id = id;
        this.item = item;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.price = price;
        this.timeListed = timeListed;
        this.sold = false;
    }

    public int getId() { return id; }
    public ItemStack getItem() { return item; }
    public UUID getSellerUuid() { return sellerUuid; }
    public String getSellerName() { return sellerName; }
    public long getPrice() { return price; }
    public long getTimeListed() { return timeListed; }
    public boolean isSold() { return sold; }
    public void setSold(boolean sold) { this.sold = sold; }
}
