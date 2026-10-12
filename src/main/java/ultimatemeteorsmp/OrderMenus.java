package ultimatemeteorsmp;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class OrderMenus {

    public static void openDebugGUI(Player p) {
        Inventory gui = Bukkit.createInventory(null, 27, "ǫʀᴅᴇʀѕ -> ᴅᴇʙᴜɢ ᴍᴇɴᴜ");
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta pm = pane.getItemMeta();
        pm.setDisplayName(" ");
        pane.setItemMeta(pm);
        for (int i = 0; i < 27; i++) gui.setItem(i, pane);

        ItemStack tnt = new ItemStack(Material.TNT);
        ItemMeta tm = tnt.getItemMeta();
        tm.setDisplayName("§c§lCLEAR ALL ACTIVE ORDERS");
        tm.setLore(Arrays.asList("§7Click to wipe all orders from database"));
        tnt.setItemMeta(tm);
        gui.setItem(11, tnt);

        ItemStack star = new ItemStack(Material.NETHER_STAR);
        ItemMeta sm = star.getItemMeta();
        sm.setDisplayName("§a§lSPAWN RANDOM DEBUG ORDER");
        sm.setLore(Arrays.asList("§7Click to create a random order ($2-$300)"));
        star.setItemMeta(sm);
        gui.setItem(13, star);

        ItemStack gold = new ItemStack(Material.GOLD_INGOT);
        ItemMeta gm = gold.getItemMeta();
        gm.setDisplayName("§e§lGIVE $10,000 TEST BALANCE");
        gm.setLore(Arrays.asList("§7Click to add $10,000 to your balance"));
        gold.setItemMeta(gm);
        gui.setItem(15, gold);

        p.openInventory(gui);
    }

    public static void openOrdersMenu(Player p, int page, OrderManager manager) {
        Inventory gui = Bukkit.createInventory(null, 54, "ǫʀᴅᴇʀѕ (ᴘᴀɢᴇ " + page + ")");
        manager.guiMap.put(p.getUniqueId(), new HashMap<>());

        String query = manager.searchQuery.get(p.getUniqueId());
        String queryLower = query != null ? query.toLowerCase() : null;

        int startIndex = (page - 1) * 45;
        int slot = 0;
        int counter = 0;

        List<String> rawOrders = new ArrayList<>();
        for (String id : manager.getOrderList()) {
            manager.checkOrderExpiry(id);
            Order order = manager.getActiveOrders().get(id);
            if (order != null && order.getPrice() > 0) {
                if (queryLower != null && !order.getItem().getType().name().toLowerCase().contains(queryLower)) {
                    continue;
                }
                if (order.getProgress() >= order.getAmount()) continue;
                rawOrders.add(id);
            }
        }

        List<String> activeOrders = manager.sortOrdersList(p, rawOrders);
        boolean hasNext = false;

        for (String id : activeOrders) {
            if (counter >= startIndex) {
                if (slot < 45) {
                    Order order = manager.getActiveOrders().get(id);
                    long diff = (order.getTimestamp() + OrderManager.ORDER_DURATION_SECONDS) - (System.currentTimeMillis() / 1000L);
                    if (diff < 0) diff = 0;
                    long daysLeft = diff / 86400;
                    long hoursLeft = (diff % 86400) / 3600;

                    ItemStack display = order.getItem().clone();
                    ItemMeta meta = display.getItemMeta();
                    meta.setDisplayName("§f" + order.getItem().getType().name());
                    meta.setLore(Arrays.asList(
                            "§a" + order.getAmount() + " requested",
                            "§7Created by: §e" + order.getOwner(),
                            " ",
                            "§a$" + order.getPrice() + " each",
                            "§a" + order.getProgress() + "/" + order.getAmount() + " ᴅᴇʟɪᴠᴇʀᴇᴅ",
                            " ",
                            "§7" + daysLeft + "ᴅ " + hoursLeft + "ʜ ᴜɴᴛɪʟ ǫʀᴅᴇʀ ᴇxᴘɪʀᴇѕ"
                    ));
                    display.setItemMeta(meta);

                    gui.setItem(slot, display);
                    manager.guiMap.get(p.getUniqueId()).put(slot, id);
                    slot++;
                } else {
                    hasNext = true;
                }
            }
            counter++;
        }

        String sortMode = manager.sortMode.getOrDefault(p.getUniqueId(), "newest");
        String sortDisplay = sortMode.equals("price_low") ? "Price: Low to High" : sortMode.equals("price_high") ? "Price: High to Low" : "Newest First";

        ItemStack hopper = new ItemStack(Material.HOPPER);
        ItemMeta hm = hopper.getItemMeta();
        hm.setDisplayName("§eѕᴏʀᴛ ǫʀᴅᴇʀѕ");
        hm.setLore(Arrays.asList("§7Currently: §e" + sortDisplay, "§8Click to cycle sorting"));
        hopper.setItemMeta(hm);
        gui.setItem(47, hopper);

        ItemStack shop = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta sm = shop.getItemMeta();
        sm.setDisplayName("§dѕʜᴏᴘ");
        sm.setLore(Arrays.asList("§7Click to open main shop"));
        shop.setItemMeta(sm);
        gui.setItem(48, shop);

        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta bm = book.getItemMeta();
        bm.setDisplayName("§aᴄʀᴇᴀᴛᴇ ǫʀᴅᴇʀ");
        bm.setLore(Arrays.asList("§7Click to create a new order"));
        book.setItemMeta(bm);
        gui.setItem(49, book);

        ItemStack sign = new ItemStack(Material.OAK_SIGN);
        ItemMeta sim = sign.getItemMeta();
        sim.setDisplayName("§eѕᴇᴀʀᴄʜ ǫʀᴅᴇʀѕ");
        if (query != null) {
            sim.setLore(Arrays.asList("§7Active Filter: §e" + query, "§cClick to clear search"));
        } else {
            sim.setLore(Arrays.asList("§7Click to filter by chat"));
        }
        sign.setItemMeta(sim);
        gui.setItem(50, sign);

        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta cm = chest.getItemMeta();
        cm.setDisplayName("§6ʏᴏᴜʀ ǫʀᴅᴇʀѕ");
        cm.setLore(Arrays.asList("§7View and manage your active orders"));
        chest.setItemMeta(cm);
        gui.setItem(51, chest);

        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pm = prev.getItemMeta();
            pm.setDisplayName("§fᴘʀᴇᴠɪᴏѕ ᴘᴀɢᴇ");
            prev.setItemMeta(pm);
            gui.setItem(45, prev);
        }
        if (hasNext) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nm = next.getItemMeta();
            nm.setDisplayName("§fɴᴇxt ᴘᴀɢᴇ");
            next.setItemMeta(nm);
            gui.setItem(53, next);
        }

        manager.guiPage.put(p.getUniqueId(), page);
        p.openInventory(gui);
    }

    public static void openYourOrdersMenu(Player p, OrderManager manager) {
        Inventory gui = Bukkit.createInventory(null, 54, "ʏᴏᴜʀ ǫʀᴅᴇʀѕ");
        manager.guiMap.put(p.getUniqueId(), new HashMap<>());

        int slot = 0;
        for (String id : manager.getOrderList()) {
            manager.checkOrderExpiry(id);
            Order order = manager.getActiveOrders().get(id);
            if (order != null && order.getOwner().equals(p.getName())) {
                if (slot < 45) {
                    long diff = (order.getTimestamp() + OrderManager.ORDER_DURATION_SECONDS) - (System.currentTimeMillis() / 1000L);
                    if (diff < 0) diff = 0;
                    long daysLeft = diff / 86400;
                    long hoursLeft = (diff % 86400) / 3600;

                    ItemStack display = order.getItem().clone();
                    ItemMeta meta = display.getItemMeta();
                    meta.setDisplayName("§f" + order.getItem().getType().name());
                    List<String> lore = new ArrayList<>();
                    lore.add("§a" + order.getAmount() + " requested");
                    lore.add(" ");
                    if (order.getPrice() > 0) {
                        lore.add("§a$" + order.getPrice() + " each");
                        lore.add("§a" + order.getProgress() + "/" + order.getAmount() + " ᴅᴇʟɪᴠᴇʀᴇᴅ");
                        lore.add(" ");
                        lore.add("§7" + daysLeft + "ᴅ " + hoursLeft + "ʜ ᴄɴᴛɪʟ ǫʀᴅᴇʀ ᴇxᴘɪʀᴇѕ");
                    } else {
                        lore.add("§cᴄᴀɴᴄᴇʟʟᴇᴅ");
                        lore.add("§a" + order.getProgress() + "/" + order.getAmount() + " ᴅᴇʟɪᴠᴇʀᴇᴅ");
                    }
                    meta.setLore(lore);
                    display.setItemMeta(meta);

                    gui.setItem(slot, display);
                    manager.guiMap.get(p.getUniqueId()).put(slot, id);
                    slot++;
                }
            }
        }

        gui.setItem(47, new ItemStack(Material.HOPPER));
        gui.setItem(48, new ItemStack(Material.AMETHYST_SHARD));
        gui.setItem(49, new ItemStack(Material.BOOK));
        gui.setItem(50, new ItemStack(Material.OAK_SIGN));
        gui.setItem(51, new ItemStack(Material.CHEST));
        
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName("§fʙᴀᴄᴋ ᴛᴏ ǫʀᴅᴇʀѕ");
        back.setItemMeta(bm);
        gui.setItem(45, back);

        p.openInventory(gui);
    }

    public static void openOrderManagementGUI(Player p, String id, OrderManager manager) {
        Inventory gui = Bukkit.createInventory(null, 27, "ǫʀᴅᴇʀѕ -> ᴇᴅɪᴛ ǫʀᴅᴇʀ");
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta pm = pane.getItemMeta();
        pm.setDisplayName(" ");
        pane.setItemMeta(pm);
        for (int i = 0; i < 27; i++) gui.setItem(i, pane);

        Order order = manager.getActiveOrders().get(id);
        if (order == null) return;

        int uncollected = order.getProgress() - order.getCollected();
        ItemStack display = order.getItem().clone();
        ItemMeta meta = display.getItemMeta();
        meta.setDisplayName("§f" + order.getItem().getType().name());
        List<String> lore = new ArrayList<>();
        lore.add("§a" + order.getAmount() + " requested");
        lore.add(" ");
        if (order.getPrice() > 0) {
            lore.add("§a$" + order.getPrice() + " each");
        } else {
            lore.add("§cᴄᴀɴᴄᴇʟʟᴇᴅ");
        }
        lore.add("§a" + order.getProgress() + "/" + order.getAmount() + " ᴅᴇʟɪᴠᴇʀᴇᴅ");
        lore.add("§7Uncollected: §e" + uncollected);
        meta.setLore(lore);
        display.setItemMeta(meta);
        gui.setItem(10, display);

        if (order.getPrice() > 0) {
            ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
            ItemMeta cm = cancel.getItemMeta();
            cm.setDisplayName("§c§lᴄᴀɴᴄᴇʟ ǫʀᴅᴇʀ");
            cm.setLore(Arrays.asList("§7Click to cancel this order", "§7and refund remaining costs."));
            cancel.setItemMeta(cm);
            gui.setItem(13, cancel);
        } else {
            ItemStack cancelled = new ItemStack(Material.RED_CONCRETE);
            ItemMeta cm = cancelled.getItemMeta();
            cm.setDisplayName("§c§lᴀʟʀᴇᴀᴅʏ ᴄᴀɴᴄᴇʟʟᴇᴅ");
            cancelled.setItemMeta(cm);
            gui.setItem(13, cancelled);
        }

        ItemStack collect = new ItemStack(Material.CHEST);
        ItemMeta com = collect.getItemMeta();
        com.setDisplayName("§a§lᴄᴏʟʟᴇᴄᴛ ɪᴛᴇᴍѕ");
        com.setLore(Arrays.asList("§7Click to view delivered items"));
        collect.setItemMeta(com);
        gui.setItem(15, collect);

        manager.viewTarget.put(p.getUniqueId(), id);
        p.openInventory(gui);
    }

    public static void openCollectGUI(Player p, String id, int page, OrderManager manager) {
        Inventory gui = Bukkit.createInventory(null, 54, "ǫʀᴅᴇʀѕ -> ᴄᴏʟʟᴇᴄᴛ ɪᴛᴇᴍѕ");
        Order order = manager.getActiveOrders().get(id);
        if (order == null) return;

        int uncollected = order.getProgress() - order.getCollected();
        int remaining = uncollected;
        int startIndex = (page - 1) * 45 * 64;
        if (remaining > startIndex) remaining -= startIndex;
        else remaining = 0;

        int slot = 0;
        boolean hasNext = false;
        while (remaining > 0) {
            if (slot >= 45) {
                hasNext = true;
                break;
            }
            ItemStack stack = order.getItem().clone();
            if (remaining >= 64) {
                stack.setAmount(64);
                remaining -= 64;
            } else {
                stack.setAmount(remaining);
                remaining = 0;
            }
            gui.setItem(slot, stack);
            slot++;
        }

        if (page > 1) gui.setItem(45, new ItemStack(Material.ARROW));
        if (hasNext) gui.setItem(53, new ItemStack(Material.ARROW));

        ItemStack collectAll = new ItemStack(Material.EMERALD);
        ItemMeta cam = collectAll.getItemMeta();
        cam.setDisplayName("§a§lᴄᴏʟʟᴇᴄᴛ ᴀʟʟ");
        cam.setLore(Arrays.asList("§7Click to collect all items into inventory"));
        collectAll.setItemMeta(cam);
        gui.setItem(51, collectAll);

        ItemStack dropAll = new ItemStack(Material.DROPPER);
        ItemMeta dam = dropAll.getItemMeta();
        dam.setDisplayName("§e§lᴅʀᴏᴘ ᴀʟʟ ɪᴛᴇᴍѕ");
        dam.setLore(Arrays.asList("§7Click to drop items 1 meter in front of you"));
        dropAll.setItemMeta(dam);
        gui.setItem(52, dropAll);

        manager.collectTarget.put(p.getUniqueId(), id);
        manager.collectPage.put(p.getUniqueId(), page);
        p.openInventory(gui);
    }

    public static void openFulfillGUI(Player p, String id, OrderManager manager) {
        Inventory gui = Bukkit.createInventory(null, 54, "ǫʀᴅᴇʀѕ -> ᴅᴇʟɪᴠᴇʀ ɪᴛᴇᴍѕ");
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }

        Order order = manager.getActiveOrders().get(id);
        if (order == null) return;
        int needed = order.getAmount() - order.getProgress();

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§eꜰᴄʟꜰɪʟʟɪɴɢ: §a" + order.getItem().getType().name());
        im.setLore(Arrays.asList("§7Needed: §f" + needed, "§7Reward: §a$" + order.getPrice() + " §7per unit"));
        info.setItemMeta(im);
        gui.setItem(49, info);

        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta cm = confirm.getItemMeta();
        cm.setDisplayName("§a§lᴄᴏɴꜛɪʀᴍ ꜛʟꜛɪʟʟ");
        cm.setLore(Arrays.asList("§7Click to deposit items & get paid!", "§7(Supports Shulker Boxes!)"));
        confirm.setItemMeta(cm);
        gui.setItem(53, confirm);

        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cam = cancel.getItemMeta();
        cam.setDisplayName("§c§lᴄᴀɴᴄᴇʟ");
        cam.setLore(Arrays.asList("§7Return items and go back"));
        cancel.setItemMeta(cam);
        gui.setItem(45, cancel);

        manager.fulfillTarget.put(p.getUniqueId(), id);
        p.openInventory(gui);
    }

    public static void openItemPickerGUI(Player p, int page, OrderManager manager) {
        Inventory gui = Bukkit.createInventory(null, 54, "ǫʀᴅᴇʀѕ -> ѕᴇʟᴇᴄᴛ ɪᴛᴇᴍ");
        List<Material> allTypes = Arrays.asList(Material.values());
        
        int startIndex = (page - 1) * 45;
        int slot = 0;
        boolean hasNext = false;

        for (int i = startIndex; i < allTypes.size(); i++) {
            Material mat = allTypes.get(i);
            ItemStack stack = new ItemStack(mat);
            if (!manager.isIllegalItem(stack)) {
                if (slot < 45) {
                    gui.setItem(slot, stack);
                    slot++;
                } else {
                    hasNext = true;
                    break;
                }
            }
        }

        gui.setItem(48, new ItemStack(Material.CAULDRON));
        gui.setItem(49, new ItemStack(Material.HOPPER));
        gui.setItem(50, new ItemStack(Material.OAK_SIGN));

        if (page > 1) gui.setItem(45, new ItemStack(Material.ARROW));
        if (hasNext) gui.setItem(53, new ItemStack(Material.ARROW));

        manager.pickerPage.put(p.getUniqueId(), page);
        p.openInventory(gui);
    }

    public static void openNewOrderGUI(Player p, OrderManager manager) {
        Inventory gui = Bukkit.createInventory(null, 27, "ǫʀᴅᴇʀѕ -> ɴᴇᴡ ǫʀᴅᴇʀ");
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta pm = pane.getItemMeta();
        pm.setDisplayName(" ");
        pane.setItemMeta(pm);
        for (int i = 0; i < 27; i++) gui.setItem(i, pane);

        ItemStack item = manager.setupItem.getOrDefault(p.getUniqueId(), new ItemStack(Material.STONE));
        int amount = manager.setupAmount.getOrDefault(p.getUniqueId(), 1);
        double price = manager.setupPrice.getOrDefault(p.getUniqueId(), 10.0);
        double total = amount * price;

        ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cm = cancel.getItemMeta();
        cm.setDisplayName("§c§lᴄᴀɴᴄᴇʟ ᴏʀᴅᴇʀ");
        cm.setLore(Arrays.asList("§7Click to cancel and go back"));
        cancel.setItemMeta(cm);
        gui.setItem(10, cancel);

        ItemStack itemDisplay = item.clone();
        ItemMeta im = itemDisplay.getItemMeta();
        im.setDisplayName("§f§l" + item.getType().name());
        im.setLore(Arrays.asList("§7Click here if this was a mistake", "§eClick to choose a different item"));
        itemDisplay.setItemMeta(im);
        gui.setItem(12, itemDisplay);

        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta chm = chest.getItemMeta();
        chm.setDisplayName("§6§lǫᴄᴀɴᴛɪᴛʏ: §e" + amount);
        chm.setLore(Arrays.asList("§7Click to type amount in chat"));
        chest.setItemMeta(chm);
        gui.setItem(13, chest);

        ItemStack emerald = new ItemStack(Material.EMERALD);
        ItemMeta em = emerald.getItemMeta();
        em.setDisplayName("§a§lᴘʀɪᴄᴇ ᴘᴇʀ ᴜɴɪᴛ: §e$" + price);
        em.setLore(Arrays.asList("§7Total Cost: §c$" + total, "§7Click to type unit price in chat"));
        emerald.setItemMeta(em);
        gui.setItem(14, emerald);

        ItemStack start = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta stm = start.getItemMeta();
        stm.setDisplayName("§a§lѕᴛᴀʀᴛ ᴏʀᴅᴇʀ");
        stm.setLore(Arrays.asList("§7Click to confirm & create order", "§7Cost: §c$" + total));
        start.setItemMeta(stm);
        gui.setItem(16, start);

        p.openInventory(gui);
    }
}
