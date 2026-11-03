package com.github.nutt1101;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.github.nutt1101.command.Command;
import com.github.nutt1101.menu.CatchableMenu;
import com.github.nutt1101.service.EntitySelectionService;
import net.momirealms.sparrow.ui.SparrowUI;
import com.github.nutt1101.event.*;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import me.angeschossen.lands.api.LandsIntegration;
import fr.xyness.SCS.API.SimpleClaimSystemAPI;
import fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider;

import java.util.logging.Level;

public class CatchBall extends JavaPlugin {

    public static Plugin plugin;

    // Plugin availability flags
    public static boolean hasResidence = false;
    public static boolean hasMythicMobs = false;
    public static boolean hasGriefPrevention = false;
    public static boolean hasLands = false;
    public static boolean hasPlaceholderAPI = false;
    public static boolean hasRedProtect = false;
    public static boolean hasSimpleClaimSystem = false;
    public static boolean hasTowny = false;
    public static boolean hasWorldGuard = false;

    // API instances (initialized once at startup)
    public static LandsIntegration landsAPI;
    public static SimpleClaimSystemAPI scsAPI;

    private Lamp<BukkitCommandActor> commands;
    private Metrics metrics;
    private EntitySelectionService selections;
    private CatchableMenu menus;
    private TaskScheduler scheduler;

    public CatchableMenu getMenus() { return menus; }
    public EntitySelectionService getSelections() { return selections; }

    private boolean checkAndInitializePlugin(String pluginName) {
        boolean hasPlugin = this.getServer().getPluginManager().getPlugin(pluginName) != null;
        if (hasPlugin) {
            plugin.getLogger().log(Level.INFO, ChatColor.GREEN + pluginName + " Hook!");
        }
        return hasPlugin;
    }

    @Override
    public void onEnable() {

        plugin = this;
        scheduler = UniversalScheduler.getScheduler(this);

        try {
            ConfigSetting.checkConfig();
        } catch (IllegalArgumentException e) {
            getLogger().severe("Could not load config.yml: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        metrics = new Metrics(this, 12380);
        selections = new EntitySelectionService(ConfigSetting.class,
                () -> new java.util.HashSet<>(ConfigSetting.catchableEntity),
                ConfigSetting::persistEntityList,
                () -> { if (menus != null) menus.refreshSelection(); });
        SparrowUI.getInstance().setUp(this);
        menus = new CatchableMenu(this, selections);

        // Initialize plugin availability flags and APIs
        initializePluginIntegrations();

        registerEvent();
        registerCommand();

    }

    private void initializePluginIntegrations() {
        // Check plugin availability and initialize APIs
        hasResidence = checkAndInitializePlugin("Residence");

        hasMythicMobs = checkAndInitializePlugin("MythicMobs");

        hasGriefPrevention = checkAndInitializePlugin("GriefPrevention");

        hasLands = checkAndInitializePlugin("Lands");
        if (hasLands) {
            try {
                landsAPI = LandsIntegration.of(plugin);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to initialize Lands API: " + e.getMessage());
                hasLands = false;
            }
        }

        hasPlaceholderAPI = checkAndInitializePlugin("PlaceholderAPI");

        hasRedProtect = checkAndInitializePlugin("RedProtect");

        hasSimpleClaimSystem = checkAndInitializePlugin("SimpleClaimSystem");
        if (hasSimpleClaimSystem) {
            try {
                scsAPI = SimpleClaimSystemAPI_Provider.getAPI();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to initialize SimpleClaimSystem API: " + e.getMessage());
                hasSimpleClaimSystem = false;
            }
        }

        hasTowny = checkAndInitializePlugin("Towny");

        hasWorldGuard = checkAndInitializePlugin("WorldGuard");
    }

    @Override
    public void onDisable() {
        if (menus != null) menus.close();
        if (commands != null) commands.unregisterAllCommands();
        // Shutdown metrics
        if (metrics != null) {
            metrics.shutdown();
        }

        // Cancel all tasks registered by this plugin
        if (scheduler != null) scheduler.cancelTasks();
    }

    // register event
    public void registerEvent() {
        PluginManager registerEvent = this.getServer().getPluginManager();
        registerEvent.registerEvents(new HitEvent(), this);
        registerEvent.registerEvents(new EntityDrop(), this);
        registerEvent.registerEvents(new BlockDrop(), this);
        registerEvent.registerEvents(new ChickenDrop(), this);
        registerEvent.registerEvents(new SkullClick(), this);
    }

    // register command
    public void registerCommand() {
        commands = Command.register(this);
    }

    public static String getServerVersion() {
        return plugin.getServer().getBukkitVersion();
    }
}