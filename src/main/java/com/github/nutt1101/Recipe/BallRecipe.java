package com.github.nutt1101.Recipe;

import com.github.nutt1101.CatchBall;
import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.items.Ball;
import com.github.nutt1101.items.DropItem;
import org.bukkit.Bukkit;
import com.github.nutt1101.utils.GameTypes;
import org.bukkit.NamespacedKey;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;


public class BallRecipe {
    public BallRecipe() {
        NamespacedKey key = new NamespacedKey(CatchBall.plugin, "ballKey");
        if (!ConfigSetting.recipeEnabled) {
            Bukkit.removeRecipe(key);
            return;
        }
        YamlDocument config = ConfigSetting.configuration();
        ShapedRecipe recipe = new ShapedRecipe(key, Ball.makeBall());
        recipe.shape(config.getStringList("recipe.shape").toArray(String[]::new));
        Section ingredients = config.getSection("recipe.ingredients");
        for (String symbol : ingredients.getRoutesAsStrings(false)) {
            Object ingredient = ingredients.get(symbol);
            if (ingredient instanceof String name) {
                if (name.equalsIgnoreCase("DROPITEM")) {
                    recipe.setIngredient(symbol.charAt(0), new RecipeChoice.ExactChoice(DropItem.makeDropItem()));
                } else {
                    recipe.setIngredient(symbol.charAt(0), GameTypes.material(name));
                }
            } else {
                recipe.setIngredient(symbol.charAt(0), new RecipeChoice.MaterialChoice(
                        ingredients.getStringList(symbol).stream().map(GameTypes::material).toList()));
            }
        }
        Bukkit.removeRecipe(key);
        Bukkit.addRecipe(recipe);
    }
}
