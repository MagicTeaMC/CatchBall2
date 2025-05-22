package com.github.nutt1101.items;

import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.utils.TranslationFileReader;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.stream.Collectors;

<<<<<<< HEAD:src/main/java/com/github/nutt1101/items/DropItem.java
public class DropItem {
=======
import static com.github.nutt1101.ConfigSetting.ballCustomModelData;
import static com.github.nutt1101.ConfigSetting.customModelData;

public class GoldEgg {
>>>>>>> dev:src/main/java/com/github/nutt1101/items/GoldEgg.java

    public static ItemStack makeDropItem() {
        ItemStack dropItem = new ItemStack(ConfigSetting.DropItemMaterial);

        ItemMeta meta = dropItem.getItemMeta();
        meta.setDisplayName(ConfigSetting.toChat(TranslationFileReader.dropItemName, "", ""));
        meta.addEnchant(Enchantment.SOUL_SPEED, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        meta.setLore(TranslationFileReader.dropItemLore.stream().map(lore -> ChatColor.
                translateAlternateColorCodes('&', lore).replace("{PERCENT}", String.valueOf(ConfigSetting.
                        DropItemChance))).collect(Collectors.toList()));

<<<<<<< HEAD:src/main/java/com/github/nutt1101/items/DropItem.java
        dropItem.setItemMeta(meta);
=======
        if(ballCustomModelData != 0) {
            meta.setCustomModelData(ballCustomModelData);
        }

        goldEgg.setItemMeta(meta);
>>>>>>> dev:src/main/java/com/github/nutt1101/items/GoldEgg.java

        return dropItem;
    }
}
