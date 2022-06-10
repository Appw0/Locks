package melonslise.locks.common.init;

import melonslise.locks.Locks;
import melonslise.locks.common.recipe.DisappearingEnchantmentRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;

public final class LocksRecipes {

    private LocksRecipes() {}

    public static void register(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(new DisappearingEnchantmentRecipe().setRegistryName(new ResourceLocation(Locks.ID, "disappearing_enchantment")));
    }
}
