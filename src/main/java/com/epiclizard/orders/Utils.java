package com.epiclizard.orders;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class Utils {
    public static ItemStack named(Material m, String name) {
        ItemStack i = new ItemStack(m);
        ItemMeta meta = i.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            i.setItemMeta(meta);
        }
        return i;
    }

    public static void setName(ItemStack i, String name) {
        ItemMeta m = i.getItemMeta();
        if (m != null) { m.setDisplayName(name); i.setItemMeta(m); }
    }

    public static void setLore(ItemStack i, List<String> lore) {
        ItemMeta m = i.getItemMeta();
        if (m != null) { m.setLore(lore); i.setItemMeta(m); }
    }

    public static String cleanName(ItemStack i) {
        String mat = i.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] parts = mat.split(" ");
        for (int j = 0; j < parts.length; j++) parts[j] = parts[j].substring(0,1).toUpperCase() + parts[j].substring(1);
        return String.join(" ", parts);
    }

    public static boolean isIllegalMaterial(Material m) {
        String name = m.name().toLowerCase(Locale.ROOT);
        return name.contains("spawn_egg") || name.contains("warden") || name.contains("dragon_egg") || name.contains("bedrock") || name.contains("barrier") || name.contains("command_block") || name.contains("structure") || name.contains("debug") || name.contains("portal") || name.contains("light") || name.contains("allow") || name.contains("deny") || name.contains("border");
    }
}
