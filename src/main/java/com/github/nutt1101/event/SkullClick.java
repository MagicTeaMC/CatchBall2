package com.github.nutt1101.event;

import cn.handyplus.lib.adapter.PlayerSchedulerUtil;
import com.github.nutt1101.CatchBall;
import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.utils.NBTHandler;
import com.github.nutt1101.utils.TranslationFileReader;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class SkullClick implements Listener{
    private final Plugin plugin = CatchBall.plugin;
    
    @EventHandler
    public void skullClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = (event.getItem() != null) ? event.getItem() : new ItemStack(Material.AIR);
        

        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            if (!item.getType().equals(Material.PLAYER_HEAD)) { return; }

            ItemMeta itemMeta = item.getItemMeta();
            PersistentDataContainer data = itemMeta.getPersistentDataContainer();
            
            if (data.has(new NamespacedKey(plugin, "skullData"), PersistentDataType.STRING)) {
                String path = data.get(new NamespacedKey(plugin, "skullData"), PersistentDataType.STRING).toString();

                if (path == null) {
                    player.sendMessage(ConfigSetting.toChat(TranslationFileReader.skullDoesNotFound, "", ""));
                    event.setCancelled(true);
                } else {
                    if (!new HitEvent().resCheck(player, event.getClickedBlock().getLocation())) {
                        event.setCancelled(true);
                        return;
                    }

                    if (!new HitEvent().gfCheck(player, event.getClickedBlock().getLocation())) {
                        event.setCancelled(true);
                        return;
                    }

                    try {
                        EntityType entityType = EntityType.valueOf(data.get(new NamespacedKey(plugin, "entityType"), PersistentDataType.STRING));
                        Location clickLocation = event.getClickedBlock().getLocation();

                        clickLocation.setX(clickLocation.getBlockX() + 0.5);
                        clickLocation.setZ(clickLocation.getBlockZ() + 0.5);
                        
                        for (int i=0; i < 3; i++) {
                            if (clickLocation.getBlock().getType().equals(Material.AIR) || clickLocation.getBlock().getType().equals(Material.WATER)) { break; }
                            clickLocation.setY(clickLocation.getY() + 1D);
                            
                            if (i == 2) {
                                player.sendMessage(ConfigSetting.toChat(TranslationFileReader.locationUnsafe, "", ""));
                                event.setCancelled(true);
                                return;
                            }
                        }

                        Entity entity = player.getWorld().spawnEntity(clickLocation, entityType);

                        NBTHandler.loadEntityNBT(plugin, entity, data);
                        PlayerSchedulerUtil.teleport(entity, clickLocation);
                        
                        event.getItem().setAmount(0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                }
                
            }
        }
    }
    
}
