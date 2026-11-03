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
import java.util.*;
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
    public static boolean UseTowny;

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
        FileConfiguration config = com.github.nutt1101.configuration.ConfigSchema.load(
                plugin.getDataFolder().toPath().resolve("config.yml"),
                plugin.getResource("config.yml"), plugin.getLogger()::warning);
        // Replace Bukkit's cached configuration only after validation and persistence succeed.
        plugin.reloadConfig();

        UsePAPI = config.getBoolean("integrations.placeholder-api.enabled", true);
        locale = config.isSet("messages.locale") ? config.getString("messages.locale") : "en";
        entityFileCreate();

        DropEnable = !config.isSet("drops.enabled") || config.getBoolean("drops.enabled");
        DropNeedPermission = config.isSet("drops.require-permission") && config.getBoolean("drops.require-permission");
        DropItemChance = config.isSet("drops.chance")
                ? config.getInt("drops.chance")
                : 50;
        try {
            DropItemMaterial = config.isSet("drops.material") ? Material.matchMaterial(Objects.requireNonNull(config.getString("drops.material"))) : Material.EGG;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, ChatColor.RED + "Invalid DropItemMaterial in config.yml, using default 'EGG' material.");
            DropItemMaterial = Material.EGG;
        }

        try {
            DropMethod = config.isSet("drops.method") ? DropMethodType.valueOf(config.getString("drops.method").toUpperCase()) : DropMethodType.CHICKEN;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, ChatColor.RED + "Invalid DropMethod in config.yml, using default 'CHICKEN' method.");
            DropMethod = DropMethodType.CHICKEN;
        }

        // Store as string instead of EntityType
        DropEntityType = config.isSet("drops.entity-type") ? config.getString("drops.entity-type").toUpperCase() : "CHICKEN";
        if (!isValidEntityType(DropEntityType)) {
            plugin.getLogger().log(Level.WARNING, ChatColor.RED + "Invalid DropEntityType in config.yml, using default 'CHICKEN'.");
            DropEntityType = "CHICKEN";
        }

        DropBlockType = config.isSet("drops.block-type") ? Material.matchMaterial(Objects.requireNonNull(config.getString("drops.block-type"))) : Material.DIAMOND_ORE;

        updatecheck = !config.isSet("Update-Check") || config.getBoolean("Update-Check");

        entityFile = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "entity.yml"));

        catchSuccessSound = config.isSet("capture.success-sound") ? config.getString("capture.success-sound").toUpperCase()
                : "ENTITY_ARROW_HIT_PLAYER".toUpperCase();

        recipeEnabled = !config.isSet("recipe.enabled") || config.getBoolean("recipe.enabled");

        residenceFlag = config.isSet("integrations.residence.flags") ? config.getStringList("integrations.residence.flags")
                : Arrays.asList("animalkilling");

        griefPreventionFlag = config.isSet("integrations.grief-prevention.flags") ? config.getStringList("integrations.grief-prevention.flags")
                : Arrays.asList("Access");
        allowCatchableTamedOwnerIsNull = !config.isSet("capture.allow-ownerless-tamed")
                || config.getBoolean("capture.allow-ownerless-tamed");
        ShowParticles = !config.isSet("capture.particles.enabled")
                || config.getBoolean("capture.particles.enabled");
        CustomParticles = config.isSet("capture.particles.type") ? config.getString("capture.particles.type") : "CLOUD";
        catchFailRate = config.getDouble("capture.fail-rate", 0.0);
        customModelData = config.getInt("items.catch-ball.custom-model-data", 0);

        ballCustomModelData = config.getInt("items.drop-item.custom-model-data", 0);

        try {
            TranslationFileReader.init();
        } catch (IOException e) {
            e.printStackTrace();
            plugin.getLogger().log(Level.WARNING,
                    ChatColor.RED + String.format("The locale you have selected '%s' is currently not supported",
                            locale));
            plugin.getLogger().log(Level.WARNING, ChatColor.RED + "We only support: en, zh_tw");
            locale = "en";
            try {
                TranslationFileReader.init();
            } catch (IOException fallbackError) {
                throw new IllegalStateException("Could not load fallback locale", fallbackError);
            }
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
        for (String entity : config.getStringList("capture.catchable-entities")) {
            String entityName = entity.toUpperCase();
            if (isValidEntityType(entityName)) {
                catchableEntity.add(entityName);
            } else {
                plugin.getLogger().log(Level.WARNING, ChatColor.YELLOW + "Unknown entity type '" + entity + "' in CatchableEntity list, skipping...");
            }
        }

        UseRes = !config.isSet("integrations.residence.enabled")
                || config.getBoolean("integrations.residence.enabled");

        UseGF = !config.isSet("integrations.grief-prevention.enabled")
                || config.getBoolean("integrations.grief-prevention.enabled");

        UseLands = !config.isSet("integrations.lands.enabled")
                || config.getBoolean("integrations.lands.enabled");

        UsePAPI = !config.isSet("integrations.placeholder-api.enabled")
                || config.getBoolean("integrations.placeholder-api.enabled");

        UseMM = !config.isSet("integrations.mythic-mobs.enabled")
                || config.getBoolean("integrations.mythic-mobs.enabled");

        UseRP = !config.isSet("integrations.red-protect.enabled")
                || config.getBoolean("integrations.red-protect.enabled");

        UseSCS = !config.isSet("integrations.simple-claim-system.enabled")
                || config.getBoolean("integrations.simple-claim-system.enabled");

        UseTowny = !config.isSet("integrations.towny.enabled")
                || config.getBoolean("integrations.towny.enabled");

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
        fileConfiguration.set("capture.catchable-entities", new ArrayList<>(new LinkedHashSet<>(catchableEntity)));

        try {
            com.github.nutt1101.configuration.ConfigSchema.save(
                    plugin.getDataFolder().toPath().resolve("config.yml"), fileConfiguration);
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