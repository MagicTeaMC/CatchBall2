package com.github.nutt1101;

import com.github.nutt1101.event.HitEvent;
import com.github.nutt1101.utils.NBTHandler;
import com.github.nutt1101.utils.TranslationFileReader;
import net.md_5.bungee.api.ChatColor;
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

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

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
        String texture = entityFile.getString("EntityList." + entityType.toUpperCase(java.util.Locale.ROOT) + ".Skull");
        if (texture == null || texture.isBlank()) return head;
        try {
            // Paper's public profile API works across our supported versions. XSkull 14's
            // reflection fails on Paper 1.21.4, so do not route textures through NMS.
            String json = new String(Base64.getDecoder().decode(texture), java.nio.charset.StandardCharsets.UTF_8);
            String url = com.google.gson.JsonParser.parseString(json).getAsJsonObject()
                    .getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
            PlayerProfile profile = Bukkit.createPlayerProfile("catchball");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(java.net.URI.create(url).toURL());
            profile.setTextures(textures);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwnerProfile(profile);
            head.setItemMeta(meta);
        } catch (java.io.IOException | RuntimeException error) {
            plugin.getLogger().log(Level.WARNING, "Could not apply texture for " + entityType, error);
        }
        return head;
    }
}
