package com.meteorsmp.ultimatemeteorsmp;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class OrderGUIListener implements Listener {
    private final UltimateMeteorSMP plugin;
    private final Map<UUID, String> pickerSearchFilter = new HashMap<>();
    private final Map<UUID, String> pickerSortMode = new HashMap<>();

    public OrderGUIListener(UltimateMeteorSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        OrderManager manager = plugin.getOrderManager();
        String message = event.getMessage();

        if (manager.awaitingAmount.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            manager.awaitingAmount.remove(player.getUniqueId());
            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(OrderManager.TAG + " §cCancelled input.");
                Bukkit.getScheduler().runTask(plugin, () -> OrderMenus.openNewOrderGUI(player, manager));
                return;
            }
            try {
                int num = Integer.parseInt(message);
                if (num > 0) {
                    manager.setupAmount.put(player.getUniqueId(), num);
                    player.sendMessage(OrderManager.TAG + " §aAmount set to §e" + num + "§a.");
                } else {
                    player.sendMessage(OrderManager.TAG + " §cAmount must be greater than 0!");
                }
            } catch (NumberFormatException e) {
                player.sendMessage(OrderManager.TAG + " §cInvalid number! Please enter a whole integer.");
            }
            Bukkit.getScheduler().runTask(plugin, () -> OrderMenus.openNewOrderGUI(player, manager));
            return;
        }

        if (manager.awaitingPrice.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            manager.awaitingPrice.remove(player.getUniqueId());
            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(OrderManager.TAG + " §cCancelled input.");
                Bukkit.getScheduler().runTask(plugin, () -> OrderMenus.openNewOrderGUI(player, manager));
                return;
            }
            try {
                double num = Double.parseDouble(message);
                if (num > 0) {
                    manager.setupPrice.put(player.getUniqueId(), num);
                    player.sendMessage(OrderManager.TAG + " §aPrice set to §e$" + num + "§a.");
                } else {
                    player.sendMessage(OrderManager.TAG + " §cPrice must be greater than 0!");
                }
            } catch (NumberFormatException e) {
                player.sendMessage(OrderManager.TAG + " §cInvalid price!");
            }
            Bukkit.getScheduler().runTask(plugin, () -> OrderMenus.openNewOrderGUI(player, manager));
            return;
        }

        if (manager.awaitingSearch.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            manager.awaitingSearch.remove(player.getUniqueId());
            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(OrderManager.TAG + " §cCancelled search.");
            } else {
                manager.searchQuery.put(player.getUniqueId(), message);
                player.sendMessage(OrderManager.TAG + " §aFiltering orders by: §e" + message);
            }
            Bukkit.getScheduler().runTask(plugin, () -> OrderMenus.openOrdersMenu(player, 1, manager));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        OrderManager manager = plugin.getOrderManager();
        String title = event.getView().getTitle();

        if (title.equals("ǫʀᴅᴇʀѕ -> ᴅᴇʟɪᴠᴇʀ ɪᴛᴇᴍѕ")) {
            String id = manager.fulfillTarget.remove(player.getUniqueId());
            if (id != null) {
                for (int i = 0; i < 44; i++) {
                    ItemStack is = event.getInventory().getItem(i);
                    if (is != null && is.getType() != Material.AIR) {
                        player.getInventory().addItem(is);
                    }
                }
            }
        } else if (title.equals("ǫʀᴅᴇʀѕ -> ᴄᴏʟʟᴇᴄᴛ ɪᴛᴇᴍѕ")) {
            manager.collectTarget.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (event.getPlayer().getOpenInventory().getTitle().contains("ǫʀᴅᴇʀѕ") || event.getPlayer().getOpenInventory().getTitle().contains("ѕᴇʟᴇᴄᴛ ɪᴛᴇᴍ")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        OrderManager manager = plugin.getOrderManager();
        String title = event.getView().getTitle();
        int slot = event.getRawSlot();

        if (title.equals("ǫʀᴅᴇʀѕ -> ᴅᴇʙᴜɢ ᴍᴇɴᴜ")) {
            event.setCancelled(true);
            if (slot == 11) {
                manager.getActiveOrders().clear();
                manager.getOrderList().clear();
                manager.saveOrders();
                player.sendMessage(OrderManager.TAG + " §cCleared all active orders!");
                player.closeInventory();
            } else if (slot == 13) {
                Material[] mats = Material.values();
                Material randomMat = mats[new Random().nextInt(mats.length)];
                while (manager.isIllegalItem(new ItemStack(randomMat))) {
                    randomMat = mats[new Random().nextInt(mats.length)];
                }
                manager.createOrder("Server", new ItemStack(randomMat), new Random().nextInt(64) + 1, new Random().nextInt(299) + 2);
                OrderMenus.openOrdersMenu(player, 1, manager);
            } else if (slot == 15) {
                manager.addBalance(player.getUniqueId(), 10000.0);
                player.sendMessage(OrderManager.TAG + " §aAdded §e$10,000 §ato your balance!");
            }
        } else if (title.equals("ǫʀᴅᴇʀѕ -> ѕᴇʟᴇᴄᴛ ɪᴛᴇᴍ")) {
            event.setCancelled(true);
            int page = manager.pickerPage.getOrDefault(player.getUniqueId(), 1);
            ItemStack clickedItem = event.getCurrentItem();

            if (slot == 45 && page > 1) {
                OrderMenus.openItemPickerGUI(player, page - 1, manager);
            } else if (slot == 53) {
                OrderMenus.openItemPickerGUI(player, page + 1, manager);
            } else if (slot >= 45 && slot <= 53 && clickedItem != null) {
                Material type = clickedItem.getType();

                // Sign Search Button
                if (type.name().contains("SIGN")) {
                    player.closeInventory();
                    SignInputHandler.openSignPrompt(player, "^^^ Search Item ^^^", input -> {
                        if (input == null || input.isEmpty() || input.equalsIgnoreCase("cancel")) {
                            pickerSearchFilter.remove(player.getUniqueId());
                            player.sendMessage(OrderManager.TAG + " §cItem search cleared.");
                        } else {
                            pickerSearchFilter.put(player.getUniqueId(), input.toLowerCase());
                            player.sendMessage(OrderManager.TAG + " §aFiltering items by: §e" + input);
                        }
                        OrderMenus.openItemPickerGUI(player, 1, manager);
                    });
                } 
                // Hopper Sort Button
                else if (type == Material.HOPPER) {
                    String current = pickerSortMode.getOrDefault(player.getUniqueId(), "az");
                    String next = current.equals("az") ? "za" : "az";
                    pickerSortMode.put(player.getUniqueId(), next);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    OrderMenus.openItemPickerGUI(player, page, manager);
                } 
                // Cauldron Reset Button
                else if (type == Material.CAULDRON || type.name().contains("CAULDRON")) {
                    pickerSearchFilter.remove(player.getUniqueId());
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
                    player.sendMessage(OrderManager.TAG + " §aItem filters reset.");
                    OrderMenus.openItemPickerGUI(player, 1, manager);
                }
            } else if (slot < 45) {
                ItemStack chosen = event.getCurrentItem();
                if (chosen != null && !manager.isIllegalItem(chosen)) {
                    ItemStack single = chosen.clone();
                    single.setAmount(1);
                    manager.setupItem.put(player.getUniqueId(), single);
                    OrderMenus.openNewOrderGUI(player, manager);
                }
            }
        } else if (title.equals("ǫʀᴅᴇʀѕ -> ɴᴇw ǫʀᴅᴇʀ") || title.equals("ǫʀᴅᴇʀѕ -> ɴᴇǫʀᴅᴇʀ")) {
            event.setCancelled(true);
            if (slot == 10) {
                manager.setupItem.remove(player.getUniqueId());
                manager.setupAmount.remove(player.getUniqueId());
                manager.setupPrice.remove(player.getUniqueId());
                OrderMenus.openOrdersMenu(player, 1, manager);
            } else if (slot == 12) {
                OrderMenus.openItemPickerGUI(player, 1, manager);
            } else if (slot == 13) {
                manager.awaitingAmount.put(player.getUniqueId(), true);
                player.closeInventory();
                player.sendMessage(OrderManager.TAG + " §eType desired quantity in chat (or cancel):");
            } else if (slot == 14) {
                manager.awaitingPrice.put(player.getUniqueId(), true);
                player.closeInventory();
                player.sendMessage(OrderManager.TAG + " §eType unit price ($) in chat (or cancel):");
            } else if (slot == 16) {
                ItemStack item = manager.setupItem.getOrDefault(player.getUniqueId(), new ItemStack(Material.STONE));
                int amount = manager.setupAmount.getOrDefault(player.getUniqueId(), 1);
                double price = manager.setupPrice.getOrDefault(player.getUniqueId(), 10.0);
                double total = amount * price;

                if (manager.getBalance(player.getUniqueId()) < total) {
                    player.sendMessage(OrderManager.TAG + " §cYou don't have enough money! Required: §e$" + total + "§c.");
                    OrderMenus.openNewOrderGUI(player, manager);
                    return;
                }

                manager.removeBalance(player.getUniqueId(), total);
                manager.createOrder(player.getName(), item, amount, price);
                player.sendMessage(OrderManager.TAG + " §a§lǫʀᴅᴇʀ ᴄʀᴇᴀᴛᴇᴅ! Ordered " + amount + "x " + item.getType().name());
                OrderMenus.openOrdersMenu(player, 1, manager);
            }
        } else if (title.equals("ǫʀᴅᴇʀѕ -> ᴇᴅɪᴛ ǫʀᴅᴇʀ")) {
            event.setCancelled(true);
            String id = manager.viewTarget.get(player.getUniqueId());
            if (id == null) return;
            Order order = manager.getActiveOrders().get(id);

            if (slot == 15 && order != null) {
                if (order.getProgress() > order.getCollected()) {
                    OrderMenus.openCollectGUI(player, id, 1, manager);
                } else {
                    player.sendMessage(OrderManager.TAG + " §cYou have no delivered items to collect!");
                }
            } else if (slot == 13 && order != null && order.getPrice() > 0) {
                int remaining = order.getAmount() - order.getProgress();
                if (remaining > 0) {
                    double refund = remaining * order.getPrice();
                    manager.addBalance(player.getUniqueId(), refund);
                    player.sendMessage(OrderManager.TAG + " §aOrder cancelled! Refunded §e$" + refund + "§a.");
                }
                order.setPrice(0.0);
                manager.saveOrders();
                OrderMenus.openYourOrdersMenu(player, manager);
            }
        } else if (title.contains("ǫʀᴅᴇʀѕ (ᴘᴀɢᴇ")) {
            event.setCancelled(true);
            int page = manager.guiPage.getOrDefault(player.getUniqueId(), 1);
            if (slot == 45 && page > 1) {
                OrderMenus.openOrdersMenu(player, page - 1, manager);
            } else if (slot == 53) {
                OrderMenus.openOrdersMenu(player, page + 1, manager);
            } else if (slot == 47) {
                String mode = manager.sortMode.getOrDefault(player.getUniqueId(), "newest");
                if (mode.equals("newest")) manager.sortMode.put(player.getUniqueId(), "price_low");
                else if (mode.equals("price_low")) manager.sortMode.put(player.getUniqueId(), "price_high");
                else manager.sortMode.put(player.getUniqueId(), "newest");
                OrderMenus.openOrdersMenu(player, page, manager);
            } else if (slot == 49) {
                OrderMenus.openItemPickerGUI(player, 1, manager);
            } else if (slot == 50) {
                if (manager.searchQuery.containsKey(player.getUniqueId())) {
                    manager.searchQuery.remove(player.getUniqueId());
                    player.sendMessage(OrderManager.TAG + " §aSearch filter cleared!");
                    OrderMenus.openOrdersMenu(player, 1, manager);
                } else {
                    manager.awaitingSearch.put(player.getUniqueId(), true);
                    player.closeInventory();
                    player.sendMessage(OrderManager.TAG + " §eType item search query in chat (or cancel):");
                }
            } else if (slot == 51) {
                OrderMenus.openYourOrdersMenu(player, manager);
            } else if (slot < 45) {
                String id = manager.guiMap.getOrDefault(player.getUniqueId(), new java.util.HashMap<>()).get(slot);
                if (id != null) {
                    Order o = manager.getActiveOrders().get(id);
                    if (o != null && o.getOwner().equals(player.getName())) {
                        OrderMenus.openOrderManagementGUI(player, id, manager);
                    } else {
                        OrderMenus.openFulfillGUI(player, id, manager);
                    }
                }
            }
        } else if (title.equals("ʏᴏᴜʀ ǫʀᴅᴇʀѕ")) {
            event.setCancelled(true);
            if (slot == 45) {
                OrderMenus.openOrdersMenu(player, 1, manager);
            } else if (slot < 45) {
                String id = manager.guiMap.getOrDefault(player.getUniqueId(), new java.util.HashMap<>()).get(slot);
                if (id != null) OrderMenus.openOrderManagementGUI(player, id, manager);
            }
        } else if (title.equals("ǫʀᴅᴇʀѕ -> ᴄᴏʟʟᴇᴄᴛ ɪᴛᴇᴍѕ")) {
            event.setCancelled(true);
            String id = manager.collectTarget.get(player.getUniqueId());
            Order order = manager.getActiveOrders().get(id);
            if (order == null) return;

            if (slot == 51) {
                int uncollected = order.getProgress() - order.getCollected();
                if (uncollected <= 0) {
                    player.sendMessage(OrderManager.TAG + " §cNothing left to collect!");
                    return;
                }
                ItemStack stack = order.getItem().clone();
                stack.setAmount(uncollected);
                player.getInventory().addItem(stack);
                order.setCollected(order.getProgress());
                manager.saveOrders();
                player.sendMessage(OrderManager.TAG + " §aSuccessfully collected all delivered items!");
                OrderMenus.openOrderManagementGUI(player, id, manager);
            }
        } else if (title.equals("ǫʀᴅᴇʀѕ -> ᴅᴇʟɪᴠᴇʀ ɪᴛᴇᴍѕ")) {
            if (slot >= 45) event.setCancelled(true);
            String id = manager.fulfillTarget.get(player.getUniqueId());
            Order order = manager.getActiveOrders().get(id);
            if (order == null) return;

            if (slot == 45) {
                OrderMenus.openOrdersMenu(player, 1, manager);
            } else if (slot == 53) {
                event.setCancelled(true);
                int needed = order.getAmount() - order.getProgress();
                int deposited = 0;

                for (int i = 0; i < 44; i++) {
                    ItemStack is = event.getInventory().getItem(i);
                    if (is != null && is.getType() != Material.AIR) {
                        if (is.getType() == order.getItem().getType()) {
                            int count = is.getAmount();
                            int remainingNeeded = needed - deposited;
                            if (remainingNeeded <= 0) break;

                            if (count <= remainingNeeded) {
                                deposited += count;
                                event.getInventory().setItem(i, null);
                            } else {
                                deposited += remainingNeeded;
                                is.setAmount(count - remainingNeeded);
                                event.getInventory().setItem(i, is);
                                break;
                            }
                        } else if (is.getItemMeta() instanceof BlockStateMeta bsm && bsm.getBlockState() instanceof ShulkerBox box) {
                            ItemStack[] contents = box.getInventory().getContents();
                            boolean modified = false;
                            for (int c = 0; c < contents.length; c++) {
                                ItemStack sItem = contents[c];
                                if (sItem != null && sItem.getType() == order.getItem().getType()) {
                                    int neededNow = needed - deposited;
                                    if (neededNow <= 0) break;
                                    int sCount = sItem.getAmount();
                                    if (sCount <= neededNow) {
                                        deposited += sCount;
                                        contents[c] = null;
                                        modified = true;
                                    } else {
                                        deposited += neededNow;
                                        sItem.setAmount(sCount - neededNow);
                                        contents[c] = sItem;
                                        modified = true;
                                        break;
                                    }
                                }
                            }
                            if (modified) {
                                box.getInventory().setContents(contents);
                                bsm.setBlockState(box);
                                is.setItemMeta(bsm);
                                event.getInventory().setItem(i, is);
                            }
                        }
                    }
                }

                if (deposited <= 0) {
                    player.sendMessage(OrderManager.TAG + " §cPlace matching items (or Shulker Boxes) into the top slots before confirming!");
                    return;
                }

                double payout = deposited * order.getPrice();
                manager.addBalance(player.getUniqueId(), payout);
                order.setProgress(order.getProgress() + deposited);
                manager.saveOrders();

                player.sendMessage(OrderManager.TAG + " §aSuccessfully delivered §e" + deposited + "x " + order.getItem().getType().name() + "§a! Earned §a$" + payout + "§a.");
                OrderMenus.openOrdersMenu(player, 1, manager);
            }
        }
    }
}
