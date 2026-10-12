package com.meteorsmp.ultimatemeteorsmp.auction;

import com.meteorsmp.ultimatemeteorsmp.UltimateMeteorSMP;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AuctionCommand implements CommandExecutor {
    private final UltimateMeteorSMP plugin;
    private final AuctionGuiManager guiManager;

    public AuctionCommand(UltimateMeteorSMP plugin, AuctionGuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }
        Player player = (Player) sender;
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", "&a[AH] &f"));

        if (command.getName().equalsIgnoreCase("ah") || command.getName().equalsIgnoreCase("auctionhouse") || command.getName().equalsIgnoreCase("auction")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("sell")) {
                if (args.length < 2) {
                    player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', "&7Please specify a price! Usage: &e/ah sell <price>"));
                    return true;
                }
                double price = AuctionManager.parsePrice(args[1]);
                if (price <= 0) {
                    player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.invalid-price")));
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item.getType() == Material.AIR) {
                    player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', "&7You must hold an item to sell!"));
                    return true;
                }

                if (!plugin.getConfig().getBoolean("auction.allow-spawners", false) && item.getType().name().contains("SPAWNER")) {
                    player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', "&7You cannot sell spawners in the auction house!"));
                    return true;
                }

                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                int id = plugin.getAuctionManager().createListing(player.getUniqueId(), player.getName(), item, price);

                player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', "&fSuccessfully listed your item for &e$" + AuctionManager.formatPrice(price) + "&f!"));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);

                if (plugin.getConfig().getBoolean("auction.broadcast-listings", true)) {
                    String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
                    for (Player online : plugin.getServer().getOnlinePlayers()) {
                        online.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', "&e" + player.getName() + " &fhas listed &e" + itemName + " &ffor &a$" + AuctionManager.formatPrice(price)));
                    }
                }

                guiManager.openMainGui(player, 1);
                return true;
            } else if (args.length > 0 && args[0].equalsIgnoreCase("view")) {
                if (args.length < 2) {
                    player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', "&7Please specify a player! Usage: &e/ah view <player>"));
                    return true;
                }
                guiManager.openPlayerViewGui(player, args[1], 1);
                return true;
            }

            guiManager.openMainGui(player, 1);
            return true;
        }

        return false;
    }
}
