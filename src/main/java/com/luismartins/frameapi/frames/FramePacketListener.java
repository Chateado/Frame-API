package com.luismartins.frameapi.frames;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.luismartins.frameapi.Main;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

public class FramePacketListener extends PacketAdapter {

    public static final int ITEM_FRAME = 71;

    public FramePacketListener() {
        super(Main.getPlugin(), ListenerPriority.NORMAL, PacketType.Play.Server.SPAWN_ENTITY);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        final Player player = event.getPlayer();

        int entityID = packet.getIntegers().read(0);//a
        double x = packet.getIntegers().read(1) / 32.0D;//b
        double y = packet.getIntegers().read(2) / 32.0D;//c
        double z = packet.getIntegers().read(3) / 32.0D;//d

        /*
         * [0] int a // ID da entidade;
         * [1] int b // x;
         * [2] int c // y;
         * [3] int d // z;
         * [4] int e // motion x;
         * [5] int f // motion y;
         * [6] int g // motion z;
         * [7] int h // pitch;
         * [8] int i // yaw;
         * [9] int j // type (??);
         * [10] int k // data (??);
         */

        Location loc = new Location(player.getWorld(), x, y, z);
        int entityType = packet.getIntegers().read(9);
        int direction = packet.getIntegers().read(10);

        if (entityType != ITEM_FRAME)
            return;

        Chunk chunk = loc.getChunk();
        if (!chunk.isLoaded())
            return;

        Frame frame = Main.getPlugin().getFrameManager().getFrameWithEntityID(chunk, entityID);
        if (frame == null) {
            BlockFace facing = convertDirectionToBlockFace(direction);
            ItemFrame entity = FrameUtils.getItemFrameFromChunk(chunk, loc, facing);
            if (entity == null)
                return;
            frame = Main.getPlugin().getFrameManager().getFrame(loc, facing);
            if (frame == null)
                return;
            frame.setEntity(entity);
        }

        final Frame frameToSend = frame;
        Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> frameToSend.sendTo(player), 10L);
    }

    private BlockFace convertDirectionToBlockFace(int direction) {
        switch (direction) {
            case 0:
                return BlockFace.SOUTH;
            case 1:
                return BlockFace.WEST;
            case 2:
                return BlockFace.NORTH;
            case 3:
                return BlockFace.EAST;
        }
        return BlockFace.NORTH;
    }

}


