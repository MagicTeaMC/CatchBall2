package com.github.nutt1101;

import com.bekvon.bukkit.residence.containers.Flags;
import com.github.nutt1101.Recipe.BallRecipe;
import com.github.nutt1101.utils.TranslationFileReader;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ConfigSetting {
    private final static Plugin plugin = CatchBall.plugin;
    public static String locale;
    public static boolean updatecheck;
    public static List<String> catchableEntity = new ArrayList<>(); // Changed from EntityType to String
    public static boolean DropEnable;
    public static boolean DropNeedPermission;
    public static DropMethodType DropMethod;
    public static String DropEntityType; // Changed from EntityType to String
    public static Material DropBlockType;
    public static int DropItemChance;
    public static Material DropItemMaterial;
    public static String catchSuccessSound;
    public static YamlConfiguration entityFile;
    public static boolean recipeEnabled;

    public static List<String> residenceFlag = new ArrayList<>();

    public static List<String> griefPreventionFlag = new ArrayList<>();
    public static boolean allowCatchableTamedOwnerIsNull;
    public static boolean ShowParticles;
    public static String CustomParticles;
    public static double catchFailRate;
    public static int customModelData;
    public static int ballCustomModelData;
    public static boolean UseRes;
    public static boolean UseGF;
    public static boolean UseLands;
    public static boolean UsePAPI;
    public static boolean UseMM;
    public static boolean UseRP;
    public static boolean UseSCS;

    // TODO
    // public static boolean UseWG;

    /**
     * Check if an entity type string is valid (exists in current server version)
     */
    private static boolean isValidEntityType(String entityName) {
        try {
            EntityType.valueOf(entityName.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Initialize or reload the plugin
     */
    public static void checkConfig() {
        // check if the file exists
        if (!new File(plugin.getDataFolder(), "config.yml").exists()) {
            plugin.saveResource("config.yml", false);
        }

        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        locale = config.isSet("Locale") ? config.getString("Locale") : "en";

        entityFileCreate();

        DropEnable = !config.isSet("DropEnable") || config.getBoolean("DropEnable");
        DropNeedPermission = config.isSet("DropNeedPermission") && config.getBoolean("DropNeedPermission");
        DropItemChance = config.isSet("DropItemChance")
                ? Integer.parseInt(config.getString("DropItemChance").replace("%", ""))
                : 50;
        try {
            DropItemMaterial = config.isSet("DropItemMaterial") ? Material.matchMaterial(Objects.requireNonNull(config.getString("DropItemMaterial"))) : Material.EGG;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, ChatColor.RED + "Invalid DropItemMaterial in config.yml, using default 'EGG' material.");
            DropItemMaterial = Material.EGG;
        }

        try {
            DropMethod = config.isSet("DropMethod") ? DropMethodType.valueOf(config.getString("DropMethod").toUpperCase()) : DropMethodType.CHICKEN;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, ChatColor.RED + "Invalid DropMethod in config.yml, using default 'CHICKEN' method.");
            DropMethod = DropMethodType.CHICKEN;
        }

        // Store as string instead of EntityType
        DropEntityType = config.isSet("DropEntityType") ? config.getString("DropEntityType").toUpperCase() : "CHICKEN";
        if (!isValidEntityType(DropEntityType)) {
            plugin.getLogger().log(Level.WARNING, ChatColor.RED + "Invalid DropEntityType in config.yml, using default 'CHICKEN'.");
            DropEntityType = "CHICKEN";
        }

        DropBlockType = config.isSet("DropBlockType") ? Material.matchMaterial(Objects.requireNonNull(config.getString("DropBlockType"))) : Material.DIAMOND_ORE;

        updatecheck = !config.isSet("Update-Check") || config.getBoolean("Update-Check");

        entityFile = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "entity.yml"));

        catchSuccessSound = config.isSet("CatchSuccessSound") ? config.getString("CatchSuccessSound").toUpperCase()
                : "ENTITY_ARROW_HIT_PLAYER".toUpperCase();

        recipeEnabled = !config.isSet("Recipe.enabled") || config.getBoolean("Recipe.enabled");

        residenceFlag = config.isSet("ResidenceFlag") ? config.getStringList("ResidenceFlag")
                : Arrays.asList("animalkilling");

        griefPreventionFlag = config.isSet("GriefPreventionFlag") ? config.getStringList("GriefPreventionFlag")
                : Arrays.asList("Access");
        allowCatchableTamedOwnerIsNull = !config.isSet("AllowCatchableTamedOwnerIsNull")
                || config.getBoolean("AllowCatchableTamedOwnerIsNull");
        ShowParticles = !config.isSet("ShowParticles")
                || config.getBoolean("ShowParticles");
        CustomParticles = config.isSet("CustomParticles") ? config.getString("CustomParticles") : "CLOUD";
        catchFailRate = !config.isSet("catchFailRate") ? config.getDouble("catchFailRate")
                : 0.1;
        customModelData = !config.isSet("customModelData") ? config.getInt("customModelData")
                : 0;

        ballCustomModelData = !config.isSet("ballCustomModelData") ? config.getInt("ballCustomModelData")
                : 0;

        try {
            TranslationFileReader.init();
        } catch (IOException e) {
            e.printStackTrace();
            plugin.getLogger().log(Level.WARNING,
                    ChatColor.RED + String.format("The locale you have selected '%s' is currently not supported",
                            locale));
            plugin.getLogger().log(Level.WARNING, ChatColor.RED + "We only support: en, zh_tw");
            locale = "en";
        }

        new BallRecipe();

        if (!catchableEntity.isEmpty()) {
            catchableEntity.clear();
        }

        try {
            if (plugin.getServer().getPluginManager().getPlugin("Residence") != null) {
                residenceFlag = residenceFlag.stream().map(flag -> Flags.valueOf(flag)).map(Flags::name)
                        .collect(Collectors.toList());
            }

        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, ChatColor.RED + e.getMessage());
            plugin.getLogger().log(Level.WARNING, ChatColor.RED + "Unknown Residence flag!");
            plugin.getLogger().log(Level.WARNING, ChatColor.RED + "Please check your config setting!");
            residenceFlag.clear();
            residenceFlag.add("animalkilling");
        }

        try {
            if (plugin.getServer().getPluginManager().getPlugin("GriefPrevention") != null) {
                griefPreventionFlag = griefPreventionFlag.stream()
                        .map(flag -> flag.substring(0, 1).toUpperCase() + flag.substring(1))
                        .map(String::toUpperCase).collect(Collectors.toList());
                for (int i = 0; i < griefPreventionFlag.size(); i++) {
                    ClaimPermission.valueOf(griefPreventionFlag.get(i));
                }
            }

        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, ChatColor.RED + e.getMessage());
            plugin.getLogger().log(Level.WARNING, ChatColor.RED + "Unknown GriefPrevention flag!");
            plugin.getLogger().log(Level.WARNING, ChatColor.RED + "Please check your config setting!");
            griefPreventionFlag.clear();
            griefPreventionFlag.add("Access");
        }

        // Changed: Load entities as strings and validate them
        for (String entity : config.getStringList("CatchableEntity")) {
            String entityName = entity.toUpperCase();
            if (isValidEntityType(entityName)) {
                catchableEntity.add(entityName);
            } else {
                plugin.getLogger().log(Level.WARNING, ChatColor.YELLOW + "Unknown entity type '" + entity + "' in CatchableEntity list, skipping...");
            }
        }

        UseRes = !config.isSet("UseRes")
                || config.getBoolean("UseRes");

        UseGF = !config.isSet("UseGF")
                || config.getBoolean("UseGF");

        UseLands = !config.isSet("UseLands")
                || config.getBoolean("UseLands");

        UsePAPI = !config.isSet("UsePAPI")
                || config.getBoolean("UsePAPI");

        UseMM = !config.isSet("UseMM")
                || config.getBoolean("UseMM");

        UseRP = !config.isSet("UseRP")
                || config.getBoolean("UseRP");

        UseSCS = !config.isSet("UseSCS")
                || config.getBoolean("UseSCS");

        /*UseWG = !config.isSet("UseWG")
                || config.getBoolean("UseWG");

                // TODO
         */
    }

    /**
     * Check the version of the server to determine the entity.yml file output version.
     */
    public static void entityFileCreate() {
        File file = new File(plugin.getDataFolder(), "entity.yml");
        InputStream inputStream = plugin.getResource("entity/" + locale + ".yml");

        if (inputStream != null) {
            try {
                FileOutputStream outputStream = new FileOutputStream(file);
                inputStream.transferTo(outputStream);
                outputStream.close();
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            plugin.getLogger().log(Level.WARNING, ChatColor.RED + "Unknown Error make plugin file can not place!");
        }
    }

    /**
     * Get entityDisplayName saved on entity.yml.
     *
     * @param entityName name of saved entity
     * @return entityDisplayName
     */
    public static String getEntityDisplayName(String entityName) {
        if (entityFile.getConfigurationSection("EntityList").contains(entityName)) {
            return entityFile.getString("EntityList." + entityName.toUpperCase() + ".DisplayName");
        }
        return entityName;
    }

    /**
     * Replace the message argument from config.yml.
     *
     * @param location replace {LOCATION} from source message
     * @param entity   replace {ENTITY} from source message
     * @return replaced message
     */
    public static String toChat(String message, String location, String entity) {
        message = message.contains("{BALL}") ? message.replace("{BALL}", TranslationFileReader.catchBallName)
                : message;
        message = message.contains("{LOCATION}") ? message.replace("{LOCATION}", location) : message;
        message = message.contains("{ENTITY}") ? message.replace("{ENTITY}", getEntityDisplayName(entity)) : message;

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Save the entity.yml file if the catchableEntity changes.
     */
    public static void saveEntityList() {
        FileConfiguration fileConfiguration = plugin.getConfig();

        // Changed: catchableEntity is already a List<String>, so no need to convert
        fileConfiguration.set("CatchableEntity", catchableEntity.toArray());

        try {
            fileConfiguration.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isLatestVersion(String current, String latest) {

        String[] currentParts = current.split("\\.");

        String[] latestParts = latest.split("\\.");

        int minLength = Math.min(currentParts.length, latestParts.length);

        for (int i = 0; i < minLength; i++) {

            int currentPart = Integer.parseInt(currentParts[i]);

            int latestPart = Integer.parseInt(latestParts[i]);

            if (currentPart < latestPart) return false;

            if (currentPart > latestPart) return true;
        }
        return currentParts.length >= latestParts.length;
    }

    public enum DropMethodType {
        CHICKEN,
        ENTITY,
        BLOCK
    }
}