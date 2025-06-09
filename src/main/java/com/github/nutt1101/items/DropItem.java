package com.github.nutt1101.items;

import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.utils.TranslationFileReader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.inventory.meta.components.CustomModelDataComponent;

public class DropItem {

    public static ItemStack makeDropItem() {
        ItemStack dropItem = new ItemStack(ConfigSetting.DropItemMaterial);

        ItemMeta meta = dropItem.getItemMeta();
        meta.displayName(ConfigSetting.toChat(TranslationFileReader.dropItemName, "", ""));
        meta.addEnchant(Enchantment.SOUL_SPEED, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        List<Component> loreComponents = TranslationFileReader.dropItemLore.stream()
                .map(lore -> lore.replace("{PERCENT}", String.valueOf(ConfigSetting.DropItemChance)))
                .map(lore -> MiniMessage.miniMessage().deserialize(lore))
                .collect(Collectors.toList());

        meta.lore(loreComponents);

        if(ConfigSetting.ballCustomModelData != 0) {
            CustomModelDataComponent component = meta.getCustomModelDataComponent();
            component.setFloats(List.of((float) ConfigSetting.ballCustomModelData));
            meta.setCustomModelDataComponent(component);
        }

        dropItem.setItemMeta(meta);

        return dropItem;
    }
}
