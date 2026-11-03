package com.github.nutt1101.Recipe;

import com.github.nutt1101.CatchBall;
import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.items.Ball;
import com.github.nutt1101.items.DropItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;


public class BallRecipe {
    public BallRecipe() {
        NamespacedKey key = new NamespacedKey(CatchBall.plugin, "ballKey");
        if (!ConfigSetting.recipeEnabled) {
            Bukkit.removeRecipe(key);
            return;
        }
        FileConfiguration config = CatchBall.plugin.getConfig();
        ShapedRecipe recipe = new ShapedRecipe(key, Ball.makeBall());
        recipe.shape(config.getStringList("recipe.shape").toArray(String[]::new));
        ConfigurationSection ingredients = config.getConfigurationSection("recipe.ingredients");
        for (String symbol : ingredients.getKeys(false)) {
            Object ingredient = ingredients.get(symbol);
            if (ingredient instanceof String name) {
                if (name.equalsIgnoreCase("DROPITEM")) {
                    recipe.setIngredient(symbol.charAt(0), new RecipeChoice.ExactChoice(DropItem.makeDropItem()));
                } else {
                    recipe.setIngredient(symbol.charAt(0), Material.matchMaterial(name));
                }
            } else {
                recipe.setIngredient(symbol.charAt(0), new RecipeChoice.MaterialChoice(
                        ingredients.getStringList(symbol).stream().map(Material::matchMaterial).toList()));
            }
        }
        Bukkit.removeRecipe(key);
        Bukkit.addRecipe(recipe);
    }
}
