package net.id.incubus_core.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Server to Client Abstract packet
 */
public abstract class S2CPacket implements Packet {

    private List<ServerPlayerEntity> recipients; // List of recipients to send the packet to

    public S2CPacket() {} // Default constructor, used when registering the packet

    public S2CPacket(List<ServerPlayerEntity> recipients) {
        this.recipients = recipients;
    }

    @Override
    public void send() {
        var id = getId();
        var packetByteBuf = PacketByteBufs.create();
        this.write(packetByteBuf);

        for(var player : recipients)
            ServerPlayNetworking.send(player, id, packetByteBuf);
    }

    public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        this.read(buf);
        client.execute(() -> execute(client, handler, buf, responseSender)); // Switch from networking thread to client thread
    }

    /**
     * Executed after a packet is received on the client
     */
    abstract void execute(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender);

    /**
     * Registers a Server to Client Packet
     *
     * @param id Packet identifier
     * @param packetObj A reference to the packet class you want to register
     */
    public static <T extends S2CPacket> void register(Identifier id, T packetObj) { ClientPlayNetworking.registerGlobalReceiver(id, packetObj::receive); }
}
