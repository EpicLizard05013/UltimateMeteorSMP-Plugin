package ultimatemeteorsmp;

import org.bukkit.inventory.ItemStack;

public class Order {
    private final String id;
    private final String owner; // Player name or "Server"
    private final ItemStack item;
    private int amount;
    private double price;
    private int progress;
    private int collected;
    private final long timestamp;

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

    public void setAmount(int amount) { this.amount = amount; }
    public void setPrice(double price) { this.price = price; }
    public void setProgress(int progress) { this.progress = progress; }
    public void setCollected(int collected) { this.collected = collected; }
}
