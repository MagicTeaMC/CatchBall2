package com.github.nutt1101.utils;

import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTEntity;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class NBTHandler {

    public static ItemMeta saveEntityNBT(Plugin plugin, Entity hitEntity, ItemMeta headMeta) {
        NBTEntity nbtEntity = new NBTEntity(hitEntity);
        String nbtData = nbtEntity.toString();

        headMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "entity"),
                PersistentDataType.STRING,
                nbtData
        );

        headMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "entityType"),
                PersistentDataType.STRING,
                hitEntity.getType().toString()
        );

        return headMeta;
    }

    public static void loadEntityNBT(Plugin plugin, Entity entity, PersistentDataContainer data) {
        try {
            String nbtString = data.get(new NamespacedKey(plugin, "entity"), PersistentDataType.STRING);
            if (nbtString != null) {
                NBTContainer nbtContainer = new NBTContainer(nbtString);
                NBTEntity nbtEntity = new NBTEntity(entity);
                nbtEntity.mergeCompound(nbtContainer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String isCustomEntity(Entity hitEntity) {
        NBTEntity nbtEntity = new NBTEntity(hitEntity);
        return nbtEntity.getString("Paper.SpawnReason");
    }
}
