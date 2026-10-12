package com.meteorsmp.ultimatemeteorsmp;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class OrderCommand implements CommandExecutor {
    private final UltimateMeteorSMP plugin;

    public OrderCommand(UltimateMeteorSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&6MeteorSMP&8] &aOpening orders marketplace..."));
        // TODO: Open Main GUI Inventory here
        return true;
    }
}
