package com.github.nutt1101.command;

import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.GUI.CatchableList;
import com.github.nutt1101.items.Ball;
import com.github.nutt1101.items.DropItem;
import com.github.nutt1101.utils.TranslationFileReader;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Command implements CommandExecutor {
    private List<String> commandArgument = CommandCheck.getCommandArgument();
    private Set<String> entityList = ConfigSetting.entityFile.getConfigurationSection("EntityList").getKeys(false);

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (command.getName().equals("ctb")) {

            if (!(CommandCheck.check(sender, command, label, args))) {
                return true;
            }

            /* player use "/ctb get" command
            player will get a catchBall*/
            if (args[0].equalsIgnoreCase("reload")) {
                ConfigSetting.checkConfig();
                sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.reloadSuccess, "", ""));
                return true;

            } else if (args[0].equalsIgnoreCase("list")) {
                if (!checkSenderPlayer(sender)) {
                    return true;
                }

                new CatchableList().openCatchableList((Player) sender, 1);

            } else if (args[0].equalsIgnoreCase("add")) {
                if (args.length == 1) {
                    sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.addEntityDoesNotExist, "", ""));
                    return true;
                }

                if (args[1].equalsIgnoreCase("all")) {
                    Set<String> entityList = ConfigSetting.entityFile.getConfigurationSection("EntityList")
                            .getKeys(false);
                    int addedCount = 0;
                    int skippedCount = 0;
                    List<String> skippedEntities = new ArrayList<>();

                    for (String entity : entityList) {
                        EntityType entityType = safeGetEntityType(entity);
                        if (entityType == null) {
                            // Entity doesn't exist in this MC version - skip it
                            skippedCount++;
                            skippedEntities.add(entity);
                            continue;
                        }

                        if (!ConfigSetting.catchableEntity.contains(entityType)) {
                            ConfigSetting.catchableEntity.add(String.valueOf(entityType));
                            addedCount++;
                        }
                    }

                    // Provide detailed feedback
                    if (addedCount > 0) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                TranslationFileReader.allEntityAddSuccess + " §7(" + addedCount + " entities added)"));
                        ConfigSetting.saveEntityList();

                        // Warn about skipped entities if any
                        if (skippedCount > 0) {
                            sender.sendMessage(ChatColor.YELLOW + "Warning: " + skippedCount +
                                    " entities were skipped (not available in this MC version)");
                        }
                    } else if (skippedCount > 0) {
                        sender.sendMessage(ChatColor.YELLOW + "No entities were added. " + skippedCount +
                                " entities in config are not available in this Minecraft version.");
                    } else {
                        sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.entityDoesExists, "", ""));
                    }
                    return true;
                }

                // Check if entity exists in config (case insensitive)
                String matchedEntity = findMatchingEntity(args[1]);
                if (matchedEntity == null) {
                    sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.unknownEntityType, "", ""));
                    return true;
                }

                // Safely get EntityType
                EntityType entityType = safeGetEntityType(matchedEntity);
                if (entityType == null) {
                    sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.unknownEntityType, "", ""));
                    return true;
                }

                if (ConfigSetting.catchableEntity.contains(entityType)) {
                    sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.entityDoesExists, "", ""));
                    return true;
                }

                ConfigSetting.catchableEntity.add(String.valueOf(entityType));
                sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.successAddEntity, "", entityType.name()));
                ConfigSetting.saveEntityList();
                return true;

            } else if (args[0].equalsIgnoreCase("remove")) {
                if (args.length == 1) {
                    sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.removeEntityDoesNotExist, "", ""));
                    return true;
                }

                if (args[1].equalsIgnoreCase("all")) {
                    int removedCount = ConfigSetting.catchableEntity.size();
                    ConfigSetting.catchableEntity.clear();

                    if (removedCount > 0) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                TranslationFileReader.allEntityRemoveSuccess + " §7(" + removedCount + " entities removed)"));
                        ConfigSetting.saveEntityList();
                    } else {
                        sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.removeEntityNotFound, "", ""));
                    }
                    return true;
                }

                // Check if entity exists in config (case insensitive)
                String matchedEntity = findMatchingEntity(args[1]);
                if (matchedEntity == null) {
                    sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.unknownEntityType, "", ""));
                    return true;
                }

                // Safely get EntityType
                EntityType entityType = safeGetEntityType(matchedEntity);
                if (entityType == null) {
                    sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.unknownEntityType, "", ""));
                    return true;
                }

                if (!ConfigSetting.catchableEntity.contains(entityType)) {
                    sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.removeEntityNotFound, "", ""));
                    return true;
                }

                ConfigSetting.catchableEntity.remove(entityType);
                sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.successRemove, "", entityType.name()));
                ConfigSetting.saveEntityList();
                return true;

            } else if (args[0].equalsIgnoreCase("give")) {
                if (args.length < 3) {
                    sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.invalidItemAmount, "", ""));
                    return true;
                }

                Player player = Bukkit.getPlayer(args[1]);

                if (player == null) {
                    sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.unknownOrOfflinePlayer, "", "")
                            .replace("{PLAYER}", args[1]));
                    return true;
                }

                if (checkItem(args[2]) == null) {
                    sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.itemNameError, "", ""));
                    return true;
                }

                int itemAmount = 1;
                if (args.length >= 4) {
                    try {
                        itemAmount = Integer.parseInt(args[3]);
                        if (itemAmount <= 0) {
                            sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.invalidItemAmount, "", ""));
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.invalidItemAmount, "", ""));
                        return true;
                    }
                }

                givePlayerItem(player, checkItem(args[2]), itemAmount);

                sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.successGiveItemToPlayer, "", "")
                        .replace("{ITEM}", args[2].equalsIgnoreCase("catchball") ? TranslationFileReader.catchBallName
                                : TranslationFileReader.dropItemName)
                        .replace("{PLAYER}", player.getName())
                        .replace("&", "§"));

                return true;

            } else if (!commandArgument.contains(args[0])) {

                // player entered an unknown argument
                sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.unknownCommandArgument, "", ""));
                return true;
            }
        }

        return true;
    }

    /**
     * Safely gets EntityType from string, handling version compatibility
     * @param entityName The entity name to convert
     * @return EntityType if valid and exists in current version, null otherwise
     */
    private EntityType safeGetEntityType(String entityName) {
        try {
            return EntityType.valueOf(entityName.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Entity type doesn't exist in this Minecraft version
            return null;
        }
    }

    /**
     * Finds matching entity name from config (case insensitive)
     * @param inputEntity The entity name input by user
     * @return Matching entity name from config, or null if not found
     */
    private String findMatchingEntity(String inputEntity) {
        for (String entity : entityList) {
            if (entity.equalsIgnoreCase(inputEntity)) {
                return entity;
            }
        }
        return null;
    }

    private ItemStack checkItem(String item) {
        item = item.toLowerCase();
        if (item.equals("catchball")) {
            return Ball.makeBall();
        }
        if (item.equals("dropitem")) {
            return DropItem.makeDropItem();
        }
        return null;
    }

    private Boolean checkSenderPlayer(CommandSender sender) {
        if (sender instanceof Player) {
            return true;
        }
        sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.consoleExecuteCommand, "", ""));
        return false;
    }

    private void givePlayerItem(Player player, ItemStack itemStack, int amount) {
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(ConfigSetting.toChat(TranslationFileReader.playerInventoryFull, "", ""));
            player.getWorld().dropItem(player.getLocation(), itemStack);
        } else {
            ItemStack item = itemStack.clone();
            item.setAmount(amount);
            player.getInventory().addItem(item);
        }
    }
}