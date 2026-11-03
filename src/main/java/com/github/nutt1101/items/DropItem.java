package com.github.nutt1101.items;

import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.utils.TranslationFileReader;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


import static com.github.nutt1101.ConfigSetting.ballCustomModelData;

public class DropItem {

    public static ItemStack makeDropItem() {
        ItemStack dropItem = new ItemStack(ConfigSetting.DropItemMaterial);

        ItemMeta meta = dropItem.getItemMeta();
        meta.displayName(ConfigSetting.toChat(TranslationFileReader.dropItemName, "", ""));
        meta.addEnchant(Enchantment.SOUL_SPEED, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        meta.lore(TranslationFileReader.dropItemLore.stream()
                .map(line -> com.github.nutt1101.utils.MessageText.render(line,
                        java.util.Map.of("percent", String.valueOf(ConfigSetting.DropItemChance)))).toList());

        if(ballCustomModelData != 0) {
            meta.setCustomModelData(ballCustomModelData);
        }

        dropItem.setItemMeta(meta);

        return dropItem;
    }
}
