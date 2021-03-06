package com.luismartins.frameapi.frames;

import io.netty.channel.Channel;
import net.minecraft.server.v1_8_R3.EntityItemFrame;
import net.minecraft.server.v1_8_R3.NetworkManager;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketListenerPlayOut;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftItemFrame;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

public class FrameUtils {

    public static void setFrameItemWithoutSending(ItemFrame entity, ItemStack item) {
        EntityItemFrame nmsEntity = ((CraftItemFrame) entity).getHandle();
        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);

        if (nmsStack != null) {
            nmsStack.count = 1;
            nmsStack.a(nmsEntity);
        }

        nmsEntity.getDataWatcher().watch(8, nmsStack);
    }

    public static ItemFrame getItemFrameFromChunk(Chunk chunk, Location loc, BlockFace face) {
        for (Entity entity : chunk.getEntities()) {
            if (entity.getType() == EntityType.ITEM_FRAME && isSameLocation(entity.getLocation(), loc)) {
                ItemFrame frameEntity = (ItemFrame) entity;
                if (face == null || frameEntity.getFacing() == face)
                    return frameEntity;
            }
        }
        return null;
    }

    public static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
    }

    public static byte[] setCanvasPixel(byte[] buffer, int x, int y, byte color) {
        if (x < 0 || y < 0 || x >= 128 || y >= 128)
            return buffer;
        buffer[y * 128 + x] = color;
        return buffer;
    }

    public static void sendPacketFast(Player player, Packet<PacketListenerPlayOut> packet) {
        try {
            NetworkManager netty = (((CraftPlayer) player).getHandle()).playerConnection.networkManager;
            Field field = NetworkManager.class.getDeclaredField("channel");
            field.setAccessible(true);
            Channel channel = (Channel) field.get(netty);
            channel.writeAndFlush(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static BufferedImage scaleImage(BufferedImage image, int width, int height) {
        return scaleImage(image, width, height, true);
    }

    public static BufferedImage scaleImage(BufferedImage image, int width, int height, boolean checks) {
        if (checks && Config.SIZE_CENTER && image.getWidth() < width && image.getHeight() < height)
            return image;
        if (image.getWidth() == width && image.getHeight() == height)
            return image;
        float ratio = image.getHeight() / image.getWidth();
        int newWidth = width;
        int newHeight = height;
        if (checks) {
            newHeight = (int) (newWidth * ratio);
            if (newHeight > height) {
                newHeight = height;
                newWidth = (int) (newHeight / ratio);
            }
        }

        BufferedImage resized = new BufferedImage(newWidth, newHeight, image.getType());

        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, newWidth, newHeight, 0, 0, image.getWidth(), image.getHeight(), null);
        g.dispose();

        return resized;
    }

    public static BufferedImage cutImage(BufferedImage image, int posX, int posY, int width, int height) {
        return image.getSubimage(posX, posY, width, height);
    }

    public static boolean isSameLocation(Location loc1, Location loc2) {
        return (loc1.getWorld() == loc2.getWorld() && loc1.getBlockX() == loc2.getBlockX() && loc1.getBlockY() == loc2.getBlockY() && loc1.getBlockZ() == loc2.getBlockZ());
    }

    public static int diff(int v1, int v2) {
        return Math.abs(v1 - v2);
    }

    public static void setPrivateField(Object instance, String fieldName, Object value) {
        try {
            Class<?> c = instance.getClass();
            Field field = c.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(instance, value);
        } catch (Exception ex) {
            ex.printStackTrace();

            //FramePicturePlugin.getPlugin().getLogger().log(Level.WARNING, "Can't set private field", ex);
        }
    }

}
