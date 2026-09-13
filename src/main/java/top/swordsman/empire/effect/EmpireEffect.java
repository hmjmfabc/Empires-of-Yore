package top.swordsman.empire.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** 通用自定义状态效果(原版 MobEffect 构造为 protected, 必须子类化)。 */
public class EmpireEffect extends MobEffect {

    public EmpireEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
