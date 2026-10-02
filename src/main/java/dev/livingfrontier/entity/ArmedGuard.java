package dev.livingfrontier.entity;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public interface ArmedGuard {
    boolean isArcher();

    void setArcher(boolean archer);

    void equipForRange(boolean ranged);

    boolean isHoldingBow();

    static void equip(PathfinderMob mob, boolean ranged) {
        if (ranged && !mob.getMainHandItem().is(Items.BOW)) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        } else if (!ranged && !mob.getMainHandItem().isEmpty()) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        // Melee weapons remain original model geometry, preserving existing attack attributes.
        mob.setDropChance(EquipmentSlot.MAINHAND, 0);
    }
}
