package com.github.nutt1101.items;

import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.utils.TranslationFileReader;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


import static com.github.nutt1101.ConfigSetting.customModelData;

public class Ball {

    public static ItemStack makeBall() {
        ItemStack catchball = new ItemStack(Material.SNOWBALL);

        ItemMeta meta = catchball.getItemMeta();
        meta.displayName(ConfigSetting.toChat(TranslationFileReader.catchBallName, "", ""));
        meta.addEnchant(Enchantment.SOUL_SPEED, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        meta.lore(TranslationFileReader.catchBallLore.stream()
                .map(line -> com.github.nutt1101.utils.MessageText.render(line, java.util.Map.of())).toList());

        if(customModelData != 0) {
            meta.setCustomModelData(customModelData);
        }

        catchball.setItemMeta(meta);

        return catchball;
    }
}
