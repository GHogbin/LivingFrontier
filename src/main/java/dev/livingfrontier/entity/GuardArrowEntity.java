package dev.livingfrontier.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;

public final class GuardArrowEntity extends Arrow {
    private boolean villageArrow;
    private @Nullable UUID intendedTarget;

    public GuardArrowEntity(EntityType<? extends GuardArrowEntity> type, Level level) {
        super(type, level);
        pickup = Pickup.DISALLOWED;
    }

    public void configure(LivingEntity shooter, LivingEntity target) {
        setOwner(shooter);
        setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
        villageArrow = shooter instanceof VillageGuardEntity;
        intendedTarget = target.getUUID();
        setBaseDamage(2);
        pickup = Pickup.DISALLOWED;
    }

    public static boolean protectedAlly(boolean villageArrow, @Nullable UUID target, Entity entity) {
        if (!villageArrow) {
            return entity instanceof RaiderEntity;
        }
        return entity instanceof VillageResidentEntity || entity instanceof Villager
                || entity instanceof Mob mob && mob.getType().getCategory().isFriendly()
                || entity instanceof Player && !entity.getUUID().equals(target);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !protectedAlly(villageArrow, intendedTarget, entity);
    }

    public boolean protects(Entity entity) {
        return protectedAlly(villageArrow, intendedTarget, entity);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("VillageArrow", villageArrow);
        if (intendedTarget != null) {
            tag.putUUID("IntendedTarget", intendedTarget);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        villageArrow = tag.getBoolean("VillageArrow");
        intendedTarget = tag.hasUUID("IntendedTarget") ? tag.getUUID("IntendedTarget") : null;
        pickup = Pickup.DISALLOWED;
    }
}
