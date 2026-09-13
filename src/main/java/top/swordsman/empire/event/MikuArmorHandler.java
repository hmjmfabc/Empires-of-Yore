package top.swordsman.empire.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import top.swordsman.empire.EmpiresOfYoreMod;
import top.swordsman.empire.item.MikuItem;

/**
 * 初音护甲效果修正：穿戴时无敌，脱下后立即恢复。
 * 旧实现只在穿戴时把 invulnerable 设为 true，脱下后永远不会清除（永久无敌）。
 * 同时跳过创造/旁观模式，避免覆盖原版能力标志。
 */
@EventBusSubscriber(modid = EmpiresOfYoreMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class MikuArmorHandler {

    private MikuArmorHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        boolean shouldBeInvulnerable = wearsMikuArmor(player);
        if (player.getAbilities().invulnerable != shouldBeInvulnerable) {
            player.getAbilities().invulnerable = shouldBeInvulnerable;
            player.onUpdateAbilities();
        }
    }

    private static boolean wearsMikuArmor(Player player) {
        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.getItem() instanceof MikuItem) {
                return true;
            }
        }
        return false;
    }
}
