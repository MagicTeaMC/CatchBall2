package com.github.nutt1101.configuration;

import org.bukkit.Material;
import com.github.nutt1101.utils.GameTypes;
import org.bukkit.Particle;
import org.bukkit.Sound;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.block.Block;
import dev.dejvokep.boostedyaml.block.Comments;
import dev.dejvokep.boostedyaml.utils.format.NodeRole;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import java.io.ByteArrayInputStream;
import org.bukkit.entity.EntityType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** Public configuration format, legacy migration, and validation. No live settings are changed here. */
public final class ConfigSchema {
    public static final int VERSION = 2;
    public static final Map<String, String> LEGACY_PATHS = legacyPaths();

    /** Material capabilities come from the running server; injectable for headless schema tests. */
    interface MaterialRules {
        boolean isItem(Material material);
        boolean isBlock(Material material);
        default Material material(String name) { return Material.matchMaterial(name); }
        default boolean isParticle(String name) {
            try { Particle.valueOf(name); return true; }
            catch (IllegalArgumentException error) { return false; }
        }
        default boolean isSound(String name) {
            try { Sound.valueOf(name); return true; }
            catch (IllegalArgumentException error) { return false; }
        }
    }

    private static final MaterialRules SERVER_MATERIALS = new MaterialRules() {
        public Material material(String name) { return GameTypes.material(name); }
        public boolean isSound(String name) { return GameTypes.sound(name) != null; }
        public boolean isParticle(String name) {
            Particle particle = GameTypes.particle(name);
            return particle != null && particle.getDataType() == Void.class;
        }
        public boolean isItem(Material material) { return !material.isAir() && material.isItem(); }
        public boolean isBlock(Material material) { return !material.isAir() && material.isBlock(); }
    };

    private ConfigSchema() {}

    private static Map<String, String> legacyPaths() {
        Map<String, String> paths = new LinkedHashMap<>();
        paths.put("Locale", "messages.locale");
        paths.put("DropEnable", "drops.enabled");
        paths.put("DropNeedPermission", "drops.require-permission");
        paths.put("DropItemChance", "drops.chance");
        paths.put("DropItemMaterial", "drops.material");
        paths.put("DropMethod", "drops.method");
        paths.put("DropEntityType", "drops.entity-type");
        paths.put("DropBlockType", "drops.block-type");
        paths.put("CatchSuccessSound", "capture.success-sound");
        paths.put("AllowCatchableTamedOwnerIsNull", "capture.allow-ownerless-tamed");
        paths.put("ShowParticles", "capture.particles.enabled");
        paths.put("CustomParticles", "capture.particles.type");
        paths.put("catchFailRate", "capture.fail-rate");
        paths.put("customModelData", "items.catch-ball.custom-model-data");
        paths.put("ballCustomModelData", "items.drop-item.custom-model-data");
        paths.put("CatchableEntity", "capture.catchable-entities");
        paths.put("Recipe.enabled", "recipe.enabled");
        paths.put("Recipe.key", "recipe.ingredients");
        paths.put("Recipe.shape", "recipe.shape");
        paths.put("UseRes", "integrations.residence.enabled");
        paths.put("ResidenceFlag", "integrations.residence.flags");
        paths.put("UseGF", "integrations.grief-prevention.enabled");
        paths.put("GriefPreventionFlag", "integrations.grief-prevention.flags");
        paths.put("UseLands", "integrations.lands.enabled");
        paths.put("UseRP", "integrations.red-protect.enabled");
        paths.put("UseSCS", "integrations.simple-claim-system.enabled");
        paths.put("UseTowny", "integrations.towny.enabled");
        paths.put("UsePAPI", "integrations.placeholder-api.enabled");
        paths.put("UseMM", "integrations.mythic-mobs.enabled");
        return Collections.unmodifiableMap(paths);
    }

    public static YamlDocument parse(String contents) {
        try {
            return YamlDocument.create(new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)),
                    LoaderSettings.builder().setAutoUpdate(false).setCreateFileIfAbsent(false)
                            .setAllowDuplicateKeys(false).setDetailedErrors(false).build());
        } catch (IOException | RuntimeException error) {
            throw new IllegalArgumentException("Invalid YAML; check indentation, duplicate keys and value syntax", error);
        }
    }

    public static YamlDocument prepare(String contents, String defaults, Consumer<String> warning) {
        return prepare(contents, defaults, warning, SERVER_MATERIALS);
    }

    static YamlDocument prepare(String contents, String defaults, Consumer<String> warning, MaterialRules materials) {
        YamlDocument source = parse(contents);
        YamlDocument result = parse(defaults);
        Object version = source.get("config-version");
        if (version != null && (!(version instanceof Integer number) || number < 1 || number > VERSION)) {
            throw invalid("config-version", "must be an integer from 1 to " + VERSION);
        }
        for (var entry : LEGACY_PATHS.entrySet()) {
            String oldPath = entry.getKey();
            String newPath = entry.getValue();
            if (source.contains(oldPath)) {
                if (source.contains(newPath)) {
                    warning.accept("Both " + oldPath + " and " + newPath + " are set; using " + newPath);
                } else {
                    requireSections(source, newPath);
                    source.move(oldPath, newPath);
                }
                source.remove(oldPath);
            }
        }
        // Drop the empty legacy container, retaining unknown children for administrator extensions.
        if (source.isSection("Recipe") && source.getSection("Recipe").getRoutesAsStrings(false).isEmpty()) {
            source.remove("Recipe");
        }
        for (String path : result.getRoutesAsStrings(true)) {
            requireSections(source, path);
            if (result.isSection(path) && source.contains(path) && !source.isSection(path)) {
                throw invalid(path, "must be a section");
            }
        }
        // Ingredients are a user-defined mapping: do not merge the default I into a custom recipe.
        if (source.contains("recipe.ingredients")) {
            Section ingredients = result.getSection("recipe.ingredients");
            for (String key : new HashSet<>(ingredients.getRoutesAsStrings(false))) ingredients.remove(key);
        }
        for (String path : source.getRoutesAsStrings(true)) {
            Block<?> sourceBlock = source.getBlock(path);
            if (source.isSection(path)) {
                if (!result.isSection(path)) result.createSection(path);
                // Keep administrator comments on sections, including inline comments.
                for (NodeRole role : NodeRole.values()) {
                    for (Comments.Position position : Comments.Position.values()) {
                        var comments = Comments.get(sourceBlock, role, position);
                        if (comments != null && !comments.isEmpty())
                            Comments.set(result.getBlock(path), role, position, comments);
                    }
                }
            } else {
                // Keep administrator comments, filling absent notes from the bundled documentation.
                Block<?> bundledBlock = result.getBlock(path);
                result.set(path, sourceBlock);
                if (bundledBlock != null) {
                    for (NodeRole role : NodeRole.values()) {
                        for (Comments.Position position : Comments.Position.values()) {
                            var custom = Comments.get(sourceBlock, role, position);
                            var fallback = Comments.get(bundledBlock, role, position);
                            if ((custom == null || custom.isEmpty()) && fallback != null && !fallback.isEmpty())
                                Comments.set(sourceBlock, role, position, fallback);
                        }
                    }
                }
            }
        }
        Object chance = result.get("drops.chance");
        if (chance instanceof String text && text.endsWith("%")) {
            try {
                result.set("drops.chance", Integer.parseInt(text.substring(0, text.length() - 1).trim()));
            } catch (NumberFormatException e) {
                throw invalid("drops.chance", "must be a percentage from 0 to 100");
            }
        }
        result.set("config-version", VERSION);
        validate(result, parse(defaults), materials);
        return result;
    }

    private static void requireSections(Section config, String path) {
        for (int dot = path.indexOf('.'); dot >= 0; dot = path.indexOf('.', dot + 1)) {
            String parent = path.substring(0, dot);
            if (config.contains(parent) && !config.isSection(parent)) {
                throw invalid(parent, "must be a section");
            }
        }
    }

    private static void validate(YamlDocument config, YamlDocument defaults, MaterialRules materials) {
        for (String path : defaults.getRoutesAsStrings(true)) {
            if (defaults.isSection(path) || path.startsWith("recipe.ingredients.")) continue;
            Object value = config.get(path);
            Object fallback = defaults.get(path);
            if (fallback instanceof Boolean && !(value instanceof Boolean)) throw invalid(path, "must be true or false");
            if (fallback instanceof String && !(value instanceof String)
                    && !(path.equals("capture.success-sound") && Boolean.FALSE.equals(value))) {
                throw invalid(path, "must be text");
            }
            if (fallback instanceof List<?> && (!(value instanceof List<?> list) || list.stream().anyMatch(v -> !(v instanceof String)))) {
                throw invalid(path, "must be a list of text values");
            }
        }
        range(config, "drops.chance", 0, 100, true);
        range(config, "capture.fail-rate", 0, 1, false);
        range(config, "items.catch-ball.custom-model-data", 0, Integer.MAX_VALUE, true);
        range(config, "items.drop-item.custom-model-data", 0, Integer.MAX_VALUE, true);
        String locale = config.getString("messages.locale");
        if (!locale.matches("[A-Za-z0-9_-]+")) throw invalid("messages.locale", "must be a locale file name without a path");
        material(config.getString("drops.material"), "drops.material", true, materials);
        Material block = material(config.getString("drops.block-type"), "drops.block-type", false, materials);
        if (!materials.isBlock(block)) throw invalid("drops.block-type", "must name a block material");
        enumValue(config, "drops.entity-type", EntityType.class);
        String method = config.getString("drops.method").toUpperCase(Locale.ROOT);
        if (!Set.of("CHICKEN", "ENTITY", "BLOCK").contains(method)) throw invalid("drops.method", "must be CHICKEN, ENTITY or BLOCK");
        config.set("drops.method", method);
        String particle = config.getString("capture.particles.type").toUpperCase(Locale.ROOT);
        if (!materials.isParticle(particle)) throw invalid("capture.particles.type", "must name an available particle that needs no extra data");
        config.set("capture.particles.type", particle);
        String sound = String.valueOf(config.get("capture.success-sound"));
        if (!sound.equalsIgnoreCase("false")) {
            sound = sound.toUpperCase(Locale.ROOT);
            if (!materials.isSound(sound)) throw invalid("capture.success-sound", "unknown Sound name");
            config.set("capture.success-sound", sound);
        } else config.set("capture.success-sound", "false");
        // Unknown entity names are deliberately kept for cross-version server configurations.
        config.set("capture.catchable-entities", new ArrayList<>(new LinkedHashSet<>(config.getStringList("capture.catchable-entities")
                .stream().map(s -> s.toUpperCase(Locale.ROOT)).toList())));
        validateRecipe(config, materials);
    }

    private static void range(YamlDocument config, String path, double min, double max, boolean integer) {
        Object value = config.get(path);
        if (!(value instanceof Number n) || !Double.isFinite(n.doubleValue()) || n.doubleValue() < min
                || n.doubleValue() > max || (integer && n.doubleValue() != Math.rint(n.doubleValue()))) {
            throw invalid(path, "must be " + (integer ? "an integer" : "a number") + " from " + min + " to " + max);
        }
    }

    private static <E extends Enum<E>> void enumValue(YamlDocument config, String path, Class<E> type) {
        String value = config.getString(path).toUpperCase(Locale.ROOT);
        try {
            Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw invalid(path, "unknown " + type.getSimpleName() + " name");
        }
        config.set(path, value);
    }

    private static Material material(String name, String path, boolean item, MaterialRules materials) {
        Material material = materials.material(name);
        if (material == null || (item && !materials.isItem(material))) throw invalid(path, "must name a valid " + (item ? "item" : "material"));
        return material;
    }

    private static void validateRecipe(YamlDocument config, MaterialRules materials) {
        List<String> shape = config.getStringList("recipe.shape");
        if (shape.isEmpty() || shape.size() > 3 || shape.getFirst().isEmpty() || shape.getFirst().length() > 3
                || shape.stream().anyMatch(row -> row.length() != shape.getFirst().length())
                || shape.stream().allMatch(String::isBlank)) throw invalid("recipe.shape", "must have 1–3 equal-width rows of 1–3 characters and at least one ingredient");
        Section ingredients = config.getSection("recipe.ingredients");
        if (ingredients == null) throw invalid("recipe.ingredients", "must be a section");
        Set<Character> used = new HashSet<>();
        shape.forEach(row -> row.chars().filter(c -> c != ' ').forEach(c -> used.add((char) c)));
        for (char symbol : used) {
            if (!ingredients.contains(String.valueOf(symbol))) throw invalid("recipe.ingredients", "missing ingredient for " + symbol);
        }
        for (String key : ingredients.getRoutesAsStrings(false)) {
            String path = "recipe.ingredients." + key;
            if (key.length() != 1 || key.equals(" ") || !used.contains(key.charAt(0))) throw invalid(path, "must be a single character used in the shape");
            Object value = ingredients.get(key);
            if (value instanceof String name) {
                if (!name.equalsIgnoreCase("DROPITEM")) material(name, path, true, materials);
            } else if (value instanceof List<?> list && !list.isEmpty()) {
                for (Object entry : list) {
                    if (!(entry instanceof String name)) throw invalid(path, "must contain material names");
                    material(name, path, true, materials);
                }
            } else throw invalid(path, "must be DROPITEM, a material name, or a nonempty material list");
        }
    }

    public static YamlDocument load(Path file, InputStream bundled, Consumer<String> warning) {
        return load(file, bundled, warning, SERVER_MATERIALS);
    }

    static YamlDocument load(Path file, InputStream bundled, Consumer<String> warning, MaterialRules materials) {
        if (bundled == null) throw new IllegalArgumentException("Missing bundled config.yml");
        try (bundled) {
            String defaults = new String(bundled.readAllBytes(), StandardCharsets.UTF_8);
            String original = Files.exists(file) ? Files.readString(file) : defaults;
            YamlDocument candidate = prepare(original, defaults, warning, materials);
            YamlDocument previous = parse(original);
            boolean migrating = !Objects.equals(previous.get("config-version"), VERSION)
                    || LEGACY_PATHS.keySet().stream().anyMatch(previous::contains);
            if (Files.exists(file) && migrating) {
                Path backup = file.resolveSibling("config.yml.bak");
                if (!Files.exists(backup)) Files.copy(file, backup);
            }
            save(file, candidate);
            return candidate;
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read or save config.yml", e);
        }
    }

    /** Write beside the target then replace it, so interrupted writes cannot truncate the config. */
    public static void save(Path file, YamlDocument config) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temp = Files.createTempFile(file.toAbsolutePath().getParent(), "config-", ".tmp");
        try {
            Files.writeString(temp, config.dump(), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static IllegalArgumentException invalid(String path, String reason) {
        return new IllegalArgumentException(path + " " + reason);
    }
}
