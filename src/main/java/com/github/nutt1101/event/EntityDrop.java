package com.github.nutt1101.event;

import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.items.GoldEgg;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Random;


public class EntityDrop implements Listener{
    private final Random chance = new Random();

    @EventHandler
    public void onEntityDrop(EntityDeathEvent event) {
        // Check config setting drop method
        if (ConfigSetting.DropMethod != ConfigSetting.DropMethodType.ENTITY) {
            return;
        }
        // Check player kill entity
        if (event.getEntity().getKiller() == null) {
            return;
        }
        // Check player permission
        if (ConfigSetting.DropNeedPermission) {
            // Not have permission, return
            if (!event.getEntity().getKiller().hasPermission("catchball.get.entity")) {
                return;
            }
        }

        ConfigSetting.DropGoldEggChance = Math.min(ConfigSetting.DropGoldEggChance, 100);
        if (event.getEntityType().equals(ConfigSetting.DropEntityType)) {
            if (chance.nextInt(99) < ConfigSetting.DropGoldEggChance) {
                event.getDrops().clear();
                event.getEntity().getWorld().dropItem(event.getEntity().getLocation(), GoldEgg.makeGoldEgg());
            }
        }
    }
}
