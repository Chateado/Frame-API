package com.luismartins.frameapi.frames;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.map.CraftMapCanvas;
import org.bukkit.craftbukkit.v1_8_R3.map.CraftMapView;

import java.lang.reflect.Field;

public class FakeMapCanvas extends CraftMapCanvas {

    public FakeMapCanvas() {
        super(null);
    }

    public byte[] getBuffer() {
        return super.getBuffer();
    }

    public void setBase(byte[] base) {
        super.setBase(base);
    }

    public CraftMapView getMapView() {
        return (Bukkit.getMap((short) 0) == null) ? (CraftMapView) Bukkit.createMap(Bukkit.getWorlds().get(0)) : (CraftMapView) Bukkit.getMap((short) 0);
    }

    public void setPixel(int x, int y, byte color) {
        Field field;
        byte[] buffer;
        try {
            field = CraftMapCanvas.class.getDeclaredField("buffer");
            field.setAccessible(true);
            buffer = (byte[]) field.get(this);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if (x < 0 || y < 0 || x >= 128 || y >= 128)
            return;
        if (buffer[y * 128 + x] != color)
            buffer[y * 128 + x] = color;
        try {
            field.set(this, buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
