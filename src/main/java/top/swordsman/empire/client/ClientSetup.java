package top.swordsman.empire.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import top.swordsman.empire.EmpiresOfYoreMod;
import top.swordsman.empire.machine.MachineMenu;

@EventBusSubscriber(modid = EmpiresOfYoreMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(EmpiresOfYoreMod.MACHINE_MENU, MachineScreen::new);
    }
}
