package com.github.nutt1101.items;

import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.utils.TranslationFileReader;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.stream.Collectors;

public class DropItem {

    public static ItemStack makeDropItem() {
        ItemStack dropItem = new ItemStack(ConfigSetting.DropItemMaterial);

        ItemMeta meta = dropItem.getItemMeta();
        meta.setDisplayName(ConfigSetting.toChat(TranslationFileReader.dropItemName, "", ""));
        meta.addEnchant(Enchantment.SOUL_SPEED, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        meta.setLore(TranslationFileReader.dropItemLore.stream().map(lore -> ChatColor.
                translateAlternateColorCodes('&', lore).replace("{PERCENT}", String.valueOf(ConfigSetting.
                        DropItemChance))).collect(Collectors.toList()));

        dropItem.setItemMeta(meta);

        return dropItem;
    }
}
