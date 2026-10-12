package com.meteorsmp.ultimatemeteorsmp.auction;

import com.meteorsmp.ultimatemeteorsmp.UltimateMeteorSMP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.Method;
import java.util.Map;

public class AuctionListener implements Listener {
    private final UltimateMeteorSMP plugin;
    private final AuctionGuiManager guiManager;

    public AuctionListener(UltimateMeteorSMP plugin, AuctionGuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (guiManager.getSearchingChat().getOrDefault(player.getUniqueId(), false)) {
            event.setCancelled(true);
            guiManager.getSearchingChat().remove(player.getUniqueId());
            String msg = event.getMessage();
            if (msg.equalsIgnoreCase("cancel") || msg.equalsIgnoreCase("none")) {
                guiManager.getActiveSearch().remove(player.getUniqueId());
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", "&a[AH] &f") + "&7Search filter cleared."));
            } else {
                guiManager.getActiveSearch().put(player.getUniqueId(), msg);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", "&a[AH] &f") + "&7Search filter set to: &e" + msg));
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> guiManager.openMainGui(player, 1));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = ChatColor.stripColor(event.getView().getTitle());

        if (title.contains("Shulker Preview")) {
            event.setCancelled(true);
            return;
        }

        if (title.contains("Confirm Purchase")) {
            event.setCancelled(true);

            Inventory top = event.getView().getTopInventory();
            if (event.getClickedInventory() != null && !event.getClickedInventory().equals(top)) {
                return;
            }

            int slot = event.getSlot();
            Integer id = guiManager.getConfirmId().get(player.getUniqueId());
            if (slot == 11) {
                guiManager.openMainGui(player, 1);
            } else if (slot == 15 && id != null) {
                AuctionItem item = plugin.getAuctionManager().getListing(id);
                if (item == null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.item-expired")));
                    player.closeInventory();
                    return;
                }

                if (item.getSellerUuid().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", "&a[AH] &f") + "&cYou cannot buy your own listing."));
                    player.closeInventory();
                    return;
                }

                if (!plugin.getAuctionManager().completePurchase(player, item)) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", "&a[AH] &f") + "&cPurchase failed. Economy is not configured or you cannot afford this item."));
                    player.closeInventory();
                    return;
                }

                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", "&a[AH] &f") + "&aSuccessfully purchased item!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                player.closeInventory();
            }
            return;
        }

        if (title.contains("Auction")) {
            event.setCancelled(true);

            Inventory top = event.getView().getTopInventory();
            if (event.getClickedInventory() != null && !event.getClickedInventory().equals(top)) {
                return;
            }

            int slot = event.getSlot();
            Map<Integer, Integer> slotMap = guiManager.getSlotMapping(player.getUniqueId());

            if (slot < 45 && slotMap != null && slotMap.containsKey(slot)) {
                int id = slotMap.get(slot);
                AuctionItem item = plugin.getAuctionManager().getListing(id);
                if (item == null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.item-expired")));
                    return;
                }

                if (event.isRightClick() && item.getItem().getType().name().contains("SHULKER_BOX")) {
                    guiManager.openShulkerPreview(player, item.getItem());
                    return;
                }

                if (item.getSellerUuid().equals(player.getUniqueId())) {
                    plugin.getAuctionManager().removeListing(id);
                    player.getInventory().addItem(item.getItem());
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix") + "&cCancelled listing and returned item."));
                    guiManager.openYourItemsGui(player, 1);
                } else {
                    if (plugin.getConfig().getBoolean("auction.require-purchase-confirmation", true)) {
                        guiManager.openConfirmGui(player, item);
                    } else {
                        if (!plugin.getAuctionManager().completePurchase(player, item)) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", "&a[AH] &f") + "&cPurchase failed. Economy is not configured or you cannot afford this item."));
                            return;
                        }
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix") + "&aSuccessfully purchased item!"));
                        guiManager.openMainGui(player, 1);
                    }
                }
            } else if (slot == 47) {
                String current = guiManager.getSortMode().getOrDefault(player.getUniqueId(), "high_to_low");
                guiManager.getSortMode().put(player.getUniqueId(), current.equals("high_to_low") ? "low_to_high" : "high_to_low");
                guiManager.openMainGui(player, 1);
            } else if (slot == 50 && title.contains("ʏᴏᴜʀ ɪᴛᴇᴍѕ")) {
                player.closeInventory();
                guiManager.getSearchingChat().put(player.getUniqueId(), true);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix") + "&7Type your search query in chat (&ccancel &7to clear):"));
            } else if (slot == 51 && title.contains("ʏᴏᴜʀ ɪᴛᴇᴍѕ")) {
                String mode = guiManager.getUserViewMode().getOrDefault(player.getUniqueId(), "active");
                guiManager.getUserViewMode().put(player.getUniqueId(), mode.equals("active") ? "transactions" : "active");
                guiManager.openYourItemsGui(player, 1);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        guiManager.getActiveSearch().remove(p.getUniqueId());
        guiManager.getSearchingChat().remove(p.getUniqueId());
        guiManager.getSortMode().remove(p.getUniqueId());
        guiManager.getUserViewMode().remove(p.getUniqueId());
    }
}
