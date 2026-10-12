package com.epiclizard.orders;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public class OrdersPlugin extends JavaPlugin implements Listener {
    private OrderManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.manager = new OrderManager(this);
        Bukkit.getPluginManager().registerEvents(this, this);

        Objects.requireNonNull(getCommand("orders")).setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console command support is limited.");
                return true;
            }
            Player player = (Player) sender;
            if (args.length >= 1) {
                String raw = String.join(" ", args).trim();
                if (raw.equalsIgnoreCase("debug")) {
                    if (player.hasPermission("meteorsmp.admin") || player.isOp()) {
                        openDebugGUI(player);
                    } else {
                        player.sendMessage(color("&cYou do not have permission."));
                    }
                    return true;
                }
                if (raw.toLowerCase().startsWith("create debug") || raw.toLowerCase().startsWith("debug ")) {
                    if (player.hasPermission("meteorsmp.admin") || player.isOp()) {
                        // simple debug creation: use item in hand if present, otherwise random
                        ItemStack item = player.getInventory().getItemInMainHand();
                        if (item == null || item.getType() == Material.AIR) {
                            item = new ItemStack(Material.DIAMOND);
                        }
                        int amount = 1;
                        double price = 10;
                        manager.createDebugOrder(item, amount, price, player.getName());
                        player.sendMessage(color("&aDebug order created."));
                    } else {
                        player.sendMessage(color("&cYou do not have permission to use debug commands."));
                    }
                    return true;
                }
            }
            openOrdersMenu(player, 1);
            return true;
        });

        // expiry task: run every minute
        new BukkitRunnable() {
            @Override
            public void run() {
                manager.checkExpiry();
            }
        }.runTaskTimer(this, 20L, 20L * 60);

        getLogger().info("Orders plugin enabled (replica v1.1)");
    }

    @Override
    public void onDisable() {
        manager.save();
        getLogger().info("Orders plugin disabled");
    }

    // ---------- GUI / Menus ----------
    public void openDebugGUI(Player p) {
        Inventory gui = Bukkit.createInventory(null, 3 * 9, color("\u0275ʀᴅᴇʀѕ -> ᴅᴇʙᴜɢ ᴍᴇɴᴜ"));
        ItemStack filler = Utils.named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) gui.setItem(i, filler);
        gui.setItem(11, Utils.named(Material.TNT, color("&c&lCLEAR ALL ACTIVE ORDERS")));
        gui.setItem(13, Utils.named(Material.NETHER_STAR, color("&a&lSPAWN RANDOM DEBUG ORDER")));
        gui.setItem(15, Utils.named(Material.GOLD_INGOT, color("&e&lGIVE $10,000 TEST BALANCE")));
        p.openInventory(gui);
    }

    public void openOrdersMenu(Player p, int page) {
        List<Order> active = manager.getActiveOrders().stream().filter(o -> o.getPrice() > 0 || o.getAmount() > o.getProgress()).collect(Collectors.toList());
        int start = (page - 1) * 45;
        Inventory gui = Bukkit.createInventory(null, 6 * 9, color("\u0275ʀᴅᴇʀѕ (ᴘᴀɢᴇ " + page + ")"));
        ItemStack filler = Utils.named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) gui.setItem(i, filler);

        int slot = 0;
        for (int i = start; i < Math.min(active.size(), start + 45); i++) {
            Order o = active.get(i);
            ItemStack display = o.getItem().clone();
            Utils.setName(display, color("&f" + o.getItemDisplay()));
            List<String> lore = new ArrayList<>();
            lore.add(color("&a" + o.getAmount() + " requested"));
            lore.add(color("&7Created by: &e" + o.getOwnerName()));
            lore.add(" ");
            lore.add(color("&a$" + o.getPrice() + " each"));
            lore.add(color("&a" + o.getProgress() + "/" + o.getAmount() + " ᴅᴇʟɪᴠᴇʀᴇᴅ"));
            lore.add(" ");
            long diff = Math.max(0, o.getExpireTime() - System.currentTimeMillis() / 1000L);
            long days = diff / 86400;
            long hours = (diff % 86400) / 3600;
            lore.add(color("&7" + days + "ᴅ " + hours + "ʜ ᴜɴᴛɪʟ ǫʀᴅᴇʀ ᴇxᴘɪʀᴇѕ"));
            Utils.setLore(display, lore);
            gui.setItem(slot++, display);
        }

        gui.setItem(47, Utils.named(Material.HOPPER, color("&eѕᴏʀᴛ ǫʀᴅᴇʀѕ")));
        gui.setItem(48, Utils.named(Material.AMETHYST_SHARD, color("&dѕʜᴏᴘ")));
        gui.setItem(49, Utils.named(Material.BOOK, color("&aᴄʀᴇᴀᴛᴇ ǫʀᴅᴇʀ")));
        gui.setItem(50, Utils.named(Material.OAK_SIGN, color("&eѕᴇᴀʀᴄʜ ǫʀᴅᴇʀѕ")));
        gui.setItem(51, Utils.named(Material.CHEST, color("&6ʏᴏᴜʀ ǫʀᴅᴇʀѕ")));
        if (page > 1) gui.setItem(45, Utils.named(Material.ARROW, color("&fᴘʀᴇᴠɪᴏᴜѕ ᴘᴀɢᴇ")));
        if (active.size() > start + 45) gui.setItem(53, Utils.named(Material.ARROW, color("&fɴᴇxt ᴘᴀɢᴇ")));

        p.openInventory(gui);
        manager.setViewerPage(p, page);
    }

    public void openNewOrderGUI(Player p) {
        Inventory gui = Bukkit.createInventory(null, 3 * 9, color("\u0275ʀᴅᴇʀѕ -> ɴᴇᴡ ǫʀᴅᴇʀ"));
        ItemStack filler = Utils.named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) gui.setItem(i, filler);

        ItemStack selected = manager.getSetupItem(p);
        if (selected == null) {
            selected = new ItemStack(Material.STONE);
            manager.setSetupItem(p, selected);
        }
        ItemStack itemDisplay = selected.clone();
        Utils.setName(itemDisplay, color("&f&l" + Utils.cleanName(selected)));
        Utils.setLore(itemDisplay, Arrays.asList(color("&7Click here if this was a mistake"), color("&eClick to choose a different item")));
        gui.setItem(12, itemDisplay);

        gui.setItem(10, Utils.named(Material.RED_CONCRETE, color("&c&lᴄᴀɴᴄᴇʟ ᴏʀᴅᴇʀ")));
        gui.setItem(13, Utils.named(Material.CHEST, color("&6&lǫᴜᴀɴᴛɪᴛʏ: &e" + manager.getSetupAmount(p))));
        gui.setItem(14, Utils.named(Material.EMERALD, color("&a&lᴘʀɪᴄᴇ ᴘᴇʀ ᴜɴɪᴛ: &e$" + manager.getSetupPrice(p))));
        gui.setItem(16, Utils.named(Material.LIME_STAINED_GLASS_PANE, color("&a&lѕᴛᴀʀᴛ ᴏʀᴅᴇʀ")));

        p.openInventory(gui);
    }

    public void openFulfillGUI(Player p, String id) {
        Order o = manager.getOrder(id);
        if (o == null) return;
        Inventory gui = Bukkit.createInventory(null, 6 * 9, color("\u0275ʀᴅᴇʀѕ -> ᴅᴇʟɪᴠᴇʀ ɪᴛᴇᴍѕ"));
        ItemStack filler = Utils.named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) gui.setItem(i, filler);

        ItemStack info = Utils.named(Material.WRITTEN_BOOK, color("&eꜰᴜʟꜰɪʟʟɪɴɢ: &a" + o.getItemDisplay()));
        Utils.setLore(info, Arrays.asList(color("&7Needed: &f" + (o.getAmount() - o.getProgress())), color("&7Reward: &a$" + o.getPrice() + " &7per unit")));
        gui.setItem(49, info);
        gui.setItem(53, Utils.named(Material.LIME_STAINED_GLASS_PANE, color("&a&lᴄᴏɴꜰɪʀᴍ ꜰᴜʟꜰɪʟʟ")));
        gui.setItem(45, Utils.named(Material.RED_STAINED_GLASS_PANE, color("&c&lᴄᴀɴᴄᴇʟ")));
        manager.setFulfillTarget(p, id);
        p.openInventory(gui);
    }

    // ---------- Events ----------
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        int slot = e.getRawSlot();
        e.setCancelled(true);

        if (title.contains("ᴅᴇʙᴜɢ ᴍᴇɴᴜ")) {
            if (slot == 11) {
                manager.clearAllOrders();
                p.sendMessage(color("&cCleared all active orders!"));
                p.closeInventory();
            } else if (slot == 13) {
                ItemStack item = getRandomLegalItem();
                manager.createDebugOrder(item, (int) (Math.random() * 64) + 1, (int) (Math.random() * 299) + 2, "Server");
                openOrdersMenu(p, 1);
            } else if (slot == 15) {
                manager.addBalance(p.getUniqueId().toString(), 10000);
                p.sendMessage(color("&aAdded &e$10,000 &ato your balance!"));
            }
            return;
        }

        if (title.contains("ǫʀᴅᴇʀѕ (ᴘᴀɢᴇ")) {
            if (slot == 45) {
                int page = manager.getViewerPage(p);
                if (page > 1) openOrdersMenu(p, page - 1);
                return;
            }
            if (slot == 53) {
                int page = manager.getViewerPage(p);
                openOrdersMenu(p, page + 1);
                return;
            }
            if (slot == 49) {
                openNewOrderGUI(p);
                return;
            }
            if (slot < 45) {
                List<Order> active = manager.getActiveOrders();
                int page = manager.getViewerPage(p);
                int index = (page - 1) * 45 + slot;
                if (index < active.size()) {
                    Order o = active.get(index);
                    if (o.getOwnerUuid().equals(p.getUniqueId().toString())) {
                        openYourOrdersMenu(p);
                    } else {
                        openFulfillGUI(p, o.getId());
                    }
                }
                return;
            }
        }

        if (title.contains("ɴᴇᴡ ǫʀᴅᴇʀ")) {
            if (slot == 10) {
                manager.clearSetup(p);
                openOrdersMenu(p, 1);
                return;
            }
            if (slot == 12) {
                // choose item from hand
                ItemStack inHand = p.getInventory().getItemInMainHand();
                if (inHand == null || inHand.getType() == Material.AIR) {
                    p.sendMessage(color("&cHold an item in your hand to select it."));
                } else {
                    manager.setSetupItem(p, inHand);
                }
                openNewOrderGUI(p);
                return;
            }
            if (slot == 13) {
                // open chat input to set amount
                p.closeInventory();
                manager.setAwaitingAmount(p, true);
                p.sendMessage(color("&eType the desired quantity in chat (or type cancel):"));
                return;
            }
            if (slot == 14) {
                p.closeInventory();
                manager.setAwaitingPrice(p, true);
                p.sendMessage(color("&eType the unit price ($) in chat (or type cancel):"));
                return;
            }
            if (slot == 16) {
                if (manager.finalizeOrderCreation(p)) {
                    p.sendMessage(color("&aOrder created!"));
                    openOrdersMenu(p, 1);
                } else {
                    openNewOrderGUI(p);
                }
                return;
            }
        }

        if (title.contains("ᴅᴇʟɪᴠᴇʀ ɪᴛᴇᴍѕ")) {
            if (slot == 45) {
                openOrdersMenu(p, 1);
                return;
            }
            if (slot == 53) {
                String id = manager.getFulfillTarget(p);
                if (id == null) return;
                Order o = manager.getOrder(id);
                if (o == null) return;
                int deposited = manager.depositFromPlayerForOrder(p, o);
                if (deposited <= 0) {
                    p.sendMessage(color("&cPlace matching items (or Shulker Boxes containing them) into your inventory before confirming!"));
                    return;
                }
                double payout = deposited * o.getPrice();
                manager.addBalance(p.getUniqueId().toString(), payout);
                o.addProgress(deposited);
                manager.save();
                p.sendMessage(color("&aSuccessfully delivered &e" + deposited + "x " + o.getItemDisplay() + "&a! Earned &a$" + payout + "&a."));
                Player owner = Bukkit.getPlayer(o.getOwnerUuid());
                if (owner != null && !o.getOwnerName().equals("Server")) {
                    owner.sendMessage(color("&e" + p.getName() + " &fhas delivered &a" + deposited + "x " + o.getItemDisplay() + " &ffor your order!"));
                }
                openOrdersMenu(p, 1);
                manager.clearFulfillTarget(p);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        String title = e.getView().getTitle();
        if (title.contains("ᴅᴇʟɪᴠᴇʀ ɪᴛᴇᴍѕ")) {
            manager.clearFulfillTarget(p);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (manager.isAwaitingAmount(p)) {
            e.setCancelled(true);
            manager.setAwaitingAmount(p, false);
            String msg = e.getMessage();
            if (msg.equalsIgnoreCase("cancel")) {
                p.sendMessage(color("&cCancelled input."));
                Bukkit.getScheduler().runTask(this, () -> openNewOrderGUI(p));
                return;
            }
            try {
                int num = Integer.parseInt(msg);
                if (num > 0) {
                    manager.setSetupAmount(p, num);
                    p.sendMessage(color("&aAmount set to &e" + num + "&a."));
                } else {
                    p.sendMessage(color("&cAmount must be greater than 0!"));
                }
            } catch (NumberFormatException ex) {
                p.sendMessage(color("&cInvalid number! Please enter a whole integer."));
            }
            Bukkit.getScheduler().runTask(this, () -> openNewOrderGUI(p));
        } else if (manager.isAwaitingPrice(p)) {
            e.setCancelled(true);
            manager.setAwaitingPrice(p, false);
            String msg = e.getMessage();
            if (msg.equalsIgnoreCase("cancel")) {
                p.sendMessage(color("&cCancelled input."));
                Bukkit.getScheduler().runTask(this, () -> openNewOrderGUI(p));
                return;
            }
            try {
                double price = Double.parseDouble(msg);
                if (price > 0) {
                    manager.setSetupPrice(p, price);
                    p.sendMessage(color("&aPrice set to &e$" + price + "&a."));
                } else {
                    p.sendMessage(color("&cPrice must be greater than 0!"));
                }
            } catch (NumberFormatException ex) {
                p.sendMessage(color("&cInvalid price!"));
            }
            Bukkit.getScheduler().runTask(this, () -> openNewOrderGUI(p));
        }
    }

    // ---------- Helpers ----------
    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private ItemStack getRandomLegalItem() {
        Material[] mats = Material.values();
        Material pick;
        do {
            pick = mats[(int) (Math.random() * mats.length)];
        } while (Utils.isIllegalMaterial(pick));
        return new ItemStack(pick);
    }

    public void openYourOrdersMenu(Player p) {
        Inventory gui = Bukkit.createInventory(null, 6 * 9, color("ʏᴏᴜʀ ǫʀᴅᴇʀѕ"));
        ItemStack filler = Utils.named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) gui.setItem(i, filler);
        List<Order> mine = manager.getOrdersByOwner(p.getUniqueId().toString());
        int slot = 0;
        for (Order o : mine) {
            if (slot >= 45) break;
            ItemStack d = o.getItem().clone();
            Utils.setName(d, color("&f" + o.getItemDisplay()));
            List<String> lore = new ArrayList<>();
            lore.add(color("&a" + o.getAmount() + " requested"));
            lore.add(" ");
            if (o.getPrice() > 0) {
                lore.add(color("&a$" + o.getPrice() + " each"));
                lore.add(color("&a" + o.getProgress() + "/" + o.getAmount() + " ᴅᴇʟɪᴠᴇʀᴇᴅ"));
            } else {
                lore.add(color("&cᴄᴀɴᴄᴇʟʟᴇᴅ"));
                lore.add(color("&a" + o.getProgress() + "/" + o.getAmount() + " ᴅᴇʟɪᴠᴇʀᴇᴅ"));
            }
            Utils.setLore(d, lore);
            gui.setItem(slot++, d);
        }
        gui.setItem(45, Utils.named(Material.ARROW, color("&fʙᴀᴄᴋ ᴛᴏ ǫʀᴅᴇʀѕ")));
        p.openInventory(gui);
    }
}
