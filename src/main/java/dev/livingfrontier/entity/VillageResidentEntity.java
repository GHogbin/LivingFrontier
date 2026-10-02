package dev.livingfrontier.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;

public abstract class VillageResidentEntity extends PathfinderMob {
    private @Nullable BlockPos villageHome;

    protected VillageResidentEntity(EntityType<? extends VillageResidentEntity> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
            ground.setCanPassDoors(true);
        }
    }

    public void setVillageHome(BlockPos home) {
        villageHome = home.immutable();
        restrictTo(home, 32);
        setPersistenceRequired();
    }

    public boolean isVillageResident() {
        return villageHome != null;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !isVillageResident() && distance > 128 * 128;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (villageHome != null) {
            tag.putLong("FrontierVillageHome", villageHome.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("FrontierVillageHome")) {
            setVillageHome(BlockPos.of(tag.getLong("FrontierVillageHome")));
        }
    }
}
