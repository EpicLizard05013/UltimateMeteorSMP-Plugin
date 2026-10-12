package com.meteorsmp.ultimatemeteorsmp;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

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

        OrderManager manager = plugin.getOrderManager();

        if (args.length > 0) {
            String raw = String.join(" ", args);

            if (raw.equalsIgnoreCase("debug")) {
                if (player.isOp() || player.hasPermission("meteorsmp.admin")) {
                    OrderMenus.openDebugGUI(player);
                } else {
                    player.sendMessage(OrderManager.TAG + " §cYou do not have permission.");
                }
                return true;
            }

            if (raw.startsWith("create debug") || raw.startsWith("debug ")) {
                if (player.isOp() || player.hasPermission("meteorsmp.admin")) {
                    raw = raw.replace("create debug ", "").replace("debug ", "");

                    if (raw.equalsIgnoreCase("random random") || raw.equalsIgnoreCase("random")) {
                        Material[] mats = Material.values();
                        Material randomMat = mats[new Random().nextInt(mats.length)];
                        while (manager.isIllegalItem(new ItemStack(randomMat))) {
                            randomMat = mats[new Random().nextInt(mats.length)];
                        }
                        int amount = new Random().nextInt(64) + 1;
                        double price = new Random().nextInt(299) + 2;
                        manager.createOrder("Server", new ItemStack(randomMat), amount, price);
                        player.sendMessage(OrderManager.TAG + " §aDebug order created: §e" + amount + "x " + randomMat.name() + " §afor §e$" + price + " §aeach (Owner: Server)!");
                        return true;
                    }
                    
                    // Additional parsing for custom debug items can be handled similarly
                } else {
                    player.sendMessage(OrderManager.TAG + " §cYou do not have permission to use debug commands.");
                    return true;
                }
            }
        }

        manager.searchQuery.remove(player.getUniqueId());
        OrderMenus.openOrdersMenu(player, 1, manager);
        return true;
    }
}
