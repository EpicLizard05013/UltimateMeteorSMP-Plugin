package com.meteorsmp.ultimatemeteorsmp;

import com.meteorsmp.ultimatemeteorsmp.auction.AuctionManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;

public class MoneyCommand implements CommandExecutor {
    private final UltimateMeteorSMP plugin;

    public MoneyCommand(UltimateMeteorSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ultimatemeteorsmp.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <player> <amount>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        double amount = AuctionManager.parsePrice(args[1]);

        if (amount <= 0) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.invalid-price", "&7Invalid amount format! Use numbers with suffixes (&e10k&7, &e5m&7).")));
            return true;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            sender.sendMessage(ChatColor.RED + "Vault economy provider not found!");
            return true;
        }

        Economy econ = rsp.getProvider();
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", "&a[AH] &f"));
        String targetName = target.getName() != null ? target.getName() : "Player";

        if (command.getName().equalsIgnoreCase("addmoney")) {
            econ.depositPlayer(target, amount);
            sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', "&aSuccessfully added &e$" + AuctionManager.formatPrice(amount) + " &fto &e" + targetName + "&f's balance."));
            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', "&aYou received &e$" + AuctionManager.formatPrice(amount) + "&f!"));
            }
        } else if (command.getName().equalsIgnoreCase("removemoney")) {
            econ.withdrawPlayer(target, amount);
            sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', ChatColor.RED + "Successfully removed &e$" + AuctionManager.formatPrice(amount) + " &ffrom &e" + targetName + "&f's balance."));
            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', ChatColor.RED + "&e$" + AuctionManager.formatPrice(amount) + " &fwas removed from your balance."));
            }
        }

        return true;
    }
}
