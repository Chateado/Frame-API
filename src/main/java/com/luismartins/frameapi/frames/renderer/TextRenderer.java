package com.luismartins.frameapi.frames.renderer;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.*;
import java.awt.image.BufferedImage;

public class TextRenderer extends MapRenderer {

    private final String text;

    private Integer mapId = null;

    private boolean rendered = false;

    public TextRenderer(String text) {
        this.text = text;
    }

    public TextRenderer(String text, Integer mapId) {
        this.text = text;
        this.mapId = mapId;
    }

    public String getText() {
        return this.text;
    }

    public Integer getMapId() {
        return this.mapId;
    }

    public boolean isRendered() {
        return this.rendered;
    }

    public void render(MapView view, MapCanvas canvas, Player player) {
        if (this.rendered)
            return;
        this.rendered = true;
        BufferedImage image = new BufferedImage(128, 128, 2);
        Graphics g = image.getGraphics();
        g.drawString(this.text, 5, 12);
        if (this.mapId != null)
            g.drawString("Map #" + this.mapId.toString(), 70, 115);
        canvas.drawImage(0, 0, image);
    }

    //Add render in other image types
}