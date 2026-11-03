package com.github.nutt1101.command;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.nutt1101.CatchBall;
import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.items.Ball;
import com.github.nutt1101.items.DropItem;
import com.github.nutt1101.menu.MenuText;
import com.github.nutt1101.utils.TranslationFileReader;
import java.io.IOException;
import java.lang.annotation.*;
import java.util.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import revxrsal.commands.Lamp;
import revxrsal.commands.annotation.*;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

@revxrsal.commands.annotation.Command({"ctb", "catchball"})
public final class Command {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Access { String value(); }

    public static Lamp<BukkitCommandActor> register(CatchBall plugin) {
        Lamp<BukkitCommandActor> lamp = BukkitLamp.builder(plugin)
                .permissionForAnnotation(Access.class, access -> actor ->
                        actor.sender().hasPermission("catchball.op")
                        || actor.sender().hasPermission("catchball.command." + access.value()))
                .exceptionHandler(new CommandErrors())
                .build();
        lamp.register(new Command());
        return lamp;
    }

    @revxrsal.commands.annotation.Command({"ctb", "catchball"})
    public void help(BukkitCommandActor actor) {
        reply(actor.sender(), TranslationFileReader.argDoesNotExist);
    }

    @Subcommand("reload") @Access("reload")
    public void reload(BukkitCommandActor actor) {
        try {
            ConfigSetting.checkConfig();
            reply(actor.sender(), TranslationFileReader.reloadSuccess);
        } catch (IllegalArgumentException error) {
            actor.sender().sendMessage(MenuText.render(TranslationFileReader.reloadFailed,
                    Map.of("reason", error.getMessage())));
        }
    }

    @Subcommand("list") @Access("list")
    public void list(BukkitCommandActor actor) {
        if (!actor.isPlayer()) {
            reply(actor.sender(), TranslationFileReader.consoleExecuteCommand);
            return;
        }
        ((CatchBall) CatchBall.plugin).getMenus().open(actor.requirePlayer());
    }

    @Subcommand("add") @Access("add")
    public void add(BukkitCommandActor actor, @revxrsal.commands.annotation.Optional @SuggestWith(AddEntities.class) String entity) {
        editSelection(actor.sender(), entity, true);
    }

    @Subcommand("remove") @Access("remove")
    public void remove(BukkitCommandActor actor, @revxrsal.commands.annotation.Optional @SuggestWith(RemoveEntities.class) String entity) {
        editSelection(actor.sender(), entity, false);
    }

    @Subcommand("give") @Access("give")
    public void give(BukkitCommandActor actor, Player player,
                     @Suggest({"CatchBall", "DropItem"}) String item,
                     @Default("1") @Range(min = 1) int amount) {
        if (!item.equalsIgnoreCase("catchball") && !item.equalsIgnoreCase("dropitem")) {
            reply(actor.sender(), TranslationFileReader.itemNameError);
            return;
        }
        // Inventory access belongs to the target player's region on Folia.
        UniversalScheduler.getScheduler(CatchBall.plugin).runTask(player, () -> {
            ItemStack stack = item.equalsIgnoreCase("catchball") ? Ball.makeBall() : DropItem.makeDropItem();
            stack.setAmount(amount);
            Map<Integer, ItemStack> remaining = player.getInventory().addItem(stack);
            if (!remaining.isEmpty()) {
                reply(player, TranslationFileReader.playerInventoryFull);
                remaining.values().forEach(leftover -> player.getWorld().dropItem(player.getLocation(), leftover));
            }
            actor.sender().sendMessage(MenuText.render(TranslationFileReader.successGiveItemToPlayer,
                    Map.of("item", MenuText.plain(item.equalsIgnoreCase("catchball")
                            ? TranslationFileReader.catchBallName : TranslationFileReader.dropItemName),
                            "player", player.getName())));
        });
    }

    private static void reply(CommandSender sender, String message) {
        sender.sendMessage(MenuText.render(message, Map.of()));
    }

    private void editSelection(CommandSender sender, String entity, boolean adding) {
        if (entity == null) {
            sender.sendMessage(ConfigSetting.toChat(adding ? TranslationFileReader.addEntityDoesNotExist
                    : TranslationFileReader.removeEntityDoesNotExist, "", ""));
            return;
        }
        Set<String> targets = new LinkedHashSet<>();
        boolean all = entity.equalsIgnoreCase("all");
        if (all) {
            if (adding) {
                for (String name : ConfigSetting.entityFile.getConfigurationSection("EntityList").getKeys(false)) {
                    if (safeGetEntityType(name) != null) targets.add(name);
                }
            }
        } else {
            String name = findMatchingEntity(entity);
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
        for (String name : ConfigSetting.entityFile.getConfigurationSection("EntityList").getKeys(false)) {
            if (name.equalsIgnoreCase(inputEntity)) {
                return name;
            }
        }
        return null;
    }


    public static final class AddEntities implements SuggestionProvider<BukkitCommandActor> {
        public Collection<String> getSuggestions(revxrsal.commands.node.ExecutionContext<BukkitCommandActor> context) {
            return entitySuggestions(true);
        }
    }

    public static final class RemoveEntities implements SuggestionProvider<BukkitCommandActor> {
        public Collection<String> getSuggestions(revxrsal.commands.node.ExecutionContext<BukkitCommandActor> context) {
            return entitySuggestions(false);
        }
    }

    private static List<String> entitySuggestions(boolean adding) {
        var section = ConfigSetting.entityFile.getConfigurationSection("EntityList");
        if (section == null) return List.of();
        List<String> selected = ConfigSetting.catchableEntity;
        List<String> values = section.getKeys(false).stream()
                .filter(name -> {
                    try { EntityType.valueOf(name); return true; }
                    catch (IllegalArgumentException ignored) { return false; }
                })
                .filter(name -> selected.contains(name) != adding)
                .sorted().collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (!values.isEmpty()) values.add("ALL");
        return values;
    }
}
