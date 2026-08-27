package top.swordsman.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import top.swordsman.SwordsmanMod;
import top.swordsman.machine.MachineMenu;

@EventBusSubscriber(modid = SwordsmanMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(SwordsmanMod.MACHINE_MENU, MachineScreen::new);
    }
}
