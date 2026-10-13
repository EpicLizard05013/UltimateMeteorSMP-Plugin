package com.meteorsmp.ultimatemeteorsmp.auction;

import com.meteorsmp.ultimatemeteorsmp.SignInputHandler;
import com.meteorsmp.ultimatemeteorsmp.UltimateMeteorSMP;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class AuctionListener implements Listener {
    private final UltimateMeteorSMP plugin;
    private final AuctionGuiManager guiManager;

    public AuctionListener(UltimateMeteorSMP plugin, AuctionGuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getView().getTitle() == null) return;
        
        String rawTitle = event.getView().getTitle();
        String title = ChatColor.stripColor(rawTitle).toLowerCase();

        // NOTE: GUI titles use small-caps Unicode letters (e.g. "ᴀᴜᴄᴛɪᴏɴ"),
        // which are different codepoints from plain ASCII letters and are
        // NOT affected by toLowerCase(). Checks here must match those exact
        // Unicode substrings - "auction" (ASCII) will never match
        // "ᴀᴜᴄᴛɪᴏɴ" (small-caps) even after lowercasing.
        if (title.contains("ѕʜᴜʟᴋᴇʀ ᴘʀᴇᴠɪᴇᴡ")) {
            event.setCancelled(true);
            return;
        }

        if (title.contains("ᴄᴏɴꜰɪʀᴍ ᴘᴜʀᴄʜᴀѕᴇ")) {
            event.setCancelled(true);
            // SECURITY: only react to clicks inside the GUI itself, never the
            // player's own inventory - getSlot() aliases the two, so a click
            // in the player's backpack can report the same number as a GUI
            // slot. getRawSlot() plus this check keeps them distinct.
            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
                return;
            }
            int slot = event.getRawSlot();
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
                if (plugin.getAuctionManager().completePurchase(player, item)) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                }
                player.closeInventory();
            }
            return;
        }

        if (title.contains("ᴀᴜᴄᴛɪᴏɴ")) {
            event.setCancelled(true);
            // SECURITY: see the note in the "confirm purchase" branch above -
            // without this check, a click in the player's own inventory can
            // alias to a listing slot number in the GUI above it, letting a
            // seller "reclaim" (and duplicate) their own listed item just by
            // clicking their own backpack at the right slot index.
            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
                return;
            }
            int slot = event.getRawSlot();
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

                // --- ITEM CANCELLER / RECLAIM FIX ---
                // Check matching UUID as string to ensure safe comparison across all Java/Paper builds
                if (item.getSellerUuid() != null && item.getSellerUuid().toString().equalsIgnoreCase(player.getUniqueId().toString())) {
                    plugin.getAuctionManager().removeListing(id);
                    
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.getItem());
                    if (!leftover.isEmpty()) {
                        for (ItemStack drop : leftover.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", "&a[AH] &f") + "&eYour inventory was full, so the returned item was dropped at your feet!"));
                    }

                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", "&a[AH] &f") + "&cCancelled listing and returned your item."));
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);

                    if (title.contains("ʏᴏᴜʀ ɪᴛᴇᴍѕ")) {
                        guiManager.openYourItemsGui(player, 1);
                    } else {
                        guiManager.openMainGui(player, 1);
                    }
                    return;
                } else {
                    if (plugin.getConfig().getBoolean("auction.require-purchase-confirmation", true)) {
                        guiManager.openConfirmGui(player, item);
                    } else {
                        if (plugin.getAuctionManager().completePurchase(player, item)) {
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                            guiManager.openMainGui(player, 1);
                        }
                    }
                }
            } else if (slot == 47) {
                String current = guiManager.getSortMode().getOrDefault(player.getUniqueId(), "high_to_low");
                guiManager.getSortMode().put(player.getUniqueId(), current.equals("high_to_low") ? "low_to_high" : "high_to_low");
                guiManager.openMainGui(player, 1);
            } else if (slot == 50) { // Sign Search Button
                player.closeInventory();
                // 1 tick delay: opening a sign editor in the same tick as
                // closeInventory() gets dropped by the client, since it's
                // still processing the inventory-close packet.
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    SignInputHandler.openSignPrompt(player, "^^^ Search ^^^", input -> {
                        if (input == null || input.isEmpty()) {
                            guiManager.getActiveSearch().remove(player.getUniqueId());
                        } else {
                            guiManager.getActiveSearch().put(player.getUniqueId(), input);
                        }
                        guiManager.openMainGui(player, 1);
                    });
                }, 1L);
            } else if (slot == 51) {
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
        guiManager.getSortMode().remove(p.getUniqueId());
        guiManager.getUserViewMode().remove(p.getUniqueId());
    }
}
