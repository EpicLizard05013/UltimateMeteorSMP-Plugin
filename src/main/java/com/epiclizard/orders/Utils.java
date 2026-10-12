package com.epiclizard.orders;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Utils {
    public static ItemStack named(Material material, String text) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', text));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static void setName(ItemStack stack, String text) {
        if (stack == null) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', text));
            stack.setItemMeta(meta);
        }
    }

    public static void setLore(ItemStack stack, List<String> lore) {
        if (stack == null) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            List<String> converted = new ArrayList<>();
            for (String line : lore) {
                converted.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(converted);
            stack.setItemMeta(meta);
        }
    }

    public static String cleanName(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return "Air";
        }
        String raw = stack.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] parts = raw.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    public static boolean isIllegalMaterial(Material material) {
        if (material == null) return true;
        String name = material.name().toLowerCase(Locale.ROOT);
        return name.contains("spawn_egg")
            || name.contains("warden")
            || name.contains("dragon_egg")
            || name.contains("bedrock")
            || name.contains("barrier")
            || name.contains("command_block")
            || name.contains("structure")
            || name.contains("debug")
            || name.contains("portal")
            || name.contains("light")
            || name.contains("allow")
            || name.contains("deny")
            || name.contains("border");
    }
}
