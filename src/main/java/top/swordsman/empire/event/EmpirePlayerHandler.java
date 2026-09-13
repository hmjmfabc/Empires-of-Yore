package top.swordsman.empire.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import top.swordsman.empire.EmpiresOfYoreMod;
import top.swordsman.empire.effect.EmpireEffects;

/**
 * ⑭c 神格(饱食度冻结) / ⑭e 亡灵形态(阳光·睡觉·腐肉) / ⑯b 兵粮寸断 / ⑯c 乐不思蜀 / ⑮ 冰霜疾行与摔倒。
 */
@EventBusSubscriber(modid = EmpiresOfYoreMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class EmpirePlayerHandler {

    /** 神格: 记录进入效果时的饱食度/饱和度, 保证"不减少"。 */
    private static final Map<UUID, float[]> DIVINITY_FOOD = new HashMap<>();
    /** 兵粮寸断: 进食前的饱食度, 用于按比例回收。 */
    private static final Map<UUID, float[]> FOOD_BEFORE_EAT = new HashMap<>();

    private EmpirePlayerHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        handleDivinity(player, level);
        handleUndeadForm(player, level);
        handleFrostStride(player, level);
        handleFallen(player);
    }

    /* ================= ⑭c 神格: 饱食度与饱和度不减少 ================= */
    private static void handleDivinity(Player player, ServerLevel level) {
        boolean active = EmpireEffects.level(player, EmpiresOfYoreMod.DIVINITY) > 0;
        FoodData food = player.getFoodData();
        if (!active) {
            DIVINITY_FOOD.remove(player.getUUID());
            return;
        }
        float[] remembered = DIVINITY_FOOD.computeIfAbsent(player.getUUID(),
                id -> new float[]{food.getFoodLevel(), food.getSaturationLevel()});
        if (food.getFoodLevel() < remembered[0]) {
            food.setFoodLevel((int) remembered[0]);
        }
        if (food.getSaturationLevel() < remembered[1]) {
            food.setSaturation(remembered[1]);
        }
        remembered[0] = food.getFoodLevel();
        remembered[1] = food.getSaturationLevel();
    }

    /* ================= ⑭e 亡灵形态: 阳光灼伤 + 睡觉限制 ================= */
    private static void handleUndeadForm(Player player, ServerLevel level) {
        if (EmpireEffects.level(player, EmpiresOfYoreMod.UNDEAD_FORM) <= 0) {
            return;
        }
        // 阳光下灼伤(佩戴头盔免疫)
        boolean helmet = !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
        if (!helmet && level.isDay() && !level.isRainingAt(player.blockPosition())
                && level.canSeeSky(player.blockPosition()) && !player.isInWaterOrRain()) {
            player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 40));
        }
    }

    /** ⑭e 亡灵形态: 只能在白天睡觉。 */
    @SubscribeEvent
    public static void onSleep(CanPlayerSleepEvent event) {
        Player player = event.getEntity();
        if (EmpireEffects.level(player, EmpiresOfYoreMod.UNDEAD_FORM) <= 0) {
            return;
        }
        if (!player.level().isDay()) {
            event.setProblem(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
        }
    }

    /** ⑭e 亡灵形态: 腐肉不再带来饥饿。 */
    @SubscribeEvent
    public static void onEatUndead(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getItem().is(net.minecraft.world.item.Items.ROTTEN_FLESH)
                && EmpireEffects.level(player, EmpiresOfYoreMod.UNDEAD_FORM) > 0) {
            player.removeEffect(MobEffects.HUNGER);
        }
    }

    /* ================= ⑯b 兵粮寸断: 饱食度只回复 (100-20n)%, 不回复饱和度 ================= */
    @SubscribeEvent
    public static void onEatCutRations(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player) {
            FOOD_BEFORE_EAT.put(player.getUUID(), new float[]{
                    player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel()});
        }
    }

    @SubscribeEvent
    public static void onEatCutRationsFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        float[] before = FOOD_BEFORE_EAT.remove(player.getUUID());
        int n = EmpireEffects.level(player, EmpiresOfYoreMod.CUT_RATIONS);
        if (n <= 0 || before == null) {
            return;
        }
        FoodData food = player.getFoodData();
        int gained = Math.max(0, food.getFoodLevel() - (int) before[0]);
        int allowed = (int) Math.floor(gained * (100 - 20 * n) / 100.0);
        food.setFoodLevel((int) before[0] + allowed);
        food.setSaturation(before[1]);
    }

    /* ================= ⑯c 乐不思蜀: 无法打开容器 / 无法拾取 ================= */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (EmpireEffects.level(player, EmpiresOfYoreMod.INDULGENCE) <= 0) {
            return;
        }
        BlockState state = player.level().getBlockState(event.getPos());
        if (state.hasBlockEntity() || state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)
                || state.is(Blocks.BARREL) || state.is(Blocks.ENDER_CHEST) || state.is(Blocks.SHULKER_BOX)) {
            if (state.getMenuProvider(player.level(), event.getPos()) != null) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        if (EmpireEffects.level(event.getPlayer(), EmpiresOfYoreMod.INDULGENCE) > 0) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    /* ================= ⑮ 冰霜疾行 ================= */
    private static void handleFrostStride(Player player, ServerLevel level) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        int frost = top.swordsman.empire.enchant.EmpireEnchantments.level(level, boots, top.swordsman.empire.enchant.EmpireEnchantments.FROST_STRIDE);
        if (frost <= 0 || !player.onGround() || player.isShiftKeyDown()) {
            return;
        }
        BlockPos below = player.blockPosition().below();
        BlockState state = level.getBlockState(below);
        boolean ice = state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.FROSTED_ICE);
        boolean soul = state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL);
        Vec3 motion = player.getDeltaMovement();
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        double boost;
        if (ice) {
            boost = 1.30 + 0.22 * frost;            // 冰面: 类似冰船
        } else if (soul) {
            boost = 1.25 + 0.06 * frost;            // 灵魂沙: 减弱减速
        } else {
            boost = 1.04 + 0.02 * frost;            // 其他方块: 类似冰面的滑行
        }
        player.setDeltaMovement(motion.x * boost, motion.y, motion.z * boost);
        player.hurtMarked = true;

        // 冰面高速滑行有概率摔倒
        if (ice && horizontal > 0.22 && level.random.nextFloat() < 0.004f * frost) {
            EmpireEffects.apply(player, EmpiresOfYoreMod.FALLEN, 1, (30 + level.random.nextInt(31)) * 20, 1);
            player.hurt(level.damageSources().flyIntoWall(), 2.0f + frost);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("effect.empire.fallen.slip"), true);
        }
    }

    /** 摔倒状态: 无法跳跃、行动缓慢(只能在冰面上爬行)。 */
    private static void handleFallen(Player player) {
        if (EmpireEffects.level(player, EmpiresOfYoreMod.FALLEN) <= 0) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x * 0.25, Math.min(motion.y, 0.0), motion.z * 0.25);
        player.hurtMarked = true;
    }
}
