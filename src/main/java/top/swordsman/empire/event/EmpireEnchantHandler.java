package top.swordsman.empire.event;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import top.swordsman.empire.EmpiresOfYoreMod;
import top.swordsman.empire.enchant.EmpireEnchantments;

/**
 * ⑬ 附魔效果:
 * a.饱和修补(进食满饱食度时修复装备) b.振奋(削减挖掘疲劳惩罚) c.梁文锋的祝福(每日两个时段随机增益)
 */
@EventBusSubscriber(modid = EmpiresOfYoreMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class EmpireEnchantHandler {

    private EmpireEnchantHandler() {
    }

    /* ================= a. 饱和修补 ================= */
    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        FoodProperties food = event.getItem().get(DataComponents.FOOD);
        if (food == null) {
            return;
        }
        if (player.getFoodData().getFoodLevel() < 20) {
            return;
        }
        // 满饱食度: 进食不回复饱食与饱和度, 改为修复附有此附魔的装备
        int points = food.nutrition() + (int) food.saturation();
        if (points <= 0) {
            return;
        }
        for (ItemStack stack : player.getInventory().items) {
            repair(stack, level, points);
        }
        for (ItemStack stack : player.getInventory().armor) {
            repair(stack, level, points);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            repair(stack, level, points);
        }
    }

    private static void repair(ItemStack stack, ServerLevel level, int points) {
        if (stack.isEmpty() || !stack.isDamageableItem() || stack.getDamageValue() <= 0) {
            return;
        }
        int enchantLevel = EmpireEnchantments.level(level, stack, EmpireEnchantments.SATIATED_MENDING);
        if (enchantLevel <= 0) {
            return;
        }
        int repair = 5 * enchantLevel * points;
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - repair));
    }

    /* ================= b. 振奋: 抵消 15x% 挖掘疲劳惩罚 ================= */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        MobEffectInstance fatigue = player.getEffect(MobEffects.DIG_SLOWDOWN);
        if (fatigue == null) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        int vigor = EmpireEnchantments.level(player.level(), tool, EmpireEnchantments.VIGOR);
        if (vigor <= 0) {
            return;
        }
        // 原版惩罚: 速度 ×0.2^(等级+1) -> 惩罚比例 = 1 - 该系数
        double factor = Math.pow(0.2, fatigue.getAmplifier() + 1);
        double penalty = 1.0 - factor;
        double remaining = penalty * (1.0 - 0.15 * vigor);   // 振奋抵消 15x%
        float original = event.getOriginalSpeed();
        if (penalty <= 0.0 || factor <= 0.0) {
            return;
        }
        // original 已含惩罚: original = raw * factor -> 还原后按新的惩罚系数折算
        float raw = (float) (original / factor);
        event.setNewSpeed((float) (raw * (1.0 - remaining)));
    }

    /* ================= c. 梁文锋的祝福: 每日 9:00~12:00 与 14:00~18:00 ================= */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || player.tickCount % 100 != 0) {
            return;
        }
        long time = level.getDayTime() % 24000L;
        boolean window = (time >= 3000 && time < 6000) || (time >= 8000 && time < 12000);
        if (!window) {
            return;
        }
        int best = 0;
        for (ItemStack stack : player.getInventory().armor) {
            best = Math.max(best, EmpireEnchantments.level(level, stack, EmpireEnchantments.LIANG_WENFENG_BLESSING));
        }
        best = Math.max(best, EmpireEnchantments.level(level, player.getMainHandItem(), EmpireEnchantments.LIANG_WENFENG_BLESSING));
        best = Math.max(best, EmpireEnchantments.level(level, player.getOffhandItem(), EmpireEnchantments.LIANG_WENFENG_BLESSING));
        if (best <= 0) {
            return;
        }
        net.minecraft.core.Holder<MobEffect>[] pool = new net.minecraft.core.Holder[]{
                MobEffects.MOVEMENT_SPEED, MobEffects.DIG_SPEED, MobEffects.DAMAGE_BOOST, MobEffects.JUMP,
                MobEffects.REGENERATION, MobEffects.DAMAGE_RESISTANCE, MobEffects.FIRE_RESISTANCE,
                MobEffects.WATER_BREATHING, MobEffects.NIGHT_VISION, MobEffects.ABSORPTION, MobEffects.LUCK
        };
        player.addEffect(new MobEffectInstance(pool[level.random.nextInt(pool.length)],
                240, best - 1, false, true, true));
    }
}
