package com.github.nutt1101;

import com.github.nutt1101.event.HitEvent;
import com.github.nutt1101.utils.NBTHandler;
import com.github.nutt1101.utils.TranslationFileReader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class HeadDrop {
    private final Plugin plugin = CatchBall.plugin;
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss");
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();

    /**
     * When CatchBall hit catchable entity, It will drop the skull of hitEntity.
     * @param hitEntity the hitEntity of the hit event
     * @param player player who throw the CatchBall
     * @return the skull of saved hitEntity info
     */
    public ItemStack getEntityHead(Entity hitEntity, Player player) {
        YamlConfiguration entityFile = ConfigSetting.entityFile;

        ItemStack entityHead = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta headMeta = entityHead.getItemMeta();

        String hitEntityUuid = hitEntity.getUniqueId().toString();
        PersistentDataContainer headData = headMeta.getPersistentDataContainer();
        headData.set(new NamespacedKey(plugin, "skullData"), PersistentDataType.STRING, hitEntityUuid);

        Date now = new Date();
        String location = "(" + hitEntity.getWorld().getName() + ") " +
                HitEvent.getCoordinate(hitEntity.getLocation());

        // Use Adventure API for display name
        Component displayName;
        if (hitEntity.customName() != null) {
            displayName = Objects.requireNonNull(hitEntity.customName()).color(NamedTextColor.WHITE);
        } else {
            String entityDisplayName = entityFile.getString("EntityList." + hitEntity.getType().toString() + ".DisplayName");
            displayName = Component.text(entityDisplayName != null ? entityDisplayName : hitEntity.getType().toString())
                    .color(NamedTextColor.WHITE);
        }
        headMeta.displayName(displayName);

        List<Component> headLore = new ArrayList<>();

        String playerName = (player == null) ? "Dispenser" : player.getName();

        for (String lore : TranslationFileReader.dropSkullLore) {
            String processedLore = lore
                    .replace("{ENTITY}", hitEntity.getType().toString())
                    .replace("{PLAYER}", playerName)
                    .replace("{TIME}", format.format(now))
                    .replace("{LOCATION}", location);

            Component loreComponent = legacySerializer.deserialize(processedLore);
            headLore.add(loreComponent);
        }

        headMeta = NBTHandler.saveEntityNBT(plugin, hitEntity, headMeta);

        headMeta.lore(headLore);
        entityHead.setItemMeta(headMeta);

        return skullTextures(entityHead, entityFile, hitEntity.getType().toString());
    }

    /**
     * Get the entity.yml file entity skull textures.
     * @param head skull of hitEntity
     * @param entityFile entity.yml file
     * @param entityType entityType
     * @return head with texture value
     */
    public ItemStack skullTextures(ItemStack head, YamlConfiguration entityFile, String entityType) {
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

        try {
            String textureValue = entityFile.getString("EntityList." + entityType.toUpperCase() + ".Skull");

            if (textureValue != null && !textureValue.isEmpty()) {
                try {
                    // Create Paper's PlayerProfile
                    PlayerProfile profile = Bukkit.createProfile("catchball");

                    // Set the texture property directly
                    ProfileProperty textureProperty = new ProfileProperty("textures", textureValue);
                    profile.setProperty(textureProperty);

                    skullMeta.setPlayerProfile(profile);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to set texture property: " + e.getMessage());
                }
            } else {
                plugin.getLogger().log(Level.WARNING, "Could not find texture value for entity type: " + entityType);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to set skull texture: " + e.getMessage());
        }

        head.setItemMeta(skullMeta);
        return head;
    }
}