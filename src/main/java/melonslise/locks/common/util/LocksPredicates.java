package melonslise.locks.common.util;

import net.minecraft.init.PotionTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;

import java.util.function.Predicate;

public final class LocksPredicates
{
	public static final Predicate<Lockable> LOCKED = lockable -> lockable.lock.isLocked();

	public static final Predicate<Lockable> NOT_LOCKED = LOCKED.negate();

	public static final Predicate<ItemStack> IS_INVISIBILITY = stack -> PotionUtils.getPotionFromItem(stack).equals(PotionTypes.INVISIBILITY) || PotionUtils.getPotionFromItem(stack).equals(PotionTypes.LONG_INVISIBILITY);

	private LocksPredicates() {}
}