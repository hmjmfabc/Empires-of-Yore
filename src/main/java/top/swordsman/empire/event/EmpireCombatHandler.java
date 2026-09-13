package top.swordsman.empire.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import top.swordsman.empire.EmpiresOfYoreMod;
import top.swordsman.empire.effect.EmpireEffects;
import top.swordsman.empire.enchant.EmpireEnchantments;

/**
 * ⑭ 状态效果(战斗部分) + ⑯d 闪电 + ⑬d 重刃 + ⑭e/f 中立化与药水互换。
 */
@EventBusSubscriber(modid = EmpiresOfYoreMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class EmpireCombatHandler {

    private static final int LIGHTNING_MAX_LEVEL = 255;

    private EmpireCombatHandler() {
    }

    /* ================= 伤害计算 ================= */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        Entity sourceEntity = event.getSource().getEntity();
        float amount = event.getAmount();

        if (sourceEntity instanceof LivingEntity attacker) {
            // ⑭c 神格: 造成伤害 ×3x
            int divinity = EmpireEffects.level(attacker, EmpiresOfYoreMod.DIVINITY);
            if (divinity > 0) {
                amount *= 3.0f * divinity;
            }
            // ⑬d 重刃: 力量效果在该装备上发挥 5 倍(原版已算 1 倍, 这里补 4 倍)
            if (attacker instanceof Player player) {
                ItemStack weapon = player.getMainHandItem();
                if (EmpireEnchantments.level(player.level(), weapon, EmpireEnchantments.HEAVY_BLADE) > 0) {
                    MobEffectInstance strength = player.getEffect(MobEffects.DAMAGE_BOOST);
                    if (strength != null) {
                        amount += 3.0f * (strength.getAmplifier() + 1) * 4.0f;
                    }
                }
            }
            // ⑭a 暴怒: 造成伤害 ×(x+1)
            int attackerAnger = EmpireEffects.level(attacker, EmpiresOfYoreMod.ANGER);
            if (attackerAnger > 0) {
                amount *= (attackerAnger + 1);
            }
        }
        // ⑭a 暴怒: 受到伤害 ×(x+1)
        int victimAnger = EmpireEffects.level(victim, EmpiresOfYoreMod.ANGER);
        if (victimAnger > 0) {
            amount *= (victimAnger + 1);
        }
        // ⑭b 平静: 受到伤害 ×0.8^x
        int calm = EmpireEffects.level(victim, EmpiresOfYoreMod.CALM);
        if (calm > 0) {
            amount *= (float) Math.pow(0.8, calm);
        }
        // ⑭d 无实体: 每次最多 1 点伤害
        if (EmpireEffects.level(victim, EmpiresOfYoreMod.INCORPOREAL) > 0) {
            amount = Math.min(amount, 1.0f);
        }
        event.setAmount(amount);
    }

    /* ================= 伤害后: 闪电传递/连锁 + 末影形态传送 ================= */
    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        Entity attackerEntity = event.getSource().getEntity();
        float damage = event.getNewDamage();

        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }

        // ⑯d 闪电: 攻击他人可传递(等级+1, 时长不变)
        if (attackerEntity instanceof Player attacker && victim instanceof Player target) {
            MobEffectInstance bolt = attacker.getEffect(EmpiresOfYoreMod.LIGHTNING);
            if (bolt != null) {
                int newLevel = Math.min(LIGHTNING_MAX_LEVEL, bolt.getAmplifier() + 1 + 1);
                attacker.removeEffect(EmpiresOfYoreMod.LIGHTNING);
                target.addEffect(new MobEffectInstance(EmpiresOfYoreMod.LIGHTNING, bolt.getDuration(), newLevel - 1, false, true, true));
                target.displayClientMessage(net.minecraft.network.chat.Component.translatable("effect.empire.lightning.transferred", newLevel), true);
            }
        }

        // ⑯d 闪电: 拥有者被攻击 -> 其他所有拥有者受到同类型数值减半(取整)的伤害
        if (EmpireEffects.level(victim, EmpiresOfYoreMod.LIGHTNING) > 0) {
            int splash = (int) Math.floor(damage / 2.0f);
            if (splash > 0) {
                for (Player other : level.players()) {
                    if (other != victim && EmpireEffects.level(other, EmpiresOfYoreMod.LIGHTNING) > 0) {
                        other.hurt(level.damageSources().indirectMagic(victim, attackerEntity), splash);
                    }
                }
            }
        }

        // ⑭f 末影形态: 受伤后随机传送至 16 格内安全位置
        if (victim instanceof Player player && EmpireEffects.level(player, EmpiresOfYoreMod.ENDER_FORM) > 0) {
            teleportRandomly(level, player);
        }
    }

    private static void teleportRandomly(ServerLevel level, Player player) {
        for (int attempt = 0; attempt < 16; attempt++) {
            double dx = (level.random.nextDouble() - 0.5) * 32.0;
            double dy = (level.random.nextDouble() - 0.5) * 16.0;
            double dz = (level.random.nextDouble() - 0.5) * 32.0;
            BlockPos pos = BlockPos.containing(player.getX() + dx, player.getY() + dy, player.getZ() + dz);
            if (pos.getY() < level.getMinBuildHeight() + 1 || pos.getY() > level.getMaxBuildHeight() - 2) {
                continue;
            }
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                    && !level.getBlockState(pos.below()).isAir()) {
                player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                return;
            }
        }
    }

    /* ================= ⑯d 闪电: 效果结束被雷击 ================= */
    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance().getEffect().value() != EmpiresOfYoreMod.LIGHTNING.get()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        int n = event.getEffectInstance().getAmplifier() + 1;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(entity.getX(), entity.getY(), entity.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        entity.hurt(level.damageSources().lightningBolt(), 10.0f * n);
    }

    /* ================= 中立化: 亡灵形态 / 末影形态 ================= */
    @SubscribeEvent
    public static void onTargetChange(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (!(target instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob) || !(mob instanceof Enemy) || isBoss(mob)) {
            return;
        }
        boolean undead = EmpireEffects.level(player, EmpiresOfYoreMod.UNDEAD_FORM) > 0;
        boolean ender = EmpireEffects.level(player, EmpiresOfYoreMod.ENDER_FORM) > 0;
        if (undead || ender || (ender && mob instanceof EnderMan)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    private static boolean isBoss(Mob mob) {
        return mob instanceof WitherBoss || mob instanceof EnderDragon
                || mob instanceof net.minecraft.world.entity.monster.warden.Warden;
    }

    /* ================= ⑯a 人工制品 / ⑭e 药水互换 ================= */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance incoming = event.getEffectInstance();

        // ⑯a 人工制品: 免疫外界施加的任何状态效果, 每免疫一次等级 -1
        int artifact = EmpireEffects.level(entity, EmpiresOfYoreMod.ARTIFACT);
        if (artifact > 0 && incoming.getEffect().value() != EmpiresOfYoreMod.ARTIFACT.get()) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            MobEffectInstance current = entity.getEffect(EmpiresOfYoreMod.ARTIFACT);
            int duration = current == null ? 600 : current.getDuration();
            entity.removeEffect(EmpiresOfYoreMod.ARTIFACT);
            if (artifact - 1 > 0) {
                entity.addEffect(new MobEffectInstance(EmpiresOfYoreMod.ARTIFACT, duration, artifact - 2, false, true, true));
            }
            return;
        }

        // ⑭e 亡灵形态: 伤害药水与治疗药水效果互换
        if (EmpireEffects.level(entity, EmpiresOfYoreMod.UNDEAD_FORM) > 0) {
            int amplifier = incoming.getAmplifier();
            if (incoming.getEffect().value() == MobEffects.HARM.value()) {
                event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
                entity.heal(4 << amplifier);
            } else if (incoming.getEffect().value() == MobEffects.HEAL.value()) {
                event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
                entity.hurt(entity.damageSources().magic(), 6 << amplifier);
            }
        }
    }

    /* ================= ⑭f 末影形态: 使用末影珍珠必出末影螨 ================= */
    @SubscribeEvent
    public static void onUseEnderPearl(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !event.getItemStack().is(net.minecraft.world.item.Items.ENDER_PEARL)) {
            return;
        }
        if (EmpireEffects.level(player, EmpiresOfYoreMod.ENDER_FORM) <= 0) {
            return;
        }
        if (player.level() instanceof ServerLevel level) {
            for (int i = 0; i < 2; i++) {
                net.minecraft.world.entity.monster.Endermite mite = EntityType.ENDERMITE.create(level);
                if (mite != null) {
                    Vec3 pos = player.position();
                    mite.moveTo(pos.x + (level.random.nextDouble() - 0.5) * 4.0, pos.y,
                            pos.z + (level.random.nextDouble() - 0.5) * 4.0, level.random.nextFloat() * 360f, 0f);
                    level.addFreshEntity(mite);
                }
            }
        }
    }

    /* ================= ⑭f 末影形态: 恐水 ================= */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (level.getGameTime() % 20 != 0) {
                continue;
            }
            for (Player player : level.players()) {
                if (EmpireEffects.playerHas(player, EmpiresOfYoreMod.ENDER_FORM) && player.isInWaterOrRain()) {
                    player.hurt(level.damageSources().drown(), 1.0f);
                }
            }
        }
    }
}
