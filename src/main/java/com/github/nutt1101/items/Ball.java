package com.github.nutt1101.items;

import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.utils.TranslationFileReader;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.stream.Collectors;

public class Ball {

    public static ItemStack makeBall() {
        ItemStack catchball = new ItemStack(Material.SNOWBALL);

        ItemMeta meta = catchball.getItemMeta();
        meta.setDisplayName(ConfigSetting.toChat(TranslationFileReader.catchBallName, "", ""));
        meta.addEnchant(Enchantment.SOUL_SPEED, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        meta.setLore(TranslationFileReader.catchBallLore.stream().map(lore -> ChatColor.
                        translateAlternateColorCodes('&', lore)).
                collect(Collectors.toList()));

        catchball.setItemMeta(meta);

        return catchball;
    }
}
