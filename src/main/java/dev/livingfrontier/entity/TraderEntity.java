package dev.livingfrontier.entity;

import dev.livingfrontier.village.TradeNetworkState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public final class TraderEntity extends AbstractVillager {
    private long originId;
    private long destinationId;
    private List<BlockPos> route = List.of();
    private int routeIndex;
    private long dwellUntil;

    public TraderEntity(EntityType<? extends TraderEntity> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
            ground.setCanPassDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 24).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.3));
        goalSelector.addGoal(2, new RoadTravelGoal(this, 0.85));
        goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Monster.class, 12, 1.15, 1.35));
        goalSelector.addGoal(4, new OpenDoorGoal(this, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public void setJourney(long originId, long destinationId, List<BlockPos> route) {
        this.originId = originId;
        this.destinationId = destinationId;
        this.route = compress(route);
        routeIndex = 0;
        dwellUntil = 0;
        offers = null;
        setPersistenceRequired();
    }

    public boolean hasJourney() {
        return originId != 0 && destinationId != 0 && !route.isEmpty();
    }

    public long originId() {
        return originId;
    }

    public long destinationId() {
        return destinationId;
    }

    public List<BlockPos> route() {
        return route;
    }

    boolean canTravel() {
        return hasJourney() && level().getGameTime() >= dwellUntil;
    }

    @Nullable
    BlockPos currentWaypoint() {
        return routeIndex >= 0 && routeIndex < route.size() ? route.get(routeIndex) : null;
    }

    void advanceWaypoint() {
        routeIndex++;
    }

    void recoverFromUnreachableWaypoint() {
        if (routeIndex < route.size() - 1) {
            routeIndex++;
        } else {
            routeIndex = Math.max(0, route.size() - 1);
            dwellUntil = level().getGameTime() + 100;
        }
    }

    void completeJourney() {
        if (!(level() instanceof ServerLevel server) || destinationId == 0) return;
        TradeNetworkState state = TradeNetworkState.get(server);
        BlockPos destination = state.settlement(destinationId).map(TradeNetworkState.SettlementView::center)
                .orElse(route.get(route.size() - 1));
        if (distanceToSqr(destination.getX() + 0.5, destination.getY() + 1, destination.getZ() + 0.5) > 64) {
            recoverFromUnreachableWaypoint();
            return;
        }
        state.arriveAndReload(destinationId, getInventory(), server.getGameTime() / 24000L);
        long previousOrigin = originId;
        originId = destinationId;
        destinationId = previousOrigin;
        List<BlockPos> reverse = new ArrayList<>(route);
        Collections.reverse(reverse);
        route = List.copyOf(reverse);
        routeIndex = 0;
        dwellUntil = server.getGameTime() + 200;
        offers = null;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
            @Nullable SpawnGroupData data, @Nullable CompoundTag spawnTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, spawnTag);
        setPersistenceRequired();
        return result;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.VILLAGER_SPAWN_EGG) || !isAlive() || isTrading() || isBaby()) {
            return super.mobInteract(player, hand);
        }
        if (hand == InteractionHand.MAIN_HAND) player.awardStat(Stats.TALKED_TO_VILLAGER);
        if (player.isShiftKeyDown()) {
            if (!level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.livingfrontier.trader.route",
                        originId, destinationId), false);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (getOffers().isEmpty()) return InteractionResult.sidedSuccess(level().isClientSide);
        if (!level().isClientSide) {
            setTradingPlayer(player);
            openTradingScreen(player, getDisplayName(), 1);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected void updateTrades() {
        if (level() instanceof ServerLevel server && destinationId != 0) {
            offers = TradeNetworkState.get(server).offers(destinationId, getInventory());
        } else {
            offers = new MerchantOffers();
        }
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        super.notifyTrade(offer);
        if (level() instanceof ServerLevel server) {
            TradeNetworkState.get(server).traderCompletedOffer(destinationId, getInventory(), offer);
        }
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        if (offer.shouldRewardExp() && !level().isClientSide) {
            level().addFreshEntity(new ExperienceOrb(level(), getX(), getY() + 0.5, getZ(), 2 + random.nextInt(3)));
        }
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("FrontierTradeOrigin", originId);
        tag.putLong("FrontierTradeDestination", destinationId);
        tag.putLongArray("FrontierTradeRoute", route.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putInt("FrontierTradeRouteIndex", routeIndex);
        tag.putLong("FrontierTradeDwellUntil", dwellUntil);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        originId = tag.getLong("FrontierTradeOrigin");
        destinationId = tag.getLong("FrontierTradeDestination");
        route = java.util.Arrays.stream(tag.getLongArray("FrontierTradeRoute")).mapToObj(BlockPos::of).toList();
        routeIndex = Math.max(0, Math.min(tag.getInt("FrontierTradeRouteIndex"), route.size()));
        dwellUntil = tag.getLong("FrontierTradeDwellUntil");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isTrading() ? SoundEvents.WANDERING_TRADER_TRADE : SoundEvents.WANDERING_TRADER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WANDERING_TRADER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WANDERING_TRADER_DEATH;
    }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean success) {
        return success ? SoundEvents.WANDERING_TRADER_YES : SoundEvents.WANDERING_TRADER_NO;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.WANDERING_TRADER_YES;
    }

    private static List<BlockPos> compress(List<BlockPos> route) {
        if (route.isEmpty()) return List.of();
        List<BlockPos> result = new ArrayList<>();
        result.add(route.get(0).immutable());
        for (int index = 8; index < route.size() - 1; index += 8) {
            result.add(route.get(index).immutable());
        }
        if (!result.get(result.size() - 1).equals(route.get(route.size() - 1))) {
            result.add(route.get(route.size() - 1).immutable());
        }
        return List.copyOf(result);
    }
}
