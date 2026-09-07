package ae2.container.crafting;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class RecipeSelection {
    private RecipeSelection() {
    }

    public static List<Candidate> findCandidates(InventoryCrafting input, World world) {
        List<Candidate> candidates = new ArrayList<>();
        for (IRecipe recipe : CraftingManager.REGISTRY) {
            ResourceLocation id = recipe.getRegistryName();
            if (id != null && recipe.matches(input, world)) {
                candidates.add(new Candidate(id, recipe, recipe.getCraftingResult(input).copy()));
            }
        }
        return List.copyOf(candidates);
    }

    @Nullable
    public static Candidate select(List<Candidate> candidates, @Nullable ResourceLocation preferredId) {
        if (preferredId != null) {
            for (Candidate candidate : candidates) {
                if (candidate.id().equals(preferredId)) {
                    return candidate;
                }
            }
        }
        return candidates.isEmpty() ? null : candidates.getFirst();
    }

    public record Candidate(ResourceLocation id, IRecipe recipe, ItemStack output) {
    }
}
