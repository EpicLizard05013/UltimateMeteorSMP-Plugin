package com.meteorsmp.ultimatemeteorsmp;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatModerationListener implements Listener {
    private final UltimateMeteorSMP plugin;
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastMessageContent = new ConcurrentHashMap<>();

    private final List<String> blockedWords = Arrays.asList(
        "nigger", "nigga", "faggot", "kys"
    );

    public ChatModerationListener(UltimateMeteorSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("ultimatemeteorsmp.bypass.chat")) return;

        String message = event.getMessage();
        String lowerMessage = message.toLowerCase();

        // 1. Check for Blocked Words / Slurs
        for (String word : blockedWords) {
            if (lowerMessage.contains(word)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "[AntiSpam] Your message contained blocked language and was blocked.");
                
                for (Player online : plugin.getServer().getOnlinePlayers()) {
                    if (online.hasPermission("ultimatemeteorsmp.staff")) {
                        online.sendMessage(ChatColor.DARK_RED + "[Watchdog] " + ChatColor.YELLOW + player.getName() + " tried to say a blocked word.");
                    }
                }
                return;
            }
        }

        // 2. Anti-Spam / Rate Limiting
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        if (lastMessageTime.containsKey(uuid)) {
            long timeDiff = now - lastMessageTime.get(uuid);
            String prevMsg = lastMessageContent.getOrDefault(uuid, "");

            if (prevMsg.equalsIgnoreCase(message) && timeDiff < 3000) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "[AntiSpam] Please do not spam the same message!");
                return;
            }
            if (timeDiff < 800) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "[AntiSpam] You are sending messages too fast!");
                return;
            }
        }

        lastMessageTime.put(uuid, now);
        lastMessageContent.put(uuid, message);
    }
}
