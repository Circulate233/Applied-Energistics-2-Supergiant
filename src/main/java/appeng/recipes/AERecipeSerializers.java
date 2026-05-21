package appeng.recipes;

import appeng.core.AppEng;
import appeng.recipes.entropy.EntropyRecipeSerializer;
import appeng.recipes.game.CraftingUnitTransformRecipeSerializer;
import appeng.recipes.game.StorageCellDisassemblyRecipeSerializer;
import appeng.recipes.handlers.ChargerRecipeSerializer;
import appeng.recipes.handlers.InscriberRecipeSerializer;
import appeng.recipes.mattercannon.MatterCannonAmmoSerializer;
import appeng.recipes.transform.TransformRecipeSerializer;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.JsonContext;

import java.util.Map;

public final class AERecipeSerializers {
    private static final Map<ResourceLocation, IAERecipeFactory> FACTORIES = new Object2ObjectOpenHashMap<>();

    static {
        register(AppEng.makeId("charger"), new ChargerRecipeSerializer());
        register(AppEng.makeId("inscriber"), new InscriberRecipeSerializer());
        register(AppEng.makeId("matter_cannon"), new MatterCannonAmmoSerializer());
        register(AppEng.makeId("transform"), new TransformRecipeSerializer());
        register(AppEng.makeId("cell_disassembly"), new StorageCellDisassemblyRecipeSerializer());
        register(AppEng.makeId("storage_cell_disassembly"), new StorageCellDisassemblyRecipeSerializer());
        register(AppEng.makeId("crafting_unit_transform"), new CraftingUnitTransformRecipeSerializer());
        register(AppEng.makeId("entropy"), new EntropyRecipeSerializer());
    }

    private AERecipeSerializers() {
    }

    public static void register(ResourceLocation id, IAERecipeFactory factory) {
        FACTORIES.put(id, factory);
    }

    public static void register(JsonObject json, JsonContext ctx) {
        if (!appeng.recipes.serializers.JsonRecipeUtils.shouldLoad(json)) {
            return;
        }

        String type = ctx.appendModId(json.get("type").getAsString());
        IAERecipeFactory factory = FACTORIES.get(new ResourceLocation(type));
        if (factory == null) {
            return;
        }

        factory.register(json, ctx);
    }
}
