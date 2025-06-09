package com.github.nutt1101;

import cn.handyplus.lib.adapter.HandySchedulerUtil;
import com.github.nutt1101.command.BrigadierCommandHandler;
import com.github.nutt1101.event.*;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bstats.bukkit.Metrics;
import com.jeff_media.updatechecker.UpdateCheckSource;
import com.jeff_media.updatechecker.UpdateChecker;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class CatchBall extends JavaPlugin {
    {
        this.getConfig();
    }

    public static Plugin plugin;

    private Metrics metrics;

    private void checkPluginHook(String pluginName) {
        if (this.getServer().getPluginManager().getPlugin(pluginName) != null) {
            plugin.getLogger().log(Level.INFO, NamedTextColor.GREEN + pluginName + " Hook!");
        }
    }

    @Override
    public void onEnable() {

        plugin = this;

        ConfigSetting.checkConfig();

        // Initialize metrics
        this.metrics = new Metrics(this, 12380);

        registerEvent();
        // Remove legacy command registration for Paper plugins
        // Commands are now registered via Brigadier in registerBrigadierCommands()

        new UpdateChecker(this, UpdateCheckSource.GITHUB_RELEASE_TAG, "MagicTeaMC/CatchBall2")
                .checkEveryXHours(1) // Check every hour
                .setDownloadLink("https://modrinth.com/plugin/catchball/version/latest")
                .setChangelogLink("https://modrinth.com/plugin/catchball/version/latest")
                .checkNow(); // And check right now

        checkPluginHook("Residence");
        checkPluginHook("MythicMobs");
        checkPluginHook("GriefPrevention");
        checkPluginHook("Lands");
        checkPluginHook("PlaceholderAPI");
        checkPluginHook("RedProtect");
        checkPluginHook("SimpleClaimSystem");
        checkPluginHook("Towny");
        // TODO
        // checkPluginHook("WorldGuard");

        HandySchedulerUtil.init(this);

        // Register Brigadier commands
        new BrigadierCommandHandler(this).registerCommands();
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

    /*
    // register command (not used in Paper plugins - kept for reference)
    // Paper plugins use Brigadier commands instead
    public void registerCommand() {
        // This method is not called in Paper plugins
        // Commands are registered via registerBrigadierCommands() instead
        PluginCommand ctbCommand = this.getCommand("ctb");
        if (ctbCommand != null) {
            ctbCommand.setExecutor(new Command());
            ctbCommand.setTabCompleter(new TabComplete());
        }
    }
    */
}