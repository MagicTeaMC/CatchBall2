package com.github.nutt1101.command;

import com.github.nutt1101.ConfigSetting;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class TabComplete implements TabCompleter {
    List<String> entityList = new ArrayList<>();

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        // tabComplete will be show suggest arument to commandSender
        if (command.getName().equals("ctb")) {
            final List<String> sort = new ArrayList<>();

            if (!sender.hasPermission("catchball.op")) { return List.of(""); }

            if (args.length == 1) {
                StringUtil.copyPartialMatches(args[0], CommandCheck.getCommandArgument(), sort);
                return sort;
            }

            if (args.length == 2) {
                entityList.clear();
                if (args[0].equalsIgnoreCase("give")) {
                    List<String> playerNames = new ArrayList<>();
                    Bukkit.getOnlinePlayers().forEach(player -> playerNames.add(player.getName()));

                    // search for player names
                    searchPlayerNames(args[1], playerNames, sort);
                    sort.sort(String.CASE_INSENSITIVE_ORDER);
                    return sort;
                } else if (args[0].equalsIgnoreCase("add")) {
                    Set<String> allEntityList = ConfigSetting.entityFile.getConfigurationSection("EntityList").getKeys(false);

                    // Use traditional for loop instead of streams for better compatibility
                    int availableToAdd = 0;
                    for (String entityName : allEntityList) {
                        EntityType entityType = safeGetEntityType(entityName);
                        if (entityType != null && !ConfigSetting.catchableEntity.contains(entityType)) {
                            entityList.add(entityName);
                            availableToAdd++;
                        }
                    }

                    // Only show "ALL" if there are entities that can actually be added
                    if (availableToAdd > 1) {
                        entityList.add("ALL");
                    }

                    StringUtil.copyPartialMatches(args[1], entityList, sort);
                    return sort;

                } else if (args[0].equalsIgnoreCase("remove")) {

                    // Use traditional for loop instead of streams for better compatibility
                    Set<String> allEntityList = ConfigSetting.entityFile.getConfigurationSection("EntityList").getKeys(false);
                    int availableToRemove = 0;
                    for (String entityName : allEntityList) {
                        EntityType entityType = safeGetEntityType(entityName);
                        if (entityType != null && ConfigSetting.catchableEntity.contains(entityType)) {
                            entityList.add(entityName);
                            availableToRemove++;
                        }
                    }

                    // Only show "ALL" if there are entities that can actually be removed
                    if (availableToRemove > 0) {
                        entityList.add("ALL");
                    }

                    StringUtil.copyPartialMatches(args[1], entityList, sort);
                    return sort;
                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("give")) {
                    StringUtil.copyPartialMatches(args[2], Arrays.asList("CatchBall", "DropItem"), sort);
                    return sort;
                }
            } else if (args.length == 4) {
                if (args[0].equalsIgnoreCase("give")) {
                    StringUtil.copyPartialMatches(args[3], Arrays.asList(
                            "1", "2", "3", "4", "5", "6", "7", "8", "9"
                    ), sort);
                    return sort;
                }
            }
        }

        return List.of("");
    }

    /**
     * Player name search
     * @param input The partial input from the user
     * @param playerNames List of all available player names
     * @param results List to add matching results to
     */
    private void searchPlayerNames(String input, List<String> playerNames, List<String> results) {
        if (input == null || input.isEmpty()) {
            results.addAll(playerNames);
            return;
        }

        String lowerInput = input.toLowerCase();

        // First add exact prefix matches (higher priority)
        for (String playerName : playerNames) {
            if (playerName.toLowerCase().startsWith(lowerInput)) {
                results.add(playerName);
            }
        }

        // Then add substring matches that aren't already included
        for (String playerName : playerNames) {
            String lowerPlayerName = playerName.toLowerCase();
            if (lowerPlayerName.contains(lowerInput) && !lowerPlayerName.startsWith(lowerInput)) {
                results.add(playerName);
            }
        }
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
}