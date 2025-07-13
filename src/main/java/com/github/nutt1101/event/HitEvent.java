package com.github.nutt1101.event;

import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import com.bekvon.bukkit.residence.api.ResidenceApi;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.github.nutt1101.CatchBall;
import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.HeadDrop;
import com.github.nutt1101.items.Ball;
import com.github.nutt1101.utils.NBTHandler;
import com.github.nutt1101.utils.TranslationFileReader;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import me.angeschossen.lands.api.land.LandWorld;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.BlockProjectileSource;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;


public class HitEvent implements Listener {
    private List<String> catchableEntity = ConfigSetting.catchableEntity; // Changed from EntityType to String
    private Location hitLocation;
    private final Plugin plugin = CatchBall.plugin;
    private final String[] mmPackage = {"io.lumine.mythic.bukkit.BukkitAPIHelper", "io.lumine.xikage.mythicmobs.api.bukkit.BukkitAPIHelper"};

    /**
     * Check if an entity is catchable by comparing string names
     */
    private boolean isEntityCatchable(EntityType entityType) {
        String entityName = entityType.name();
        return catchableEntity.contains(entityName);
    }

    @EventHandler
    public void CatchBallHitEvent(ProjectileHitEvent event){
        // check if shooter is a player
        if (event.getEntity().getShooter() instanceof Player player) {

            if (!checkCatchBall(event.getEntity())) { return; }

            if (!player.hasPermission("catchball.use")) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigSetting.toChat(TranslationFileReader.noPermissionToUse,
                        getCoordinate(event.getHitBlock() == null ? Objects.requireNonNull(event.getHitEntity()).getLocation() : event.getHitBlock().getLocation())
                        , "").replace("{BALL}", TranslationFileReader.catchBallName)));

                event.getEntity().remove();

                if (event.getHitEntity() != null) {
                    Entity hitEntity = event.getHitEntity();
                    hitLocation = hitEntity.getLocation();
                    event.getHitEntity().getWorld().dropItem(event.getHitEntity().getLocation(), Ball.makeBall());
                } else { event.getHitBlock().getWorld().dropItem(event.getHitBlock().getLocation(), Ball.makeBall()); }

                event.setCancelled(true);

                return;
            }

            event.setCancelled(true);

            event.getEntity().remove();
            // hit a entity
            if (event.getHitEntity() != null) {
                handleEntityCatch(player, event.getHitEntity(), true);
                // hit block, catchBall will be return
            } else if (event.getHitBlock() != null) {

                event.getEntity().remove();

                hitLocation = event.getHitBlock().getLocation();
                player.sendMessage(ConfigSetting.toChat(TranslationFileReader.ballHitBlock, getCoordinate(hitLocation), ""));

                event.getHitBlock().getWorld().dropItem(event.getHitBlock().getLocation(), Ball.makeBall());
                return;
            }

        } else if (event.getEntity().getShooter() instanceof BlockProjectileSource){

            if (!checkCatchBall(event.getEntity())) { return; }

            event.setCancelled(true);
            event.getEntity().remove();
            // hit a entity
            if (event.getHitEntity() != null) {
                Entity hitEntity = event.getHitEntity();
                hitLocation = hitEntity.getLocation();

                String checkCustom = getIsCustomEntity(hitEntity);

                // Use string comparison instead of EntityType comparison
                if (isEntityCatchable(hitEntity.getType()) && !(hitEntity instanceof Player) && !checkCustom.equals("CUSTOM")) {
                    hitEntity.remove();
                    hitEntity.getWorld().dropItem(hitLocation, new HeadDrop().getEntityHead(event.getHitEntity(), null));
                    return;
                }

                hitEntity.getWorld().dropItem(hitLocation, Ball.makeBall());

            } else if (event.getHitBlock() != null) {
                hitLocation = event.getHitBlock().getLocation();
                event.getHitBlock().getWorld().dropItem(event.getHitBlock().getLocation(), Ball.makeBall());
                return;
            }
        }

        return;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity targetEntity = event.getRightClicked();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // Check if player is holding a catch ball
        if (!isCatchBall(itemInHand)) {
            return;
        }

        // Check permissions
        if (!player.hasPermission("catchball.use")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigSetting.toChat(TranslationFileReader.noPermissionToUse,
                    getCoordinate(targetEntity.getLocation()), "").replace("{BALL}", TranslationFileReader.catchBallName)));
            return;
        }

        event.setCancelled(true);

        // Remove one catch ball from inventory
        if (itemInHand.getAmount() > 1) {
            itemInHand.setAmount(itemInHand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }

        // Handle the entity catch
        handleEntityCatch(player, targetEntity, false);
    }

    private void handleEntityCatch(Player player, Entity hitEntity, boolean isProjectile) {
        hitLocation = hitEntity.getLocation();

        // Check all protection plugins using static flags
        if (!resCheck(player, hitEntity.getLocation()) && ConfigSetting.UseRes) {
            hitEntity.getWorld().dropItem(hitEntity.getLocation(), Ball.makeBall());
            player.sendMessage(ConfigSetting.toChat(TranslationFileReader.canNotCatchable, getCoordinate(hitEntity.getLocation()), ""));
            return;
        }

        if (!mmCheck(player, hitEntity) && ConfigSetting.UseMM) {
            hitEntity.getWorld().dropItem(hitEntity.getLocation(), Ball.makeBall());
            player.sendMessage(ConfigSetting.toChat(TranslationFileReader.canNotCatchable, getCoordinate(hitEntity.getLocation()), ""));
            return;
        }

        if (!gfCheck(player, hitEntity.getLocation()) && ConfigSetting.UseGF) {
            hitEntity.getWorld().dropItem(hitEntity.getLocation(), Ball.makeBall());
            player.sendMessage(ConfigSetting.toChat(TranslationFileReader.canNotCatchable, getCoordinate(hitEntity.getLocation()), ""));
            return;
        }

        if (!landsCheck(player, hitEntity.getLocation()) && ConfigSetting.UseLands) {
            hitEntity.getWorld().dropItem(hitEntity.getLocation(), Ball.makeBall());
            player.sendMessage(ConfigSetting.toChat(TranslationFileReader.canNotCatchable, getCoordinate(hitEntity.getLocation()), ""));
            return;
        }

        if (!rpCheck(player, hitEntity.getLocation()) && ConfigSetting.UseRP) {
            hitEntity.getWorld().dropItem(hitEntity.getLocation(), Ball.makeBall());
            player.sendMessage(ConfigSetting.toChat(TranslationFileReader.canNotCatchable, getCoordinate(hitEntity.getLocation()), ""));
            return;
        }

        if (!scsCheck(player, hitEntity.getLocation()) && ConfigSetting.UseSCS) {
            hitEntity.getWorld().dropItem(hitEntity.getLocation(), Ball.makeBall());
            player.sendMessage(ConfigSetting.toChat(TranslationFileReader.canNotCatchable, getCoordinate(hitEntity.getLocation()), ""));
            return;
        }

        if (!townyCheck(player, hitEntity.getLocation()) && ConfigSetting.UseTowny) {
            hitEntity.getWorld().dropItem(hitEntity.getLocation(), Ball.makeBall());
            player.sendMessage(ConfigSetting.toChat(TranslationFileReader.canNotCatchable, getCoordinate(hitEntity.getLocation()), ""));
            return;
        }

        // TODO: Uncomment when WorldGuard check is implemented
        /*if (!wgCheck(player, hitEntity.getLocation()) && ConfigSetting.UseWG) {
            hitEntity.getWorld().dropItem(hitEntity.getLocation(), Ball.makeBall());
            player.sendMessage(ConfigSetting.toChat(TranslationFileReader.canNotCatchable, getCoordinate(hitEntity.getLocation()), ""));
            return;
        }*/

        // Check if entity is tameable and owned by someone else
        if (hitEntity instanceof Tameable tameable) {
            if (tameable.isTamed()) {
                boolean isNullOwnerValue = tameable.getOwner() == null;
                boolean sameOwner = isNullOwnerValue ? true : tameable.getOwner().getName().equals(player.getName());
                if ((isNullOwnerValue && !ConfigSetting.allowCatchableTamedOwnerIsNull) || !sameOwner) {
                    hitEntity.getWorld().dropItem(hitEntity.getLocation(), Ball.makeBall());
                    player.sendMessage(ConfigSetting.toChat(TranslationFileReader.canNotCatchable, getCoordinate(hitEntity.getLocation()), ""));
                    return;
                }
            }
        }

        String checkCustom = getIsCustomEntity(hitEntity);

        // Use string comparison instead of EntityType comparison
        if (isEntityCatchable(hitEntity.getType()) && !(hitEntity instanceof Player) && !checkCustom.equals("CUSTOM")) {
            // Check catch failure rate
            if(Math.random() < ConfigSetting.catchFailRate) {
                hitEntity.getWorld().dropItem(hitLocation, Ball.makeBall());
                player.sendMessage(ConfigSetting.toChat(TranslationFileReader.catchFail, getCoordinate(hitLocation), hitEntity.getType().name()));
                return;
            }

            // Success sound
            if (!(ConfigSetting.catchSuccessSound.equals("FALSE"))) {
                player.playSound(player.getLocation(), Sound.valueOf(ConfigSetting.catchSuccessSound), 1f, 1f);
            }

            // Remove the entity and drop the head
            hitEntity.remove();
            hitEntity.getWorld().dropItem(hitLocation, new HeadDrop().getEntityHead(hitEntity, player));

            // Show particles
            if (ConfigSetting.ShowParticles) {
                hitEntity.getWorld().spawnParticle(Particle.valueOf(ConfigSetting.CustomParticles), hitLocation, 1);
            }

            player.sendMessage(ConfigSetting.toChat(TranslationFileReader.catchSuccess, getCoordinate(hitLocation), hitEntity.getType().name()));
            return;
        }

        // If entity cannot be caught, return catch ball
        player.sendMessage(ConfigSetting.toChat(TranslationFileReader.canNotCatchable, getCoordinate(hitLocation), ""));
        hitEntity.getWorld().dropItem(hitLocation, Ball.makeBall());
    }

    private boolean isCatchBall(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        ItemStack catchBall = Ball.makeBall();
        return item.isSimilar(catchBall);
    }

    // config text will be use this method , so put on this class
    public static String getCoordinate(Location location) {

        return location.getBlockX() + ", " +
                location.getBlockY() + ", " +
                location.getBlockZ();
    }

    public boolean resCheck(Player player, Location location) {
        if (!CatchBall.hasResidence) { return true; }

        if (ResidenceApi.getResidenceManager().getByLoc(location) == null) { return true; }

        ClaimedResidence residence = ResidenceApi.getResidenceManager().getByLoc(location);

        if (residence.getOwnerUUID().equals(player.getUniqueId()) || player.isOp() || player.hasPermission("catchball.op")) { return true; }

        for (String flags : ConfigSetting.residenceFlag) {
            if (!residence.getPermissions().playerHas(player, Flags.valueOf(flags.toLowerCase()) , true)) {

                player.sendMessage(ConfigSetting.toChat(TranslationFileReader.noResidencePermissions, "", "").
                        replace("{FLAG}", flags));

                return false;
            }
        }

        return true;
    }

    public boolean mmCheck(Player player, Entity entity) {
        if (!CatchBall.hasMythicMobs) { return true; }

        for (int i=0 ; i < 2; i++) {
            try {
                Class<?> api = Class.forName(mmPackage[i]);
                Object ins = api.getConstructor().newInstance();

                Method isMythicMob = api.getDeclaredMethod("isMythicMob", Entity.class);

                return !((boolean) isMythicMob.invoke(ins, entity));
            } catch (Exception e) {
            }
        }

        return true;
    }

    public boolean landsCheck(Player player, Location location) {
        if (!CatchBall.hasLands) { return true; }

        LandWorld world = CatchBall.landsAPI.getWorld(hitLocation.getWorld());

        if (world != null) { // Lands is enabled in this world
            if (world.hasFlag(player, hitLocation, null, me.angeschossen.lands.api.flags.Flags.ATTACK_ANIMAL, false)) {
                return true;
            } else {
                return false;
            }
        }

        return true;
    }

    public boolean gfCheck(Player player, Location location) {
        if (!CatchBall.hasGriefPrevention) {return true;}

        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
        if (claim == null) {
            return true;
        }

        java.util.UUID ownerId = claim.getOwnerID();
        if ((ownerId != null && ownerId.equals(player.getUniqueId())) ||
                player.hasPermission("catchball.op") || player.isOp()) {
            return true;
        }

        for (String flags : ConfigSetting.griefPreventionFlag) {
            if (!claim.hasExplicitPermission(player, ClaimPermission.valueOf(flags))) {
                player.sendMessage(ConfigSetting.toChat(
                                TranslationFileReader.noResidencePermissions, "", "")
                        .replace("{FLAG}", flags));

                return false;
            }
        }
        return true;
    }

    public boolean rpCheck(Player player, Location location) {
        if (!CatchBall.hasRedProtect) { return true; }
        Region r = RedProtect.get().getAPI().getRegion(player.getLocation());
        return r != null && r.canSpawnPassives(player);
    }

    public boolean scsCheck(Player player, Location location) {
        if (!CatchBall.hasSimpleClaimSystem) { return true; }

        fr.xyness.SCS.Types.Claim claim = CatchBall.scsAPI.getClaimAtChunk(getChunkFromLocation(location));
        if (claim != null) {
            return claim.getPermission(player.getName(), null);
        }
        return true;
    }

    public Chunk getChunkFromLocation(Location location) {
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        return location.getWorld().getChunkAt(chunkX, chunkZ);
    }

    // TODO
    /* public boolean wgCheck(Player player, Location location) {
        if (!CatchBall.hasWorldGuard) {
            return true;
        }

        LocalPlayer localPlayer = (LocalPlayer) WorldGuard.getInstance().getPlatform().getSessionManager().get((LocalPlayer) player);
        ApplicableRegionSet regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(location.getWorld())).getApplicableRegions(BukkitAdapter.asBlockVector(location));

        return regions.queryState(localPlayer, com.sk89q.worldguard.protection.flags.Flags.DAMAGE_ANIMALS) == StateFlag.State.ALLOW;
    } */

    public boolean townyCheck(Player player, Location location) {
        if (!CatchBall.hasTowny) { return true; }
        boolean bBuild = PlayerCacheUtil.getCachePermission(player, location, Material.DIRT, TownyPermission.ActionType.BUILD);
        return bBuild;
    }

    public boolean checkCatchBall(Projectile projectile) {
        if (!(projectile instanceof Snowball)) { return false; }

        if (projectile instanceof ThrowableProjectile) {
            ThrowableProjectile throwableProjectile = (ThrowableProjectile) projectile;
            if (!Objects.requireNonNull(throwableProjectile.getItem().getItemMeta()).equals(Ball.makeBall().getItemMeta())) { return false; }
        }

        return true;
    }

    public String getIsCustomEntity(Entity hitEntity) {
        String checkCustom = null;

        checkCustom = NBTHandler.isCustomEntity(hitEntity);

        return checkCustom;
    }

}
