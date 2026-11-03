package com.github.nutt1101.configuration;

import org.junit.jupiter.api.Test;
import org.bukkit.Material;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigSchemaTest {
    @TempDir Path directory;

    // Paper resolves capabilities through server registries. Schema tests use the capabilities
    // of the few fixture materials, while name resolution still uses the real Material enum.
    private static final ConfigSchema.MaterialRules MATERIALS = new ConfigSchema.MaterialRules() {
        public boolean isItem(Material material) {
            return java.util.Set.of(Material.EGG, Material.DIAMOND, Material.DIAMOND_ORE).contains(material);
        }
        public boolean isSound(String name) { return name.equals("ENTITY_ARROW_HIT_PLAYER"); }
        public boolean isBlock(Material material) { return material == Material.DIAMOND_ORE; }
    };

    private String defaults() throws Exception {
        try (var stream = getClass().getResourceAsStream("/config.yml")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test void migratesLegacyValuesWithoutLosingCustomSettings() throws Exception {
        String legacy = """
                Locale: zh_tw
                DropEnable: false
                DropItemChance: 73%
                catchFailRate: 0.42
                customModelData: 123
                ballCustomModelData: 456
                CatchableEntity: [cow, SHEEP, cow]
                Recipe:
                  enabled: true
                  key:
                    X: [EGG, DIAMOND]
                  shape: [XX]
                custom-extension: keep-me
                """;
        var result = ConfigSchema.prepare(legacy, defaults(), ignored -> {}, MATERIALS);
        assertEquals("zh_tw", result.getString("messages.locale"));
        assertFalse(result.getBoolean("drops.enabled"));
        assertEquals(73, result.getInt("drops.chance"));
        assertEquals(0.42, result.getDouble("capture.fail-rate"));
        assertEquals(123, result.getInt("items.catch-ball.custom-model-data"));
        assertEquals(456, result.getInt("items.drop-item.custom-model-data"));
        assertEquals(List.of("COW", "SHEEP"), result.getStringList("capture.catchable-entities"));
        assertEquals(List.of("EGG", "DIAMOND"), result.getStringList("recipe.ingredients.X"));
        assertFalse(result.contains("recipe.ingredients.I"));
        assertFalse(result.contains("Recipe"));
        assertFalse(result.contains("Locale"));
        assertEquals("keep-me", result.getString("custom-extension"));
        assertEquals(2, result.getInt("config-version"));
    }

    @Test void newPathsWinAndMigrationIsIdempotent() throws Exception {
        List<String> warnings = new ArrayList<>();
        var result = ConfigSchema.prepare("DropEnable: true\ndrops:\n  enabled: false\n", defaults(), warnings::add, MATERIALS);
        assertFalse(result.getBoolean("drops.enabled"));
        assertEquals(1, warnings.size());
        String serialized = result.dump();
        assertEquals(serialized, ConfigSchema.prepare(serialized, defaults(), ignored -> {}, MATERIALS).dump());
    }

    @Test void rejectsBadValuesAndRecipeBeforeWriting() throws Exception {
        for (String invalid : List.of(
                "drops:\n  chance: 101\n", "capture:\n  fail-rate: -0.1\n",
                "drops:\n  enabled: yes-please\n", "drops: false\n",
                "items:\n  catch-ball:\n    custom-model-data: 1.5\n",
                "drops:\n  material: DOES_NOT_EXIST\n", "drops:\n  block-type: EGG\n",
                "recipe:\n  shape: [XYZ]\n", "recipe:\n  shape: ['II', 'I']\n",
                "recipe:\n  ingredients:\n    I: [EGG, NO_SUCH_ITEM]\n",
                "messages:\n  locale: ../en\n", "config-version: 999\n", "drops: [\n")) {
            Path file = directory.resolve("config.yml");
            Files.writeString(file, invalid);
            assertThrows(IllegalArgumentException.class, () -> ConfigSchema.load(file,
                    new ByteArrayInputStream(defaults().getBytes(StandardCharsets.UTF_8)), ignored -> {}, MATERIALS), invalid);
            assertEquals(invalid, Files.readString(file));
            assertFalse(Files.exists(directory.resolve("config.yml.bak")));
        }
    }

    @Test void backsUpOriginalOnlyOnce() throws Exception {
        Path file = directory.resolve("config.yml");
        String original = "DropItemChance: 37%\n";
        Files.writeString(file, original);
        for (int i = 0; i < 2; i++) {
            var result = ConfigSchema.load(file, new ByteArrayInputStream(defaults().getBytes(StandardCharsets.UTF_8)), ignored -> {}, MATERIALS);
            assertEquals(37, result.getInt("drops.chance"));
            assertEquals(original, Files.readString(directory.resolve("config.yml.bak")));
        }
    }

    @Test void migratesEveryLegacyPathAndSavesGuiEditsToNewPath() throws Exception {
        var bundled = ConfigSchema.parse(defaults());
        var legacy = ConfigSchema.parse("");
        for (var entry : ConfigSchema.LEGACY_PATHS.entrySet()) {
            Object value = bundled.get(entry.getValue());
            if (value instanceof dev.dejvokep.boostedyaml.block.implementation.Section section) {
                legacy.set(entry.getKey(), section.getStringRouteMappedValues(false));
            } else {
                legacy.set(entry.getKey(), value);
            }
        }
        var migrated = ConfigSchema.prepare(legacy.dump(), defaults(), ignored -> {}, MATERIALS);
        for (var entry : ConfigSchema.LEGACY_PATHS.entrySet()) {
            assertFalse(migrated.contains(entry.getKey()), entry.getKey());
            assertTrue(migrated.contains(entry.getValue()), entry.getValue());
        }
        migrated.set("capture.catchable-entities", List.of("COW"));
        Path file = directory.resolve("config.yml");
        ConfigSchema.save(file, migrated);
        var reloaded = ConfigSchema.load(file, new ByteArrayInputStream(defaults().getBytes(StandardCharsets.UTF_8)), ignored -> {}, MATERIALS);
        assertEquals(List.of("COW"), reloaded.getStringList("capture.catchable-entities"));
        assertFalse(reloaded.contains("CatchableEntity"));
    }

    @Test void rejectsDuplicateKeysAndPreservesMigrationComments() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> ConfigSchema.prepare(
                "drops:\n  chance: 20\n  chance: 30\n", defaults(), ignored -> {}, MATERIALS));
        var result = ConfigSchema.prepare("""
                # My custom capture list
                CatchableEntity: [COW] # Keep this note
                Recipe:
                  key:
                    # My recipe ingredient
                    X: EGG
                  shape: [X]
                custom-extension: retained # Plugin extension
                """, defaults(), ignored -> {}, MATERIALS);
        String saved = result.dump();
        assertTrue(saved.contains("My custom capture list"));
        assertTrue(saved.contains("Keep this note"));
        assertTrue(saved.contains("My recipe ingredient"));
        assertTrue(saved.contains("Plugin extension"));
        assertEquals(saved, ConfigSchema.prepare(saved, defaults(), ignored -> {}, MATERIALS).dump());
    }

    @Test void acceptsDisabledSoundAndFutureEntityNames() throws Exception {
        var result = ConfigSchema.prepare("capture:\n  success-sound: false\n  catchable-entities: [FUTURE_MOB]\n", defaults(), ignored -> {}, MATERIALS);
        assertEquals("false", result.getString("capture.success-sound"));
        assertEquals(List.of("FUTURE_MOB"), result.getStringList("capture.catchable-entities"));
    }
}
