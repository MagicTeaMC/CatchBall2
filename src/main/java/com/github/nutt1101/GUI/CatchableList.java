package com.github.nutt1101.GUI;

import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.HeadDrop;
import com.github.nutt1101.utils.TranslationFileReader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CatchableList {
    private List<ItemStack> head = new ArrayList<>();
    private ItemStack prevPage = new ItemStack(Material.PAPER);
    private ItemStack nextPage = new ItemStack(Material.PAPER);
    private ItemStack currentPage = new ItemStack(Material.OAK_SIGN);

    private ItemStack itemSet(ItemStack item, String displayName, int page) {
        displayName = displayName.replace("{PAGE}", String.valueOf(page));
        ItemMeta itemMeta = item.getItemMeta();

        // Convert legacy color codes to Component
        Component displayNameComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(displayName);
        itemMeta.displayName(displayNameComponent);

        item.setItemMeta(itemMeta);
        return item;
    }

    // Check if an entity is catchable by comparing string names instead of EntityType
    private boolean isEntityCatchable(String entityName) {
        // Convert the catchable entity list to string names for comparison
        for (Object catchableEntity : ConfigSetting.catchableEntity) {
            String catchableEntityName;

            // Handle both EntityType objects and string names
            if (catchableEntity instanceof String) {
                catchableEntityName = (String) catchableEntity;
            } else {
                // If it's an EntityType, get its name
                catchableEntityName = catchableEntity.toString();
            }

            if (catchableEntityName.equalsIgnoreCase(entityName)) {
                return true;
            }
        }
        return false;
    }

    public void openCatchableList(Player player, int page) {
        YamlConfiguration entityFile = ConfigSetting.entityFile;
        Set<String> entityList = entityFile.getConfigurationSection("EntityList").getKeys(false);

        // Convert title to Component
        Component inventoryTitle = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(TranslationFileReader.catchableListTitle);
        Inventory catchableInventory = Bukkit.createInventory(player, 54, inventoryTitle);
        head.clear();

        for (String entity : entityList) {
            ItemStack skull = new HeadDrop().skullTextures(new ItemStack(Material.PLAYER_HEAD), entityFile, entity);
            ItemMeta skullMeta = skull.getItemMeta();

            // Create catchable status component
            Component catchableComponent = isEntityCatchable(entity)
                    ? Component.text("TRUE").color(NamedTextColor.GREEN)
                    : Component.text("FALSE").color(NamedTextColor.RED);

            // Set display name using Component
            Component entityDisplayName = Component.text(entity).color(NamedTextColor.WHITE);
            skullMeta.displayName(entityDisplayName);

            // Create lore using Components
            List<Component> loreComponents = new ArrayList<>();
            String entityDisplayNameFromConfig = entityFile.getString("EntityList." + entity + ".DisplayName");

            for (String line : TranslationFileReader.guiSkullLore) {
                // Replace placeholders and convert to Component
                String processedLine = line
                        .replace("{ENTITY}", entityDisplayNameFromConfig)
                        .replace("{CATCHABLE}", isEntityCatchable(entity) ? "&aTRUE" : "&cFALSE");

                Component loreComponent = LegacyComponentSerializer.legacyAmpersand()
                        .deserialize(processedLine);
                loreComponents.add(loreComponent);
            }

            skullMeta.lore(loreComponents);
            skull.setItemMeta(skullMeta);
            head.add(skull);
        }

        int totalHeads = head.size();
        page = Math.min(page, (int) Math.ceil((float) totalHeads / 45.0));
        page = Math.max(page, 1);

        int start = (page - 1) * 45;
        int finish = Math.min(page * 45, totalHeads);

        catchableInventory.addItem(head.subList(start, finish).toArray(new ItemStack[0]));

        catchableInventory.setItem(45, itemSet(prevPage, TranslationFileReader.prevPage, page));
        catchableInventory.setItem(49, itemSet(currentPage, TranslationFileReader.currentPage, page));
        catchableInventory.setItem(53, itemSet(nextPage, TranslationFileReader.nextPage, page));

        player.openInventory(catchableInventory);
    }
}