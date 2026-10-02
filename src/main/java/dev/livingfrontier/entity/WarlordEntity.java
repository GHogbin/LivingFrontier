package dev.livingfrontier.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class WarlordEntity extends RaiderEntity {
    private static final EntityDataAccessor<Boolean> CHARGING =
            SynchedEntityData.defineId(WarlordEntity.class, EntityDataSerializers.BOOLEAN);
    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.translatable("entity.livingfrontier.warlord"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private int slamCooldown = 100;
    private int chargeTicks;
    private boolean frontierSealed;

    public WarlordEntity(EntityType<? extends WarlordEntity> type, Level level) {
        super(type, level);
        xpReward = 80;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes().add(Attributes.MAX_HEALTH, 160).add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.ATTACK_DAMAGE, 8).add(Attributes.FOLLOW_RANGE, 28)
                .add(Attributes.ARMOR, 6).add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CHARGING, false);
    }

    public boolean isCharging() {
        return entityData.get(CHARGING);
    }

    public boolean isFrontierSealed() {
        return frontierSealed;
    }

    public void awakenFromSeals() {
        frontierSealed = false;
        setNoAi(false);
        playSound(SoundEvents.RAVAGER_ROAR, 1.5F, 0.8F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return (!frontierSealed || source.is(DamageTypes.GENERIC_KILL)) && super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("FrontierSealed", frontierSealed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        frontierSealed = tag.getBoolean("FrontierSealed");
        if (frontierSealed) {
            setNoAi(true);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) level();
        bossBar.setProgress(getHealth() / getMaxHealth());
        bossBar.setName(getDisplayName());
        if (!isAlive()) {
            return;
        }
        if (chargeTicks > 0) {
            navigation.stop();
            setDeltaMovement(getDeltaMovement().multiply(0.25, 1, 0.25));
            level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.15, getZ(), 8, 2.5, 0.1, 2.5, 0.01);
            if (--chargeTicks == 0) {
                entityData.set(CHARGING, false);
                slam(level);
                slamCooldown = getHealth() < getMaxHealth() / 2 ? 90 : 140;
            }
        } else if (slamCooldown > 0) {
            slamCooldown--;
        } else if (getTarget() != null && distanceToSqr(getTarget()) <= 36) {
            chargeTicks = 30;
            entityData.set(CHARGING, true);
            playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, 0.6F);
        }
    }

    private void slam(ServerLevel level) {
        playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.5F, 0.6F);
        level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.2, getZ(), 50, 3.0, 0.15, 3.0, 0.1);
        for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(4),
                player -> !player.isCreative() && !player.isSpectator() && player.isAlive())) {
            if (distanceToSqr(player) <= 16 && hasLineOfSight(player)) {
                player.hurt(damageSources().mobAttack(this), 6);
                player.knockback(0.8, getX() - player.getX(), getZ() - player.getZ());
            }
        }
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        super.die(source);
        bossBar.removeAllPlayers();
    }
}
