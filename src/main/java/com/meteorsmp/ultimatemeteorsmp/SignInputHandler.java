package com.meteorsmp.ultimatemeteorsmp;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class SignInputHandler implements Listener {
    private static final Map<UUID, Consumer<String>> pendingInputs = new HashMap<>();

    public static void openSignPrompt(Player player, String promptLine, Consumer<String> callback) {
        pendingInputs.put(player.getUniqueId(), callback);
        
        // Temporarily place a sign at player's location or open sign GUI
        Block block = player.getLocation().getBlock();
        Material original = block.getType();
        block.setType(Material.OAK_SIGN);
        
        player.openSign((org.bukkit.block.Sign) block.getState());
        
        // Revert block after 2 ticks
        Bukkit.getScheduler().runTaskLater(UltimateMeteorSMP.getPlugin(UltimateMeteorSMP.class), () -> {
            block.setType(original);
        }, 2L);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (pendingInputs.containsKey(player.getUniqueId())) {
            Consumer<String> callback = pendingInputs.remove(player.getUniqueId());
            String input = event.getLine(0);
            if (input == null || input.equalsIgnoreCase("cancel")) {
                input = "";
            }
            callback.accept(input);
        }
    }
}
