package com.luismartins.frameapi.frames;

import com.luismartins.frameapi.util.HttpRequest;
import lombok.RequiredArgsConstructor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@RequiredArgsConstructor
public class FrameImageStore {

    private final File folder;

    public BufferedImage download(String url) throws IOException {
        HttpRequest request = HttpRequest.get(url)
                .connectTimeout(10000)
                .readTimeout(10000)
                .userAgent("Frame-API/1.0.0");

        String contentType = request.header("Content-Type");

        if (contentType != null && (contentType.equals("image/png") || contentType.equals("image/jpeg"))) {
            return ImageIO.read(request.stream());
        }

        throw new IllegalStateException("Nenhuma imagem foi encontrada nesse link!");
    }

    public BufferedImage getPicture(String name) throws IOException {
        File file = new File(folder, name);

        if (file.exists() && file.isFile()) {
            return ImageIO.read(file);
        } else {
            System.out.println("NOT FOUND: " + name);
        }

        throw new IllegalStateException("Arquivo " + name + " não encontrado!");
    }

    public File savePicture(String name, BufferedImage image) throws IOException {
        File file = new File(folder, name);
        ImageIO.write(image, "png", file);
        return file;
    }

    public void deletePicture(String name) {

    }
}
