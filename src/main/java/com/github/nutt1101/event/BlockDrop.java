package com.github.nutt1101.event;

import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.items.DropItem;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Random;

public class BlockDrop implements Listener {
    private final Random chance = new Random();

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Check config setting drop method
        if (ConfigSetting.DropMethod != ConfigSetting.DropMethodType.BLOCK) {
            return;
        }
        // Check player permission
        if (ConfigSetting.DropNeedPermission) {
            // Not have permission, return
            if (!event.getPlayer().hasPermission("catchball.get.block")) {
                return;
            }
        }

        // Check player gamemode, if creative mode, return
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ConfigSetting.DropItemChance = Math.min(ConfigSetting.DropItemChance, 100);

        if (event.getBlock().getType() == ConfigSetting.DropBlockType) {
            if (chance.nextInt(99) < ConfigSetting.DropItemChance) {
                event.setDropItems(false);
                event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), DropItem.makeDropItem());
            }
        }
    }
}
