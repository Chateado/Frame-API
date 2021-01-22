package com.luismartins.frameapi.frames;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.luismartins.frameapi.Main;
import com.luismartins.frameapi.command.CommandFrame;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EntityTracker;
import net.minecraft.server.v1_8_R3.EntityTrackerEntry;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class FrameManager {

    private final File framesFile;

    private final Map<String, List<Frame>> frames = new HashMap<>();

    @Getter
    private final Map<UUID, BufferedImage> creatingCache = new HashMap<>();

    @Getter
    private final FrameImageStore pictureDatabase;

    public FrameManager(File folder) {
        this.framesFile = new File(folder, "frames.json");

        File frameImages = new File(folder, "frame_images");

        if (!frameImages.exists() && !frameImages.mkdir()) {
            throw new RuntimeException("Não foi possível criar a pasta frame_images!");
        }

        this.pictureDatabase = new FrameImageStore(frameImages);
    }

    public void enable() {
        this.loadFrames();
        this.cacheFrames();

        // Registra o comando
        Main.getPlugin().registerCommand(new CommandFrame());

        // Packet Listener
        ProtocolLibrary.getProtocolManager().addPacketListener(new FramePacketListener());

        // Packet Sender
        Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), new FramePacketSender(), 1, 1);
    }

    public void cacheFrames() {
        if (frames.isEmpty()) return;

        System.out.println("Caching frames ...");

        long memory = FrameUtils.getUsedMemory();
        int amount = 0;

        for (List<Frame> frameList : this.frames.values()) {
            for (Frame frame : frameList) {
                frame.sendTo(null);
                amount++;
            }
        }

        System.out.println("Cached " + amount + " frames!");

        long usedMemory = FrameUtils.getUsedMemory() - memory;
        if (usedMemory > 0L) {
            System.out.println("The frame cache use " + usedMemory + "mb memory!");
        }
    }


    public void removeFrame(Frame frame) {
        if (frame == null)
            return;
        Chunk chunk = frame.getLocation().getChunk();
        List<Frame> frameList = getFramesInChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        frameList.remove(frame);
        setFramesInChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ(), frameList);

        saveFrames();
    }

    public List<Frame> getFramesInChunk(Chunk chunk) {
        return getFramesInChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public void setFramesInChunk(Chunk chunk, List<Frame> frames) {
        setFramesInChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ(), frames);
    }

    public List<Frame> getFramesInChunk(String world, int chunkX, int chunkZ) {
        return this.frames.getOrDefault(String.format("%s|%d|%d", world, chunkX, chunkZ), new ArrayList<>());
    }

    public void setFramesInChunk(String world, int chunkX, int chunkZ, List<Frame> frames) {
        this.frames.put(String.format("%s|%d|%d", world, chunkX, chunkZ), frames);
    }

    public Frame getFrame(Location loc, BlockFace face) {
        Chunk chunk = loc.getChunk();
        List<Frame> frameList = getFramesInChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        for (Frame frame : frameList) {
            if (FrameUtils.isSameLocation(frame.getLocation(), loc) && (face == null || frame.getFacing() == face))
                return frame;
        }
        return null;
    }


    public Frame getFrameWithEntityID(Chunk chunk, int entityId) {
        List<Frame> frameList = getFramesInChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        for (Frame frame : frameList) {
            if (frame.isLoaded() && frame.getEntity().getEntityId() == entityId)
                return frame;
        }
        return null;
    }

    public void sendFrame(Frame frame) {
        if (!frame.isLoaded())
            return;

        ItemFrame entity = frame.getEntity();
        WorldServer worldServer = ((CraftWorld) entity.getWorld()).getHandle();
        EntityTracker tracker = worldServer.tracker;
        EntityTrackerEntry trackerEntry = tracker.trackedEntities.d(entity.getEntityId());

        if (trackerEntry == null)
            return;

        for (Object playerNMS : trackerEntry.trackedPlayers) {
            CraftPlayer craftPlayer = ((EntityPlayer) playerNMS).getBukkitEntity();
            frame.sendTo(craftPlayer);
        }
    }

    public int getNewFrameID() {
        int highestId = -1;
        for (List<Frame> frameList : this.frames.values()) {
            for (Frame frame : frameList)
                highestId = Math.max(highestId, frame.getId());
        }
        return highestId + 1;
    }

    public Frame addFrame(String pictureURL, ItemFrame entity) {
        Frame frame = new Frame(getNewFrameID(), pictureURL, entity.getLocation(), entity.getFacing());
        frame.setEntity(entity);

        if (frame.getBufferImage() == null)
            return null;

        Chunk chunk = entity.getLocation().getChunk();
        List<Frame> frameList = getFramesInChunk(chunk);
        frameList.add(frame);
        setFramesInChunk(chunk, frameList);

        FrameUtils.setFrameItemWithoutSending(entity, new ItemStack(Material.AIR));

        sendFrame(frame);
        saveFrames();
        return frame;
    }

    public List<Frame> getFramesWithImage(String image) {
        List<Frame> frameList = new ArrayList<>();
        for (List<Frame> frames : this.frames.values()) {
            for (Frame frame : frames) {
                if (frame.getPicture().equals(image))
                    frameList.add(frame);
            }
        }
        return frameList;
    }

    public List<Frame> getFrames() {
        List<Frame> frameList = new ArrayList<>();
        for (List<Frame> frames : this.frames.values())
            frameList.addAll(frames);
        return frameList;
    }

    public Frame getFrame(ItemFrame entity) {
        for (List<Frame> frameList : this.frames.values()) {
            for (Frame frame : frameList) {
                if (frame.isLoaded() && frame.getLocation().getWorld() == entity.getWorld() && frame.getEntity().getEntityId() == entity.getEntityId())
                    return frame;
            }
        }
        return null;
    }

    public void addMultiFrames(BufferedImage img, ItemFrame[] frames, int vertical, int horizontal) {
        /*if (frames.length == 0 || horizontal <= 0)
            return null;*/

        img = FrameUtils.scaleImage(img, img.getWidth() * vertical, img.getHeight() * horizontal);

        int width = img.getWidth() / vertical;
        int height = img.getHeight() / horizontal;

        //List<Frame> frameList = new ArrayList<>();
        int globalId = getNewFrameID();
        int id = globalId;

        for (int y = 0; y < horizontal; y++) {
            for (int x = 0; x < vertical; x++) {
                try {
                    BufferedImage frameImg = FrameUtils.cutImage(img, x * width, y * height, width, height);
                    frameImg = FrameUtils.scaleImage(frameImg, 128, 128, false);

                    File file = this.pictureDatabase.savePicture(String.format("Frame%s_%s-%s", globalId, x, y), frameImg);

                    ItemFrame entity = frames[vertical * y + x];
                    FrameUtils.setFrameItemWithoutSending(entity, new ItemStack(Material.AIR));

                    Frame frame = getFrame(entity);
                    if (frame != null) {
                        removeFrame(frame);
                    }

                    frame = new Frame(globalId, file.getName(), entity.getLocation(), entity.getFacing());
                    frame.setEntity(entity);

                    Chunk chunk = frame.getLocation().getChunk();
                    List<Frame> chunkFrames = getFramesInChunk(chunk);
                    chunkFrames.add(frame);
                    setFramesInChunk(chunk, chunkFrames);

                    globalId++;
                    //frameList.add(frame);

                    sendFrame(frame);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        saveFrames();
    }

    public void loadFrames() {
        this.frames.clear();

        if (framesFile.exists() && framesFile.isFile()) {
            try (FileReader reader = new FileReader(framesFile)) {
                JsonArray frames = new JsonParser().parse(reader).getAsJsonArray();

                for (int i = 0; i < frames.size(); i++) {
                    JsonObject frameJson = frames.get(i).getAsJsonObject();

                    int id = frameJson.get("ID").getAsInt();
                    int x = frameJson.get("X").getAsInt();
                    int y = frameJson.get("Y").getAsInt();
                    int z = frameJson.get("Z").getAsInt();
                    String worldName = frameJson.get("World").getAsString();

                    World world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        //Retorna informando que nao há nenhum mundo.
                        continue;
                    }

                    Location loc = new Location(world, x, y, z);

                    BlockFace face = null;
                    if (frameJson.has("Facing"))
                        face = BlockFace.valueOf(frameJson.get("Facing").getAsString());

                    String picture = frameJson.get("Picture").getAsString();
                    Frame frame = new Frame(id, picture, loc, face);
                    Chunk chunk = loc.getChunk();

                    if (chunk.isLoaded()) {
                        ItemFrame entity = FrameUtils.getItemFrameFromChunk(chunk, loc, face);
                        if (entity != null) {
                            FrameUtils.setFrameItemWithoutSending(entity, new ItemStack(Material.AIR));
                            frame.setEntity(entity);
                            sendFrame(frame);
                        }
                    }

                    List<Frame> frameList = getFramesInChunk(chunk);
                    frameList.add(frame);
                    setFramesInChunk(chunk, frameList);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Main.getPlugin().getLogger().log(Level.INFO, "Carregado {0} frames!", getFrames().size());
    }

    public void saveFrames() {
        JsonArray frames = new JsonArray();

        for (List<Frame> frameList : this.frames.values()) {
            for (Frame frame : frameList) {
                JsonObject frameJson = new JsonObject();

                frameJson.addProperty("ID", frame.getId());
                frameJson.addProperty("Picture", frame.getPicture());
                frameJson.addProperty("World", frame.getLocation().getWorld().getName());

                frameJson.addProperty("X", frame.getLocation().getBlockX());
                frameJson.addProperty("Y", frame.getLocation().getBlockY());
                frameJson.addProperty("Z", frame.getLocation().getBlockZ());

                if (frame.getFacing() != null)
                    frameJson.addProperty("Facing", frame.getFacing().name());

                frames.add(frameJson);
            }
        }

        try (FileWriter fw = new FileWriter(framesFile)) {
            fw.write(new Gson().toJson(frames));
        } catch (IOException e) {
            Main.getPlugin().getLogger().log(Level.WARNING, "Erro ao salvar os frames", e);
        }
    }
}

