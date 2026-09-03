package top.swordsman.empire.event;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import top.swordsman.empire.EmpiresOfYoreMod;

/**
 * empire:mining 维度行为：
 * <ul>
 *   <li>怪物在该维度生成时获得增强(生命/攻击/移速/追踪范围)。</li>
 *   <li>玩家进入/登录该维度时，若不在 512 层附近，则移动到 512 层出生。</li>
 * </ul>
 */
@EventBusSubscriber(modid = EmpiresOfYoreMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class MiningEventHandler {

    private static final double SPAWN_Y = 512.0;
    private static final ResourceLocation BOOST_ID =
            ResourceLocation.fromNamespaceAndPath("empire", "mining_boost");

    private MiningEventHandler() {
    }

    @SubscribeEvent
    public static void onMobSpawn(FinalizeSpawnEvent event) {
        if (!EmpiresOfYoreMod.isMiningDimension(event.getLevel().getLevel())) {
            return;
        }
        Mob mob = event.getEntity();
        if (!(mob instanceof Enemy)) {
            return;
        }
        boost(mob, Attributes.MAX_HEALTH, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        boost(mob, Attributes.ATTACK_DAMAGE, 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        boost(mob, Attributes.MOVEMENT_SPEED, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        boost(mob, Attributes.FOLLOW_RANGE, 12.0, AttributeModifier.Operation.ADD_VALUE);
        mob.setHealth(mob.getMaxHealth());
    }

    private static void boost(Mob mob, Holder<Attribute> attribute, double amount,
                              AttributeModifier.Operation operation) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null) {
            instance.addTransientModifier(new AttributeModifier(BOOST_ID, amount, operation));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        repositionPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        repositionPlayer(event.getEntity());
    }

    private static void repositionPlayer(Player player) {
        if (!EmpiresOfYoreMod.isMiningDimension(player.level())) {
            return;
        }
        double y = player.getY();
        if (y < 256.0 || y > 768.0) {
            player.teleportTo(player.getX(), SPAWN_Y, player.getZ());
        }
    }
}
