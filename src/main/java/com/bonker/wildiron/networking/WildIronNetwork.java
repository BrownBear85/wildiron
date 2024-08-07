package com.bonker.wildiron.networking;

import com.bonker.wildiron.WildIron;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class WildIronNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel CHANNEL;

    public static void register() {
        CHANNEL = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(WildIron.MODID, "messages"))
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .clientAcceptedVersions(PROTOCOL_VERSION::equals)
                .serverAcceptedVersions(PROTOCOL_VERSION::equals)
                .simpleChannel();

        CHANNEL.messageBuilder(FiredGunC2SPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .decoder(FiredGunC2SPacket::new)
                .encoder(FiredGunC2SPacket::encode)
                .consumerMainThread(FiredGunC2SPacket::handle)
                .add();
    }

    public static <T> void sendToServer(T packet) {
        CHANNEL.sendToServer(packet);
    }
}
