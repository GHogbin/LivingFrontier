package dev.livingfrontier.village;

import dev.livingfrontier.FrontierConfig;
import dev.livingfrontier.LivingFrontier;
import dev.livingfrontier.entity.TravellerEntity;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LivingFrontier.MOD_ID)
public final class VillagerConversations {
    private VillagerConversations() {
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (FrontierConfig.VILLAGER_CONVERSATIONS.get() && event.getHand() == InteractionHand.MAIN_HAND
                && event.getEntity().isShiftKeyDown() && event.getTarget() instanceof Villager villager
                && villager.isAlive() && !villager.isBaby()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (event.getEntity() instanceof ServerPlayer player) {
                speak(villager, player);
            }
        }
    }

    public static Outcome speak(Villager villager, ServerPlayer player) {
        if (!FrontierConfig.VILLAGER_CONVERSATIONS.get() || !villager.isAlive() || villager.isBaby()
                || !player.isAlive() || player.isSpectator() || player.level() != villager.level()
                || player instanceof net.minecraftforge.common.util.FakePlayer || player.distanceToSqr(villager) > 36) {
            return Outcome.UNAVAILABLE;
        }
        ServerLevel level = (ServerLevel) villager.level();
        villager.getLookControl().setLookAt(player, 30, 30);
        if (player.isCreative()) {
            tell(player, "greeting." + greeting(villager));
            return Outcome.GREETING;
        }
        if (villager.getPlayerReputation(player) < -20) {
            tell(player, "unwelcome");
            return Outcome.UNAVAILABLE;
        }
        VillageLifeState state = VillageLifeState.get(level);
        long day = level.getGameTime() / 24000L;
        UUID id = player.getUUID();
        VillageLifeState.Errand job = state.getErrand(id);
        if (job == null || job.day() != day) {
            Supply supply = supplyFor(villager.getVillagerData().getProfession());
            state.setErrand(id, new VillageLifeState.Errand(villager.getUUID(), supply.name(), day, false));
            tell(player, "greeting." + greeting(villager));
            tell(player, "request", supply.count, new ItemStack(supply.item).getHoverName());
            return Outcome.REQUESTED;
        }
        if (job.completed()) {
            tell(player, "thanks_today");
            return Outcome.ALREADY_COMPLETED;
        }
        if (!job.villager().equals(villager.getUUID())) {
            tell(player, "other_errand");
            return Outcome.OTHER_VILLAGER;
        }
        Supply supply = Supply.valueOf(job.supply());
        ItemStack held = player.getMainHandItem();
        if (!held.is(supply.item) || held.getCount() < supply.count) {
            tell(player, "request", supply.count, new ItemStack(supply.item).getHoverName());
            return Outcome.WAITING;
        }
        // Mark completion before rewards; one persisted errand per player per world game-day.
        state.setErrand(id, new VillageLifeState.Errand(job.villager(), job.supply(), day, true));
        held.shrink(supply.count);
        ItemStack reward = new ItemStack(Items.EMERALD, 2);
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
        villager.getGossips().add(id, GossipType.MINOR_POSITIVE, 5);
        villager.playSound(SoundEvents.VILLAGER_YES, 0.8F, 1);
        tell(player, "completed");
        return Outcome.COMPLETED;
    }

    public static void speakTraveller(TravellerEntity traveller, Player player) {
        if (player.isSpectator()) {
            return;
        }
        traveller.getLookControl().setLookAt(player, 30, 30);
        int message = Math.floorMod(traveller.getUUID().hashCode() + (int) (traveller.level().getGameTime() / 24000L), 3);
        player.displayClientMessage(Component.translatable("message.livingfrontier.traveller." + message), false);
        traveller.playSound(SoundEvents.VILLAGER_AMBIENT, 0.5F, 1);
    }

    private static String greeting(Villager villager) {
        VillagerProfession profession = villager.getVillagerData().getProfession();
        if (profession == VillagerProfession.FARMER) return "farmer";
        if (profession == VillagerProfession.LIBRARIAN) return "librarian";
        if (profession == VillagerProfession.ARMORER || profession == VillagerProfession.WEAPONSMITH
                || profession == VillagerProfession.TOOLSMITH) return "smith";
        return "general";
    }

    public static Supply supplyFor(VillagerProfession profession) {
        if (profession == VillagerProfession.FARMER) return Supply.WHEAT;
        if (profession == VillagerProfession.LIBRARIAN) return Supply.PAPER;
        if (profession == VillagerProfession.ARMORER || profession == VillagerProfession.WEAPONSMITH
                || profession == VillagerProfession.TOOLSMITH) return Supply.IRON;
        if (profession == VillagerProfession.FLETCHER) return Supply.STICKS;
        return Supply.BREAD;
    }

    private static void tell(Player player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("message.livingfrontier.villager." + key, args), false);
    }

    public enum Supply {
        WHEAT(Items.WHEAT, 12), PAPER(Items.PAPER, 8), IRON(Items.IRON_INGOT, 3),
        STICKS(Items.STICK, 16), BREAD(Items.BREAD, 4);

        public final Item item;
        public final int count;

        Supply(Item item, int count) {
            this.item = item;
            this.count = count;
        }
    }

    public enum Outcome {
        UNAVAILABLE, GREETING, REQUESTED, WAITING, COMPLETED, ALREADY_COMPLETED, OTHER_VILLAGER
    }
}
