package com.meteorsmp.ultimatemeteorsmp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AnnounceCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("server.announce")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /announce <message>");
            return true;
        }

        String message = ChatColor.translateAlternateColorCodes('&', String.join(" ", args));
        String title = ChatColor.DARK_RED + "" + ChatColor.BOLD + "ᴀɴɴᴏᴜɴᴄᴇᴍᴇɴᴛ";

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(title, message, 10, 100, 20);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        Bukkit.broadcastMessage(ChatColor.DARK_RED + "[ᴀɴɴᴏᴜɴᴄᴇ] " + ChatColor.WHITE + message);
        return true;
    }
}
