package com.luismartins.frameapi.frames;

import lombok.Data;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketListenerPlayOut;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FramePacketSender implements Runnable {

    private static final Queue<QueuedPacket> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void run() {
        int loads = 0;
        while (!queue.isEmpty() && loads++ <= 5) {
            QueuedPacket packet = queue.poll();
            Player player = packet.getPlayer();

            if (player != null && player.isOnline()) {
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet.packet);
            }
        }
    }

    public static void removePlayerFromQueue(Player player) {
        synchronized (queue) {
            queue.removeIf(queuedPacket -> queuedPacket.player == player);
        }
    }

    public static void addPacketToQueue(Player player, Packet<PacketListenerPlayOut> packet) {
        queue.add(new QueuedPacket(player, packet));
    }

    @Data
    private static class QueuedPacket {

        public final Player player;
        public final Packet<PacketListenerPlayOut> packet;

    }

}
