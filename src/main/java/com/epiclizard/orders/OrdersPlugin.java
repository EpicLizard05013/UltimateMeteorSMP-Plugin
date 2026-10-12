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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OrdersPlugin extends JavaPlugin implements Listener {
    private OrderManager orderManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.orderManager = new OrderManager(this);
        Bukkit.getPluginManager().registerEvents(this, this);

        Objects.requireNonNull(getCommand("orders"), "orders command missing").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("debug")) {
                if (player.hasPermission("meteorsmp.admin") || player.isOp()) {
                    openDebugGUI(player);
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission.");
                }
                return true;
            }

            openOrdersMenu(player, 1);
            return true;
        });

        new BukkitRunnable() {
            @Override
            public void run() {
                orderManager.checkExpiry();
            }
        }.runTaskTimer(this, 20L, 20L * 60L);
    }

    @Override
    public void onDisable() {
        if (orderManager != null) {
            orderManager.save();
        }
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void openDebugGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 3 * 9, color("Orders -> Debug Menu"));
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, Utils.named(Material.GRAY_STAINED_GLASS_PANE, " "));
        }
        gui.setItem(11, Utils.named(Material.TNT, color("&c&lClear all active orders")));
        gui.setItem(13, Utils.named(Material.NETHER_STAR, color("&a&lSpawn random debug order")));
        gui.setItem(15, Utils.named(Material.GOLD_INGOT, color("&e&lGive $10,000 test balance")));
        player.openInventory(gui);
    }

    public void openOrdersMenu(Player player, int page) {
        List<Order> orders = orderManager.getActiveOrders();
        Inventory gui = Bukkit.createInventory(null, 6 * 9, color("Orders (Page " + page + ")"));
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, Utils.named(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        int start = (page - 1) * 45;
        int slot = 0;
        for (int i = start; i < Math.min(start + 45, orders.size()); i++) {
            Order order = orders.get(i);
            if (order == null) continue;
            ItemStack display = order.getItem().clone();
            Utils.setName(display, color("&f" + order.getDisplayName()));
            List<String> lore = new ArrayList<>();
            lore.add(color("&a" + order.getAmount() + " requested"));
            lore.add(color("&7Created by: &e" + order.getOwnerName()));
            lore.add(" ");
            lore.add(color("&a$" + order.getPrice() + " each"));
            lore.add(color("&a" + order.getProgress() + "/" + order.getAmount() + " delivered"));
            Utils.setLore(display, lore);
            gui.setItem(slot++, display);
        }

        gui.setItem(45, Utils.named(Material.ARROW, color("&fPrevious page")));
        gui.setItem(53, Utils.named(Material.ARROW, color("&fNext page")));
        gui.setItem(49, Utils.named(Material.BOOK, color("&aCreate order")));
        gui.setItem(51, Utils.named(Material.CHEST, color("&6Your orders")));
        player.openInventory(gui);
        orderManager.setViewerPage(player, page);
    }

    public void openNewOrderGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 3 * 9, color("Orders -> New Order"));
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, Utils.named(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        ItemStack selected = orderManager.getSetupItem(player);
        if (selected == null || selected.getType() == Material.AIR) {
            selected = new ItemStack(Material.STONE);
            orderManager.setSetupItem(player, selected);
        }

        ItemStack itemDisplay = selected.clone();
        Utils.setName(itemDisplay, color("&f&l" + Utils.cleanName(itemDisplay)));
        List<String> lore = new ArrayList<>();
        lore.add(color("&7Click to choose a different item"));
        Utils.setLore(itemDisplay, lore);
        gui.setItem(12, itemDisplay);

        gui.setItem(10, Utils.named(Material.RED_CONCRETE, color("&c&lCancel")));
        gui.setItem(13, Utils.named(Material.CHEST, color("&6&lQuantity: &e" + orderManager.getSetupAmount(player))));
        gui.setItem(14, Utils.named(Material.EMERALD, color("&a&lPrice: &e$" + orderManager.getSetupPrice(player))));
        gui.setItem(16, Utils.named(Material.LIME_STAINED_GLASS_PANE, color("&a&lStart order")));
        player.openInventory(gui);
    }

    public void openFulfillGUI(Player player, String id) {
        Order order = orderManager.getOrder(id);
        if (order == null) return;

        Inventory gui = Bukkit.createInventory(null, 6 * 9, color("Orders -> Deliver Items"));
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, Utils.named(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        ItemStack info = Utils.named(Material.WRITTEN_BOOK, color("&eFulfilling: &a" + order.getDisplayName()));
        List<String> lore = new ArrayList<>();
        lore.add(color("&7Needed: &f" + (order.getAmount() - order.getProgress())));
        lore.add(color("&7Reward: &a$" + order.getPrice() + " per unit"));
        Utils.setLore(info, lore);
        gui.setItem(49, info);
        gui.setItem(53, Utils.named(Material.LIME_STAINED_GLASS_PANE, color("&a&lConfirm fulfill")));
        gui.setItem(45, Utils.named(Material.RED_STAINED_GLASS_PANE, color("&c&lCancel")));

        orderManager.setFulfillTarget(player, id);
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        int slot = event.getRawSlot();
        event.setCancelled(true);

        if (title.contains("Debug Menu")) {
            if (slot == 11) {
                orderManager.clearAllOrders();
                player.sendMessage(color("&cCleared all active orders!"));
                player.closeInventory();
                return;
            }
            if (slot == 13) {
                ItemStack random = new ItemStack(Material.DIAMOND);
                orderManager.createDebugOrder(random, 8 + (int) (Math.random() * 32), 2 + (Math.random() * 100), "Server");
                openOrdersMenu(player, 1);
                return;
            }
            if (slot == 15) {
                orderManager.addBalance(player.getUniqueId().toString(), 10000);
                player.sendMessage(color("&aAdded &e$10,000 &ato your balance!"));
            }
            return;
        }

        if (title.contains("Orders (Page")) {
            if (slot == 45) {
                int page = orderManager.getViewerPage(player);
                if (page > 1) openOrdersMenu(player, page - 1);
                return;
            }
            if (slot == 53) {
                int page = orderManager.getViewerPage(player);
                openOrdersMenu(player, page + 1);
                return;
            }
            if (slot == 49) {
                openNewOrderGUI(player);
                return;
            }
            if (slot == 51) {
                player.sendMessage(color("&eYour orders list is ready to be implemented."));
                return;
            }
            if (slot < 45) {
                List<Order> orders = orderManager.getActiveOrders();
                int page = orderManager.getViewerPage(player);
                int index = (page - 1) * 45 + slot;
                if (index >= 0 && index < orders.size()) {
                    Order order = orders.get(index);
                    if (order != null) {
                        openFulfillGUI(player, order.getId());
                    }
                }
            }
            return;
        }

        if (title.contains("New Order")) {
            if (slot == 10) {
                orderManager.clearSetup(player);
                openOrdersMenu(player, 1);
                return;
            }
            if (slot == 12) {
                ItemStack inHand = player.getInventory().getItemInMainHand();
                if (inHand == null || inHand.getType() == Material.AIR) {
                    player.sendMessage(color("&cHold an item in your hand first."));
                } else {
                    orderManager.setSetupItem(player, inHand.clone());
                }
                openNewOrderGUI(player);
                return;
            }
            if (slot == 13) {
                player.closeInventory();
                orderManager.setAwaitingAmount(player, true);
                player.sendMessage(color("&eType a quantity in chat or type cancel."));
                return;
            }
            if (slot == 14) {
                player.closeInventory();
                orderManager.setAwaitingPrice(player, true);
                player.sendMessage(color("&eType a unit price in chat or type cancel."));
                return;
            }
            if (slot == 16) {
                if (orderManager.finalizeOrderCreation(player)) {
                    player.sendMessage(color("&aOrder created!"));
                    openOrdersMenu(player, 1);
                } else {
                    openNewOrderGUI(player);
                }
                return;
            }
        }

        if (title.contains("Deliver Items")) {
            if (slot == 45) {
                openOrdersMenu(player, 1);
                return;
            }
            if (slot == 53) {
                String id = orderManager.getFulfillTarget(player);
                if (id == null) return;
                Order order = orderManager.getOrder(id);
                if (order == null) return;

                int deposited = orderManager.depositFromPlayerForOrder(player, order);
                if (deposited <= 0) {
                    player.sendMessage(color("&cNo matching items were deposited."));
                    return;
                }

                double payout = deposited * order.getPrice();
                orderManager.addBalance(player.getUniqueId().toString(), payout);
                order.addProgress(deposited);
                orderManager.save();
                player.sendMessage(color("&aSuccessfully delivered &e" + deposited + "x " + order.getDisplayName() + "&a! Earned &a$" + payout + "&a."));
                openOrdersMenu(player, 1);
                orderManager.clearFulfillTarget(player);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        if (title.contains("Deliver Items")) {
            orderManager.clearFulfillTarget(player);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (orderManager.isAwaitingAmount(player)) {
            event.setCancelled(true);
            orderManager.setAwaitingAmount(player, false);
            String msg = event.getMessage();
            if (msg.equalsIgnoreCase("cancel")) {
                player.sendMessage(color("&cCancelled input."));
                Bukkit.getScheduler().runTask(this, () -> openNewOrderGUI(player));
                return;
            }
            try {
                int amount = Integer.parseInt(msg);
                if (amount > 0) {
                    orderManager.setSetupAmount(player, amount);
                    player.sendMessage(color("&aAmount set to &e" + amount + "&a."));
                } else {
                    player.sendMessage(color("&cAmount must be greater than 0."));
                }
            } catch (NumberFormatException ex) {
                player.sendMessage(color("&cInvalid number. Please enter a whole integer."));
            }
            Bukkit.getScheduler().runTask(this, () -> openNewOrderGUI(player));
            return;
        }

        if (orderManager.isAwaitingPrice(player)) {
            event.setCancelled(true);
            orderManager.setAwaitingPrice(player, false);
            String msg = event.getMessage();
            if (msg.equalsIgnoreCase("cancel")) {
                player.sendMessage(color("&cCancelled input."));
                Bukkit.getScheduler().runTask(this, () -> openNewOrderGUI(player));
                return;
            }
            try {
                double price = Double.parseDouble(msg);
                if (price > 0) {
                    orderManager.setSetupPrice(player, price);
                    player.sendMessage(color("&aPrice set to &e$" + price + "&a."));
                } else {
                    player.sendMessage(color("&cPrice must be greater than 0."));
                }
            } catch (NumberFormatException ex) {
                player.sendMessage(color("&cInvalid price."));
            }
            Bukkit.getScheduler().runTask(this, () -> openNewOrderGUI(player));
        }
    }
}
