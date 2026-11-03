package com.github.nutt1101.command;

import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.CatchBall;
import com.github.nutt1101.menu.MenuText;
import java.io.IOException;
import java.util.Map;
import java.util.LinkedHashSet;
import com.github.nutt1101.items.Ball;
import com.github.nutt1101.items.DropItem;
import com.github.nutt1101.utils.TranslationFileReader;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Command implements BasicCommand {
    private final List<String> commandArgument = CommandCheck.getCommandArgument();
    private final TabComplete tabComplete = new TabComplete();

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        handleCommand(source.getSender(), args);
    }

    @Override
    public java.util.Collection<String> suggest(CommandSourceStack source, String[] args) {
        return tabComplete.suggest(source.getSender(), args);
    }

    private boolean handleCommand(CommandSender sender, String[] args) {
        if (!(CommandCheck.check(sender, args))) {
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            try {
                ConfigSetting.checkConfig();
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.reloadFailed, "", "")
                        .replace("{REASON}", e.getMessage()));
                return true;
            }
            sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.reloadSuccess, "", ""));
            return true;

        } else if (args[0].equalsIgnoreCase("list")) {
            if (!checkSenderPlayer(sender)) {
                return true;
            }

            ((CatchBall) CatchBall.plugin).getMenus().open((Player) sender);

        } else if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {
            editSelection(sender, args, args[0].equalsIgnoreCase("add"));
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

        return true;
    }

    private void editSelection(CommandSender sender, String[] args, boolean adding) {
        if (args.length < 2) {
            sender.sendMessage(ConfigSetting.toChat(adding ? TranslationFileReader.addEntityDoesNotExist
                    : TranslationFileReader.removeEntityDoesNotExist, "", ""));
            return;
        }
        Set<String> targets = new LinkedHashSet<>();
        boolean all = args[1].equalsIgnoreCase("all");
        if (all) {
            if (adding) {
                for (String name : ConfigSetting.entityFile.getConfigurationSection("EntityList").getKeys(false)) {
                    if (safeGetEntityType(name) != null) targets.add(name);
                }
            }
        } else {
            String name = findMatchingEntity(args[1]);
            if (name == null || safeGetEntityType(name) == null) {
                sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.unknownEntityType, "", ""));
                return;
            }
            targets.add(name);
        }
        try {
            boolean changed = ((CatchBall) CatchBall.plugin).getSelections().update(next -> {
                if (adding) next.addAll(targets);
                else if (all) next.clear();
                else next.removeAll(targets);
            });
            String message = !changed ? (adding ? TranslationFileReader.entityDoesExists : TranslationFileReader.removeEntityNotFound)
                    : all ? (adding ? TranslationFileReader.allEntityAddSuccess : TranslationFileReader.allEntityRemoveSuccess)
                    : (adding ? TranslationFileReader.successAddEntity : TranslationFileReader.successRemove);
            sender.sendMessage(ConfigSetting.toChat(message, "", all ? "" : targets.iterator().next()));
        } catch (IOException error) {
            sender.sendMessage(MenuText.render(TranslationFileReader.localeYamlConfig.getString("Menu.save-failed",
                    "<red>Could not save the selection. Please try again."), Map.of()));
            CatchBall.plugin.getLogger().log(java.util.logging.Level.WARNING, "Could not save entity selection", error);
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

    /**
     * Finds matching entity name from config (case insensitive)
     * @param inputEntity The entity name input by user
     * @return Matching entity name from config, or null if not found
     */
    private String findMatchingEntity(String inputEntity) {
        for (String entity : ConfigSetting.entityFile.getConfigurationSection("EntityList").getKeys(false)) {
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