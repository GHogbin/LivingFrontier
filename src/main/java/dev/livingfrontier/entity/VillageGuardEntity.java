package dev.livingfrontier.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.item.Items;
import javax.annotation.Nullable;

public final class VillageGuardEntity extends VillageResidentEntity implements ArmedGuard {
    private static final EntityDataAccessor<Boolean> ARCHER =
            SynchedEntityData.defineId(VillageGuardEntity.class, EntityDataSerializers.BOOLEAN);
    private boolean roleAssigned;
    public VillageGuardEntity(EntityType<? extends VillageGuardEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 32).add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 5).add(Attributes.ARMOR, 4).add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        goalSelector.addGoal(2, new MoveTowardsRestrictionGoal(this, 1));
        goalSelector.addGoal(3, new GuardCombatGoal<>(this, 1.1));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                entity -> !(entity instanceof Creeper)));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ARCHER, false);
    }

    @Override
    public boolean isArcher() {
        return entityData.get(ARCHER);
    }

    @Override
    public void setArcher(boolean archer) {
        entityData.set(ARCHER, archer);
        roleAssigned = true;
        equipForRange(archer);
    }

    @Override
    public void equipForRange(boolean ranged) {
        ArmedGuard.equip(this, ranged && isArcher());
    }

    @Override
    public boolean isHoldingBow() {
        return getMainHandItem().is(Items.BOW);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
            @Nullable SpawnGroupData data, @Nullable CompoundTag spawnTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, spawnTag);
        if (!roleAssigned) {
            setArcher(random.nextBoolean());
        }
        return result;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("FrontierArcher", isArcher());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setArcher(tag.contains("FrontierArcher") ? tag.getBoolean("FrontierArcher")
                : (getUUID().getLeastSignificantBits() & 1) == 0);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            if (!level().isClientSide) {
                player.displayClientMessage(Component.translatable(isArcher() ? "message.livingfrontier.guard.archer"
                        : "message.livingfrontier.guard"), false);
                playSound(SoundEvents.VILLAGER_YES, 0.5F, 0.9F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }
}
