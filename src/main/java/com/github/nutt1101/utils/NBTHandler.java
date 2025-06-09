package com.github.nutt1101.utils;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class NBTHandler {

    public static ItemMeta saveEntityNBT(Plugin plugin, Entity hitEntity, ItemMeta headMeta) {
        // Get NBT data from entity using NBT.modify to read the data
        final String[] nbtData = new String[1];
        NBT.modify(hitEntity, nbt -> {
            nbtData[0] = nbt.toString();
        });

        // Store NBT data in item's persistent data container
        headMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "entity"),
                PersistentDataType.STRING,
                nbtData[0]
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
                // Parse NBT string and apply to entity
                ReadWriteNBT parsedNBT = NBT.parseNBT(nbtString);
                NBT.modify(entity, nbt -> {
                    nbt.mergeCompound(parsedNBT);
                });

                // Handle age-specific logic for Ageable entities
                if (entity instanceof Ageable ageableEntity) {
                    boolean isBaby = parsedNBT.hasTag("IsBaby") && parsedNBT.getBoolean("IsBaby");
                    if (isBaby) {
                        ageableEntity.setBaby();
                    } else {
                        ageableEntity.setAdult();
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error loading entity NBT: " + e.getMessage());
        }
    }

    public static String isCustomEntity(Entity hitEntity) {
        // Read NBT data from entity to check spawn reason
        final String[] spawnReason = new String[1];
        NBT.modify(hitEntity, nbt -> {
            spawnReason[0] = nbt.getString("Paper.SpawnReason");
        });
        return spawnReason[0];
    }
}