package top.swordsman.empire.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.registries.DeferredHolder;

/** 状态效果等级查询与等级上限钳制。 */
public final class EmpireEffects {

    private EmpireEffects() {
    }

    /** 返回效果等级(1 起, 无则 0)。 */
    public static int level(LivingEntity entity, DeferredHolder<MobEffect, MobEffect> effect) {
        MobEffectInstance instance = entity.getEffect(effect);
        return instance == null ? 0 : instance.getAmplifier() + 1;
    }

    public static boolean playerHas(net.minecraft.world.entity.player.Player player, DeferredHolder<MobEffect, MobEffect> effect) {
        return level(player, effect) > 0;
    }

    public static MobEffectInstance instance(DeferredHolder<MobEffect, MobEffect> effect, int level, int duration) {
        return new MobEffectInstance(effect, duration, Math.max(0, level - 1), false, true, true);
    }

    /** 按等级上限施加/升级效果(同等级或更低则刷新时长)。 */
    public static void apply(LivingEntity entity, DeferredHolder<MobEffect, MobEffect> effect, int level, int duration, int maxLevel) {
        int clamped = Math.min(Math.max(1, level), maxLevel);
        MobEffectInstance current = entity.getEffect(effect);
        if (current == null || current.getAmplifier() + 1 < clamped) {
            entity.addEffect(instance(effect, clamped, duration));
        } else {
            entity.addEffect(new MobEffectInstance(effect, duration, current.getAmplifier(), false, true, true));
        }
    }
}
