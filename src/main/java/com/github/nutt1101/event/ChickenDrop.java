package com.github.nutt1101.event;

import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.items.DropItem;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class ChickenDrop implements Listener {
    private EntityType chicken = EntityType.CHICKEN;
    private Random chance = new Random();

    @EventHandler
    public void ChickenDropEgg(EntityDropItemEvent event) {
        if (ConfigSetting.DropMethod != ConfigSetting.DropMethodType.CHICKEN) { return; }
        if (!event.getItemDrop().getItemStack().equals(new ItemStack(Material.EGG))) { return; }

        ConfigSetting.DropItemChance = Math.min(ConfigSetting.DropItemChance, 100);

        if (event.getEntityType().equals(chicken)) {
            if (chance.nextInt(99) < ConfigSetting.DropItemChance) {
                event.setCancelled(true);
                event.getEntity().getWorld().dropItem(event.getEntity().getLocation(), DropItem.makeDropItem());
            }
        }
    }
}
