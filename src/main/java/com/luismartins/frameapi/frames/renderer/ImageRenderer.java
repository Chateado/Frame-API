package com.luismartins.frameapi.frames.renderer;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.*;

public class ImageRenderer extends MapRenderer {

    private final Image image;

    private boolean rendered = false;

    public int imageX = 0;

    public int imageY = 0;

    public ImageRenderer(Image image) {
        this.image = image;
    }

    public Image getImage() {
        return this.image;
    }

    public boolean isRendered() {
        return this.rendered;
    }

    public void render(MapView view, MapCanvas canvas, Player player) {
        if (this.rendered)
            return;
        this.rendered = true;
        canvas.drawImage(this.imageX, this.imageY, this.image);
    }
}
