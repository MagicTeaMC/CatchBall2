package com.github.nutt1101;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.github.nutt1101.event.HitEvent;
import com.github.nutt1101.utils.NBTHandler;
import com.github.nutt1101.utils.TranslationFileReader;

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
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import net.md_5.bungee.api.ChatColor;

public class HeadDrop {
    private final Plugin plugin = CatchBall.plugin;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss");

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

        if (hitEntity.getCustomName() != null) {
            headMeta.setDisplayName(ChatColor.WHITE + hitEntity.getCustomName());
        } else {
            headMeta.setDisplayName(ChatColor.WHITE + entityFile.getString("EntityList." + hitEntity.getType().toString() + ".DisplayName"));
        }

        List<String> headLore = new ArrayList<>();

        if (player == null) {
            headLore.addAll(TranslationFileReader.dropSkullLore.stream().map(lore -> ChatColor.translateAlternateColorCodes('&', lore).
                    replace("{ENTITY}", hitEntity.getType().toString()).replace("{PLAYER}", "Dispenser").replace("{TIME}", format.format(now)).
                    replace("{LOCATION}", location)).collect(Collectors.toList()));
        } else {
            headLore.addAll(TranslationFileReader.dropSkullLore.stream().map(lore -> ChatColor.translateAlternateColorCodes('&', lore).
                    replace("{ENTITY}", hitEntity.getType().toString()).replace("{PLAYER}", player.getName()).replace("{TIME}", format.format(now)).
                    replace("{LOCATION}", location)).collect(Collectors.toList()));
        }

        headMeta = NBTHandler.saveEntityNBT(plugin, hitEntity, headMeta);

        headMeta.setLore(headLore);
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
            PlayerProfile profile = Bukkit.createPlayerProfile("catchball");
            PlayerTextures textures = profile.getTextures();

            String textureValue = entityFile.getString("EntityList." + entityType.toUpperCase() + ".Skull");

            if (textureValue != null && !textureValue.isEmpty()) {
                try {
                    String decodedValue = new String(Base64.getDecoder().decode(textureValue));

                    String urlStr = decodedValue.split("\"url\":\"")[1].split("\"")[0];
                    URL textureUrl = new URL(urlStr);

                    textures.setSkin(textureUrl);
                    profile.setTextures(textures);

                    skullMeta.setOwnerProfile(profile);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to decode texture value: " + e.getMessage());
                }
            } else {
                plugin.getLogger().log(Level.WARNING, "Could not find texture value for entity type: " + entityType);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to set skull texture: " + e.getMessage());
            e.printStackTrace();
        }

        head.setItemMeta(skullMeta);
        return head;
    }
}