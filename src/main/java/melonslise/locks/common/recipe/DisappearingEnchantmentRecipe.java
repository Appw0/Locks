package melonslise.locks.common.recipe;

import melonslise.locks.common.init.LocksEnchantments;
import melonslise.locks.common.init.LocksItems;
import melonslise.locks.common.item.LockItem;
import melonslise.locks.common.util.LocksPredicates;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class DisappearingEnchantmentRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    private final ItemStack outStack = new ItemStack(LocksItems.WOOD_LOCK);

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        boolean hasLock = false;
        boolean hasPotion = false;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (hasLock && hasPotion) {
                    return false;
                } if (stack.getItem() instanceof LockItem && EnchantmentHelper.getEnchantmentLevel(LocksEnchantments.DISAPPEARING, stack) == 0) {
                    hasLock = true;
                } else if (LocksPredicates.IS_INVISIBILITY.test(stack)) {
                    hasPotion = true;
                } else {
                    return false;
                }
            }
        }
        return hasLock && hasPotion;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack lock = ItemStack.EMPTY;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof LockItem) {
                lock = stack.copy();
                lock.addEnchantment(LocksEnchantments.DISAPPEARING, 1);
                break;
            }
        }
        return lock;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        NonNullList<ItemStack> stacks = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
        for(int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (LocksPredicates.IS_INVISIBILITY.test(stack)) {
                stacks.set(i, stack.copy());
                break;
            }
        }
        return stacks;
    }

    @Override
    public boolean canFit(int width, int height) { return width * height >= 2; }

    @Override
    public ItemStack getRecipeOutput() { return outStack; }
}
