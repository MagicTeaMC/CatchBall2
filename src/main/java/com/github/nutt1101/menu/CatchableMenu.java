package com.github.nutt1101.menu;

import com.github.nutt1101.CatchBall;
import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.HeadDrop;
import com.github.nutt1101.service.EntitySelectionService;
import com.github.nutt1101.utils.TranslationFileReader;
import net.kyori.adventure.text.Component;
import net.momirealms.sparrow.ui.item.Item;
import net.momirealms.sparrow.ui.item.click.ItemClick;
import net.momirealms.sparrow.ui.pane.Element;
import net.momirealms.sparrow.ui.pane.Pane;
import net.momirealms.sparrow.ui.pane.page.Page;
import net.momirealms.sparrow.ui.state.MutableSignal;
import net.momirealms.sparrow.ui.state.Signal;
import net.momirealms.sparrow.ui.state.Signals;
import net.momirealms.sparrow.ui.window.AnvilWindow;
import net.momirealms.sparrow.ui.window.NormalWindow;
import net.momirealms.sparrow.ui.window.Window;
import net.momirealms.sparrow.ui.window.WindowSession;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

/** Sparrow windows own view state; selection edits are shared with commands. */
public final class CatchableMenu {
    private final CatchBall plugin;
    private final EntitySelectionService selections;
    private final MutableSignal<Set<String>> allowed = Signal.of(Set.of());
    private final Set<WindowSession> sessions = ConcurrentHashMap.newKeySet();
    private volatile List<EntityMenuModel.Entry> catalog = List.of();
    private volatile Map<String, ItemStack> thumbnails = Map.of();
    private volatile Map<String, String> messages = Map.of();

    public CatchableMenu(CatchBall plugin, EntitySelectionService selections) {
        this.plugin = plugin;
        this.selections = selections;
        reload();
    }

    public void refreshSelection() { allowed.set(selections.snapshot()); }

    public void reload() {
        close();
        Map<String, String> strings = new HashMap<>();
        loadBundledMessages("en", strings);
        if (!"en".equals(ConfigSetting.locale)) loadBundledMessages(ConfigSetting.locale, strings);
        TranslationFileReader.localeYamlConfig.getValues(true).forEach((key, value) -> {
            if (value instanceof String text) strings.put(key, text);
        });
        if (!TranslationFileReader.localeYamlConfig.contains("Menu.title")) {
            strings.put("Menu.title", TranslationFileReader.catchableListTitle);
        }
        messages = Map.copyOf(strings);
        List<EntityMenuModel.Entry> entries = new ArrayList<>();
        Map<String, ItemStack> heads = new HashMap<>();
        var section = ConfigSetting.entityFile.getConfigurationSection("EntityList");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                EntityType type;
                try { type = EntityType.valueOf(id); }
                catch (IllegalArgumentException ignored) { continue; }
                if (type == EntityType.PLAYER || type == EntityType.UNKNOWN) continue;
                entries.add(new EntityMenuModel.Entry(id, ConfigSetting.getEntityDisplayName(id)));
                heads.put(id, new HeadDrop().skullTextures(new ItemStack(Material.PLAYER_HEAD), ConfigSetting.entityFile, id));
            }
        }
        catalog = List.copyOf(entries);
        thumbnails = Map.copyOf(heads);
        refreshSelection();
    }

    private void loadBundledMessages(String locale, Map<String, String> target) {
        var stream = plugin.getResource("locale/" + locale + ".yml");
        if (stream == null) return;
        try (var reader = new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)) {
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(reader).getValues(true).forEach((key, value) -> {
                if (value instanceof String text) target.put(key, text);
            });
        } catch (IOException error) {
            report(error);
        }
    }

    public void close() {
        List.copyOf(sessions).forEach(WindowSession::end);
        sessions.clear();
    }

    public void open(Player player) {
        if (!canView(player)) {
            player.sendMessage(text("NoPermission", "<red>You do not have permission."));
            return;
        }
        new View(player).open();
    }

    public static boolean canView(Player player) {
        return player.hasPermission("catchball.op") || player.hasPermission("catchball.command.list");
    }

    public static boolean canEdit(Player player) {
        return player.hasPermission("catchball.op") || player.hasPermission("catchball.gui.edit");
    }

    private Component text(String key, String fallback) { return text(key, fallback, Map.of()); }
    private Component text(String key, String fallback, Map<String, String> values) {
        return MenuText.render(messages.getOrDefault(key, fallback), values);
    }

    private ItemStack icon(Material material, Component name, Component... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name);
        meta.lore(List.of(lore));
        stack.setItemMeta(meta);
        return stack;
    }

    private final class View {
        private final Player viewer;
        private volatile WindowSession session;
        private final MutableSignal<String> query = Signal.of("");
        private final MutableSignal<EntityMenuModel.Filter> filter = Signal.of(EntityMenuModel.Filter.ALL);
        private final Signal<List<EntityMenuModel.Entry>> results;
        private final Page<EntityMenuModel.Entry> pages;

        private View(Player viewer) {
            this.viewer = viewer;
            results = Signals.combine(allowed, query, filter,
                    (selection, needle, mode) -> EntityMenuModel.filter(catalog, selection, needle, mode));
            pages = Page.of(results, 45);
        }

        private void open() {
            var pane = Pane.builder("EEEEEEEEE", "EEEEEEEEE", "EEEEEEEEE", "EEEEEEEEE", "EEEEEEEEE", "PASFICD#N")
                    .addIngredient('E', pages, entry -> Element.item(entity(entry)), Runnable::run)
                    .addIngredient('P', pageButton(-1))
                    .addIngredient('N', pageButton(1))
                    .addIngredient('A', bulkButton(true))
                    .addIngredient('D', bulkButton(false))
                    .addIngredient('S', button(Material.COMPASS, "Menu.search", "<yellow>Search", this::search))
                    .addIngredient('F', Item.builder().dependsOn(filter).setItemProvider(context -> icon(Material.HOPPER,
                            text("Menu.filter", "<yellow>Filter: <filter>", Map.of("filter", filterName()))))
                            .addClickHandler(click -> { filter.set(filter.get().next()); pages.setPage(0); }).build())
                    .addIngredient('I', Item.builder().dependsOn(query, results, pages.page(), pages.count()).setItemProvider(context ->
                            icon(Material.OAK_SIGN, text("Menu.page", "<aqua>Page <page>/<pages>",
                                            Map.of("page", "" + (pages.page().get() + 1), "pages", "" + pages.count().get())),
                                    text("Menu.results", "<gray><count> entities · <query>", Map.of("count", "" + results.get().size(), "query", query.get())),
                                    text(canEdit(viewer) ? "Menu.edit-hint" : "Menu.view-hint",
                                            canEdit(viewer) ? "<yellow>Click an entity to change its status." : "<gray>Viewing only."))).build())
                    .addIngredient('C', button(Material.MILK_BUCKET, "Menu.clear", "<yellow>Clear search", click -> {
                        query.set(""); pages.setPage(0);
                    })).build();
            NormalWindow.builder().setUpperPane(pane)
                    .setTitle(text("Menu.title", messages.getOrDefault("catchableListTitle", "<gold>Catchable entities")))
                    .setSessionKind(WindowSession.Kind.RETAINED_STACK)
                    .addOpenHandler(window -> { session = window.session(); sessions.add(session); })
                    .addSessionEndHandler(reason -> { if (session != null) sessions.remove(session); })
                    .build(viewer).open().exceptionally(error -> { report(error); return null; });
        }

        private String filterName() {
            String name = filter.get().name().toLowerCase(Locale.ROOT);
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(text("Menu.filters." + name, name));
        }

        private Item entity(EntityMenuModel.Entry entry) {
            return Item.builder().dependsOn(allowed).setItemProvider(context -> {
                ItemStack stack = thumbnails.getOrDefault(entry.id(), new ItemStack(Material.PLAYER_HEAD)).clone();
                ItemMeta meta = stack.getItemMeta();
                meta.displayName(text("Menu.entity-name", "<white><entity>", Map.of("entity", entry.displayName())));
                meta.lore(List.of(Component.text(entry.id(), net.kyori.adventure.text.format.NamedTextColor.GRAY)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false), text(allowed.get().contains(entry.id()) ? "Menu.allowed" : "Menu.blocked",
                        allowed.get().contains(entry.id()) ? "<green>Allowed" : "<red>Blocked")));
                stack.setItemMeta(meta);
                return stack;
            }).addClickHandler(click -> {
                if (!canView(click.player()) || !canEdit(click.player())) {
                    click.player().sendMessage(text("NoPermission", "<red>You do not have permission."));
                    return;
                }
                try {
                    selections.update(next -> { if (!next.remove(entry.id())) next.add(entry.id()); });
                } catch (IOException error) {
                    click.player().sendMessage(text("Menu.save-failed", "<red>Could not save the selection. Please try again."));
                    report(error);
                }
            }).build();
        }

        private Item pageButton(int step) {
            return Item.builder().dependsOn(pages.page(), pages.count()).setItemProvider(context -> icon(Material.ARROW,
                            text(step < 0 ? "prevPage" : "nextPage", step < 0 ? "<yellow>Previous page" : "<yellow>Next page")))
                    .addClickHandler(click -> pages.advance(step)).build();
        }

        private Item button(Material material, String key, String fallback, Consumer<ItemClick> action) {
            return Item.builder().setItemProviderConstant(icon(material, text(key, fallback)))
                    .addClickHandler(action).build();
        }

        private Item bulkButton(boolean enabling) {
            if (!canEdit(viewer)) return Item.simple(icon(Material.GRAY_STAINED_GLASS_PANE, Component.empty()));
            return button(enabling ? Material.LIME_DYE : Material.RED_DYE,
                    enabling ? "Menu.allow-all" : "Menu.block-all",
                    enabling ? "<green>Allow all listed entities" : "<red>Block all listed entities",
                    click -> confirmBulk(click, enabling));
        }

        private void confirmBulk(ItemClick click, boolean enabling) {
            if (!canView(click.player()) || !canEdit(click.player())) {
                click.player().sendMessage(text("NoPermission", "<red>You do not have permission."));
                return;
            }
            Set<String> targets = catalog.stream().map(EntityMenuModel.Entry::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
            var confirmation = Pane.builder("#########", "#Y##I##N#", "#########")
                    .addIngredient('I', Item.simple(icon(Material.PAPER,
                            text(enabling ? "Menu.confirm-allow" : "Menu.confirm-block",
                                    enabling ? "<yellow>Allow <count> listed entities?" : "<yellow>Block <count> listed entities?",
                                    Map.of("count", "" + targets.size())))))
                    .addIngredient('N', button(Material.RED_DYE, "Menu.cancel", "<red>Cancel", cancel -> cancel.window().back()))
                    .addIngredient('Y', button(Material.LIME_DYE, "Menu.confirm", "<green>Confirm", confirm -> {
                        if (!canView(confirm.player()) || !canEdit(confirm.player())) {
                            confirm.player().sendMessage(text("NoPermission", "<red>You do not have permission."));
                            return;
                        }
                        try {
                            selections.update(next -> { if (enabling) next.addAll(targets); else next.removeAll(targets); });
                            confirm.window().back();
                        } catch (IOException error) {
                            confirm.player().sendMessage(text("Menu.save-failed", "<red>Could not save the selection. Please try again."));
                            report(error);
                        }
                    })).build();
            click.window().navigate(NormalWindow.builder().setUpperPane(confirmation)
                    .setTitle(text("Menu.confirm-title", "<gold>Confirm selection change"))
                    .setBackOnPlayerClose(true)).exceptionally(error -> { report(error); return null; });
        }

        private void search(ItemClick click) {
            var instructions = Pane.builder("#########", "####I####", "#########", "B#######C")
                    .addIngredient('I', button(Material.PAPER, "Menu.search-hint", "<gray>Type an entity name above, then go back.", ignored -> {}))
                    .addIngredient('B', button(Material.ARROW, "Menu.back", "<yellow>Back", back -> back.window().back()))
                    .addIngredient('C', button(Material.MILK_BUCKET, "Menu.clear", "<yellow>Clear search", clear -> {
                        query.set(""); pages.setPage(0); clear.window().back();
                    })).build();
            click.window().navigate(AnvilWindow.builder()
                    .setTitle(text("Menu.search", "<yellow>Search"))
                    .setUpperPane(Pane.builder("###").setFrozen(true).build())
                    .setLowerPane(instructions)
                    .setTextFieldAlwaysEnabled(true)
                    .setBackOnPlayerClose(true)
                    .addRenameHandler(input -> { query.set(input.strip()); pages.setPage(0); }))
                    .exceptionally(error -> { report(error); return null; });
        }
    }

    private void report(Throwable error) { plugin.getLogger().log(Level.WARNING, "CatchBall menu operation failed", error); }
}
