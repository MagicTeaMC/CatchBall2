package com.github.nutt1101.items;

import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.utils.TranslationFileReader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.List;
import java.util.stream.Collectors;

import static com.github.nutt1101.ConfigSetting.customModelData;

public class Ball {

    public static ItemStack makeBall() {
        ItemStack catchball = new ItemStack(Material.SNOWBALL);

        ItemMeta meta = catchball.getItemMeta();
        meta.displayName(ConfigSetting.toChat(TranslationFileReader.catchBallName, "", ""));
        meta.addEnchant(Enchantment.SOUL_SPEED, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        List<Component> catchBallLoreComponents = TranslationFileReader.catchBallLore.stream()
                .map(lore -> MiniMessage.miniMessage().deserialize(lore))
                .collect(Collectors.toList());

        meta.lore(catchBallLoreComponents);

        if(ConfigSetting.customModelData != 0) {
            CustomModelDataComponent component = meta.getCustomModelDataComponent();
            component.setFloats(List.of((float) ConfigSetting.customModelData));
            meta.setCustomModelDataComponent(component);
        }

        catchball.setItemMeta(meta);

        return catchball;
    }
}
