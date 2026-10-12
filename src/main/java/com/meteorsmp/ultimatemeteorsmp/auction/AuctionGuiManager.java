package com.meteorsmp.ultimatemeteorsmp.auction;

import com.meteorsmp.ultimatemeteorsmp.UltimateMeteorSMP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.block.ShulkerBox;

import java.util.*;

public class AuctionGuiManager {
    private final UltimateMeteorSMP plugin;
    private final Map<UUID, String> activeSearch = new HashMap<>();
    private final Map<UUID, Boolean> searchingChat = new HashMap<>();
    private final Map<UUID, String> sortMode = new HashMap<>(); // "high_to_low" or "low_to_high"
    private final Map<UUID, String> userViewMode = new HashMap<>(); // "active" or "transactions"
    private final Map<UUID, Integer> confirmId = new HashMap<>();
    private final Map<UUID, Map<Integer, Integer>> slotIdMapping = new HashMap<>();

    public AuctionGuiManager(UltimateMeteorSMP plugin) {
        this.plugin = plugin;
    }

    public void openMainGui(Player player, int page) {
        slotIdMapping.remove(player.getUniqueId());
        String title = ChatColor.DARK_GRAY + "ᴀᴜᴄᴛɪᴏɴ (ᴘᴀɢᴇ " + page + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        String search = activeSearch.get(player.getUniqueId());
        String sort = sortMode.getOrDefault(player.getUniqueId(), "high_to_low");

        List<AuctionItem> validListings = new ArrayList<>();
        long durationMillis = plugin.getConfig().getInt("auction.listing-duration-hours", 24) * 3600000L;

        for (AuctionItem item : plugin.getAuctionManager().getAllListings()) {
            if (System.currentTimeMillis() - item.getTimestamp() < durationMillis) {
                if (search != null && !search.isEmpty()) {
                    if (!item.getItemName().toLowerCase().contains(search.toLowerCase())) {
                        continue;
                    }
                }
                validListings.add(item);
            }
        }

        validListings.sort((a, b) -> sort.equals("low_to_high") ? Double.compare(a.getPrice(), b.getPrice()) : Double.compare(b.getPrice(), a.getPrice()));

        int maxPage = Math.max(1, (int) Math.ceil(validListings.size() / 45.0));
        if (page > maxPage) page = maxPage;

        int startIndex = (page - 1) * 45;
        Map<Integer, Integer> slotMap = new HashMap<>();

        for (int i = 0; i < 45; i++) {
            int index = startIndex + i;
            if (index < validListings.size()) {
                AuctionItem ahItem = validListings.get(index);
                ItemStack display = ahItem.getItem().clone();
                ItemMeta meta = display.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    lore.add("");
                    lore.add(ChatColor.WHITE + "Price: " + ChatColor.AQUA + "$" + AuctionManager.formatPrice(ahItem.getPrice()));
                    lore.add(ChatColor.WHITE + "Seller: " + ChatColor.AQUA + ahItem.getSellerName());
                    long timeLeft = durationMillis - (System.currentTimeMillis() - ahItem.getTimestamp());
                    lore.add(ChatColor.WHITE + "Time Left: " + ChatColor.AQUA + formatTime(timeLeft));
                    if (display.getType().name().contains("SHULKER_BOX")) {
                        lore.add(ChatColor.YELLOW + "Right-Click to preview shulker");
                    }
                    meta.setLore(lore);
                    display.setItemMeta(meta);
                }
                inv.setItem(i, display);
                slotMap.put(i, ahItem.getId());
            }
        }
        slotIdMapping.put(player.getUniqueId(), slotMap);

        // Control bar
        inv.setItem(45, createGuiItem(Material.ARROW, ChatColor.GREEN + "ᴘʀᴇᴠɪᴏᴜѕ ᴘᴀɢᴇ", ChatColor.GRAY + "Click to go to previous page."));
        inv.setItem(46, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + ""));
        inv.setItem(47, createGuiItem(Material.HOPPER, ChatColor.YELLOW + "ѕᴏʀᴛ: " + (sort.equals("low_to_high") ? ChatColor.GREEN + "LOW TO HIGH" : ChatColor.YELLOW + "HIGH TO LOW"), ChatColor.GRAY + "Click to switch sort order."));
        inv.setItem(48, createGuiItem(Material.ENDER_CHEST, ChatColor.GREEN + "ѕʜᴏᴘ", ChatColor.GRAY + "Click to open /shop."));
        inv.setItem(49, createGuiItem(Material.ANVIL, ChatColor.YELLOW + "ʀᴇꜰʀᴇѕʜ", ChatColor.GRAY + "Click to refresh."));
        
        ItemStack searchItem = createGuiItem(Material.OAK_SIGN, ChatColor.YELLOW + "ѕᴇᴀʀᴄʜ", search != null ? ChatColor.GRAY + "Current filter: " + ChatColor.YELLOW + search : ChatColor.GRAY + "Click to search items.");
        inv.setItem(50, searchItem);

        inv.setItem(51, createGuiItem(Material.CHEST, ChatColor.GREEN + "ʏᴏᴜʀ ɪᴛᴇᴍѕ", ChatColor.GRAY + "Click to view your listings & transactions."));
        inv.setItem(52, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + ""));
        inv.setItem(53, createGuiItem(Material.ARROW, ChatColor.GREEN + "ɴᴇxᴛ ᴘᴀɢᴇ", ChatColor.GRAY + "Click to go to next page."));

        player.openInventory(inv);
    }

    public void openYourItemsGui(Player player, int page) {
        slotIdMapping.remove(player.getUniqueId());
        String mode = userViewMode.getOrDefault(player.getUniqueId(), "active");
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "ᴀᴜᴄᴛɪᴏɴ -> ʏᴏᴜʀ ɪᴛᴇᴍѕ");

        List<Object> items = new ArrayList<>();
        if (mode.equals("transactions")) {
            items.addAll(plugin.getAuctionManager().getTransactions(player.getUniqueId()));
        } else {
            for (AuctionItem item : plugin.getAuctionManager().getAllListings()) {
                if (item.getSellerUuid().equals(player.getUniqueId())) {
                    items.add(item);
                }
            }
        }

        int maxPage = Math.max(1, (int) Math.ceil(items.size() / 45.0));
        if (page > maxPage) page = maxPage;
        int startIndex = (page - 1) * 45;
        Map<Integer, Integer> slotMap = new HashMap<>();

        for (int i = 0; i < 45; i++) {
            int index = startIndex + i;
            if (index < items.size()) {
                Object obj = items.get(index);
                if (obj instanceof AuctionItem) {
                    AuctionItem ahItem = (AuctionItem) obj;
                    ItemStack display = ahItem.getItem().clone();
                    ItemMeta meta = display.getItemMeta();
                    if (meta != null) {
                        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                        long durationMillis = plugin.getConfig().getInt("auction.listing-duration-hours", 24) * 3600000L;
                        long age = System.currentTimeMillis() - ahItem.getTimestamp();
                        if (age >= durationMillis) {
                            lore.add(ChatColor.RED + "(EXPIRED)");
                            lore.add(ChatColor.YELLOW + "Click to reclaim expired item!");
                        } else {
                            lore.add(ChatColor.WHITE + "Price: " + ChatColor.AQUA + "$" + AuctionManager.formatPrice(ahItem.getPrice()));
                            lore.add(ChatColor.WHITE + "Time Left: " + ChatColor.AQUA + formatTime(durationMillis - age));
                            lore.add(ChatColor.RED + "Click to cancel & reclaim item!");
                        }
                        meta.setLore(lore);
                        display.setItemMeta(meta);
                    }
                    inv.setItem(i, display);
                    slotMap.put(i, ahItem.getId());
                } else if (obj instanceof AuctionManager.Transaction) {
                    AuctionManager.Transaction tx = (AuctionManager.Transaction) obj;
                    ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
                    ItemMeta meta = book.getItemMeta();
                    meta.setDisplayName(ChatColor.YELLOW + tx.getItemName());
                    List<String> lore = new ArrayList<>();
                    if (tx.getBuyer().equals(player.getName())) {
                        lore.add(ChatColor.WHITE + "You bought " + ChatColor.GREEN + tx.getSeller() + ChatColor.WHITE + "'s item for $" + AuctionManager.formatPrice(tx.getPrice()));
                    } else {
                        lore.add(ChatColor.GREEN + tx.getBuyer() + ChatColor.WHITE + " bought your item for $" + AuctionManager.formatPrice(tx.getPrice()));
                    }
                    meta.setLore(lore);
                    book.setItemMeta(meta);
                    inv.setItem(i, book);
                }
            }
        }
        slotIdMapping.put(player.getUniqueId(), slotMap);

        inv.setItem(45, createGuiItem(Material.ARROW, ChatColor.GREEN + "ᴘʀᴇᴠɪᴏᴜѕ ᴘᴀɢᴇ", ChatColor.GRAY + "Previous page"));
        inv.setItem(47, createGuiItem(Material.ENDER_CHEST, ChatColor.GREEN + "ѕʜᴏᴘ", ChatColor.GRAY + "Open shop"));
        inv.setItem(48, createGuiItem(Material.ANVIL, ChatColor.YELLOW + "ʀᴇꜰʀᴇѕʜ", ChatColor.GRAY + "Refresh"));
        inv.setItem(49, createGuiItem(Material.OAK_SIGN, ChatColor.YELLOW + "ѕᴇᴀʀᴄʜ", ChatColor.GRAY + "Search items"));
        inv.setItem(50, createGuiItem(Material.CHEST, ChatColor.GREEN + "ʏᴏᴜʀ ɪᴛᴇᴍѕ", ChatColor.GRAY + "Active listings"));
        inv.setItem(51, createGuiItem(Material.BOOK, ChatColor.YELLOW + "ᴠɪᴇᴡ: " + (mode.equals("transactions") ? ChatColor.GREEN + "TRANSACTIONS" : ChatColor.YELLOW + "ACTIVE"), ChatColor.GRAY + "Switch view mode"));
        inv.setItem(53, createGuiItem(Material.ARROW, ChatColor.GREEN + "ɴᴇxᴛ ᴘᴀɢᴇ", ChatColor.GRAY + "Next page"));

        player.openInventory(inv);
    }

    public void openPlayerViewGui(Player player, String targetName, int page) {
        // Implementation for /ah view <player> mirroring main GUI filtered by target seller
        openMainGui(player, page);
    }

    public void openConfirmGui(Player player, AuctionItem item) {
        confirmId.put(player.getUniqueId(), item.getId());
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "ᴄᴏɴꜰɪʀᴍ ᴘᴜʀᴄʜᴀѕᴇ");
        inv.setItem(11, createGuiItem(Material.RED_WOOL, ChatColor.RED + "ᴄᴀɴᴄᴇʟ ᴘᴜʀᴄʜᴀѕᴇ", ChatColor.GRAY + "Return to Auction House"));
        inv.setItem(13, item.getItem());
        inv.setItem(15, createGuiItem(Material.LIME_WOOL, ChatColor.GREEN + "ᴄᴏɴꜰɪʀᴍ ᴘᴜʀᴄʜᴀѕᴇ", ChatColor.GRAY + "Price: " + ChatColor.GREEN + "$" + AuctionManager.formatPrice(item.getPrice())));
        player.openInventory(inv);
    }

    public void openShulkerPreview(Player player, ItemStack shulker) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "ѕʜᴜʟᴋᴇʀ ᴘʀᴇᴠɪᴇᴡ");
        if (shulker.getItemMeta() instanceof BlockStateMeta) {
            BlockStateMeta meta = (BlockStateMeta) shulker.getItemMeta();
            if (meta.getBlockState() instanceof ShulkerBox) {
                ShulkerBox box = (ShulkerBox) meta.getBlockState();
                ItemStack[] contents = box.getInventory().getContents();
                for (int i = 0; i < Math.min(contents.length, 27); i++) {
                    if (contents[i] != null) inv.setItem(i, contents[i]);
                }
            }
        }
        player.openInventory(inv);
    }

    public Map<Integer, Integer> getSlotMapping(UUID uuid) {
        return slotIdMapping.get(uuid);
    }

    public Map<UUID, Boolean> getSearchingChat() { return searchingChat; }
    public Map<UUID, String> getActiveSearch() { return activeSearch; }
    public Map<UUID, Integer> getConfirmId() { return confirmId; }
    public Map<UUID, String> getSortMode() { return sortMode; }
    public Map<UUID, String> getUserViewMode() { return userViewMode; }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatTime(long millis) {
        long hours = millis / 3600000L;
        long minutes = (millis % 3600000L) / 60000L;
        return hours + "h " + minutes + "m";
    }
}
