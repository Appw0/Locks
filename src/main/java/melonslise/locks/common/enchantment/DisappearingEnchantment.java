package melonslise.locks.common.enchantment;

import melonslise.locks.common.init.LocksEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.inventory.EntityEquipmentSlot;

public class DisappearingEnchantment extends Enchantment {

    public DisappearingEnchantment()
    {
        super(Rarity.UNCOMMON, LocksEnchantments.LOCK_TYPE, new EntityEquipmentSlot[] { EntityEquipmentSlot.MAINHAND });
    }

    @Override
    public int getMinEnchantability(int level)
    {
        return 0;
    }

    @Override
    public int getMaxEnchantability(int level)
    {
        return 0;
    }

    @Override
    public int getMaxLevel()
    {
        return 1;
    }

    @Override
    public boolean isTreasureEnchantment() { return true; }

}
