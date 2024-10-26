package com.github.nutt1101;

import java.util.logging.Level;

import cn.handyplus.lib.adapter.HandySchedulerUtil;
import com.github.nutt1101.command.Command;
import com.github.nutt1101.command.TabComplete;
import com.github.nutt1101.event.DropGoldEgg;
import com.github.nutt1101.event.GUIClick;
import com.github.nutt1101.event.HitEvent;
import com.github.nutt1101.event.SkullClick;
import com.jeff_media.updatechecker.UpdateCheckSource;
import com.jeff_media.updatechecker.UpdateChecker;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Arrays;
import java.util.List;

public class CatchBall extends JavaPlugin{
    private FileConfiguration config = this.getConfig();

    public static Plugin plugin;

    private void checkPluginHook(String pluginName) {
        if (this.getServer().getPluginManager().getPlugin(pluginName) != null) {
            plugin.getLogger().log(Level.INFO, ChatColor.GREEN + pluginName + " Hook!");
        }
    }

    @Override
    public void onEnable() {

        plugin = this;

        ConfigSetting.checkConfig();

        new Metrics(this, 12380);
        registerEvent();
        registerCommand();

        new UpdateChecker(this, UpdateCheckSource.HANGAR, "Maoyue_OUO/CatchBall/Release")
                .checkEveryXHours(1) // Check every hour
                .setDownloadLink("https://hangar.papermc.io/Maoyue_OUO/CatchBall/versions")
                .setChangelogLink("https://hangar.papermc.io/Maoyue_OUO/CatchBall/versions")
                .checkNow(); // And check right now

        checkPluginHook("Residence");
        checkPluginHook("MythicMobs");
        checkPluginHook("GriefPrevention");
        checkPluginHook("Lands");
        checkPluginHook("PlaceholderAPI");
        checkPluginHook("RedProtect");
        checkPluginHook("SimpleClaimSystem");
        // TODO
        // checkPluginHook("WorldGuard");

        HandySchedulerUtil.init(this);

    }

    // register event
    public void registerEvent() {
        PluginManager registerEvent = this.getServer().getPluginManager();
        registerEvent.registerEvents(new HitEvent(), this);
        registerEvent.registerEvents(new DropGoldEgg(), this);
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
