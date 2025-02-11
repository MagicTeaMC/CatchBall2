package com.github.nutt1101.event;

import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.items.GoldEgg;
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

        ConfigSetting.DropGoldEggChance = Math.min(ConfigSetting.DropGoldEggChance, 100);

        if (event.getBlock().getType() == ConfigSetting.DropBlockType) {
            if (chance.nextInt(99) < ConfigSetting.DropGoldEggChance) {
                event.setDropItems(false);
                event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), GoldEgg.makeGoldEgg());
            }
        }
    }
}
