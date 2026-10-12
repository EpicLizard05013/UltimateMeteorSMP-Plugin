package com.meteorsmp.ultimatemeteorsmp;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class OrdersHelpCommand implements CommandExecutor {
    private final UltimateMeteorSMP plugin;

    public OrdersHelpCommand(UltimateMeteorSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l=== METEORSMP ORDERS HELP ==="));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6/orders &7- Open main order market GUI"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6/orders debug &7- Open Admin Debug Management GUI"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l============================="));
        return true;
    }
}
