package com.github.nutt1101.event;

import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.GUI.CatchableList;
import com.github.nutt1101.utils.TranslationFileReader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class GUIClick implements Listener {
    List<String> savelist = new ArrayList<>();

    // Serializers for Component conversion
    private final PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();

    @EventHandler
    public void guiClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Component title = ConfigSetting.toChat(TranslationFileReader.catchableListTitle, "", "");

        if (event.getView().title().equals(title)) {
            event.setCancelled(true);

            if (event.getClickedInventory() == null) { return; }

            if (event.getClickedInventory().equals(player.getInventory())) { return; }

            // Convert Component to plain text for string manipulation
            String displayNameText = plainSerializer.serialize(
                    Objects.requireNonNull(Objects.requireNonNull(event.getClickedInventory().getItem(49)).getItemMeta().displayName())
            );

            int page = Integer.parseInt(displayNameText
                    .replace(" ", "").split(Pattern.quote(":") + "|" + Pattern.quote("\uff1a"))[1]);

            switch (event.getSlot()) {
                case 45:
                    new CatchableList().openCatchableList(player, page - 1);
                    player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
                    break;
                case 53:
                    new CatchableList().openCatchableList(player, page + 1);
                    player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
                    break;
                default:
                    if (event.getClickedInventory().getItem(event.getSlot()) != null &&
                            event.getClickedInventory().getItem(event.getSlot()).getType().equals(Material.PLAYER_HEAD)) {

                        ItemStack clickItem = event.getClickedInventory().getItem(event.getSlot());
                        assert clickItem != null;
                        ItemMeta clickItemMeta = clickItem.getItemMeta();
                        List<Component> lore = clickItemMeta.lore();

                        if (lore == null) {
                            lore = new ArrayList<>();
                        }

                        // Convert display name to plain text for EntityType parsing
                        String entityName = plainSerializer.serialize(Objects.requireNonNull(clickItem.getItemMeta().displayName()));
                        EntityType entityType = EntityType.valueOf(entityName);

                        int loreIndex = getLoreIndex(lore, "{CATCHABLE}");

                        if (ConfigSetting.catchableEntity.contains(entityType)) {
                            ConfigSetting.catchableEntity.remove(entityType);

                            // Create new component with updated text
                            Component originalComponent = ConfigSetting.toChat(TranslationFileReader.guiSkullLore.get(loreIndex), "", "");
                            String componentText = plainSerializer.serialize(originalComponent);
                            String updatedText = componentText.replace("{CATCHABLE}", "&cFALSE");
                            Component updatedComponent = legacySerializer.deserialize(updatedText);

                            lore.set(loreIndex, updatedComponent);

                        } else {
                            ConfigSetting.catchableEntity.add(String.valueOf(entityType));

                            // Create new component with updated text
                            Component originalComponent = ConfigSetting.toChat(TranslationFileReader.guiSkullLore.get(loreIndex), "", "");
                            String componentText = plainSerializer.serialize(originalComponent);
                            String updatedText = componentText.replace("{CATCHABLE}", "&aTRUE");
                            Component updatedComponent = legacySerializer.deserialize(updatedText);

                            lore.set(loreIndex, updatedComponent);
                        }

                        clickItemMeta.lore(lore);
                        clickItem.setItemMeta(clickItemMeta);
                        player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 1.0f);

                        ConfigSetting.saveEntityList();
                        break;
                    }
            }
        }
    }

    public int getLoreIndex(List<Component> lore, String contain) {
        for (int i = 0; i < lore.size(); i++) {
            String loreText = plainSerializer.serialize(lore.get(i));
            if (loreText.contains(contain)) {
                return i;
            }
        }
        return 1;
    }
}