package com.github.nutt1101;

import cn.handyplus.lib.adapter.HandySchedulerUtil;
import com.github.nutt1101.command.Command;
import com.github.nutt1101.command.TabComplete;
import com.github.nutt1101.event.*;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import me.angeschossen.lands.api.LandsIntegration;
import fr.xyness.SCS.API.SimpleClaimSystemAPI;
import fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider;

import java.util.logging.Level;

public class CatchBall extends JavaPlugin {
    private FileConfiguration config = this.getConfig();

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

    private Metrics metrics;

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

        ConfigSetting.checkConfig();

        Metrics metrics = new Metrics(this, 12380);

        // Initialize plugin availability flags and APIs
        initializePluginIntegrations();

        registerEvent();
        registerCommand();


        HandySchedulerUtil.init(this);
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
        // Shutdown metrics
        if (metrics != null) {
            metrics.shutdown();
        }

        // Cancel all tasks registered by this plugin
        getServer().getScheduler().cancelTasks(this);
    }

    // register event
    public void registerEvent() {
        PluginManager registerEvent = this.getServer().getPluginManager();
        registerEvent.registerEvents(new HitEvent(), this);
        if (ConfigSetting.DropEnable) {
            registerEvent.registerEvents(new EntityDrop(), this);
            registerEvent.registerEvents(new BlockDrop(), this);
            registerEvent.registerEvents(new ChickenDrop(), this);
        }
        registerEvent.registerEvents(new SkullClick(), this);
        registerEvent.registerEvents(new GUIClick(), this);
    }

    // register command
    public void registerCommand() {
        PluginCommand ctbCommand = this.getCommand("ctb");
        if (ctbCommand != null) {
            ctbCommand.setExecutor(new Command());
            ctbCommand.setTabCompleter(new TabComplete());
        }
    }

    public static String getServerVersion() {
        return plugin.getServer().getBukkitVersion();
    }
}