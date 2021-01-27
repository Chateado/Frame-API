package com.luismartins.frameapi;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.google.gson.Gson;
import com.luismartins.frameapi.frames.FrameManager;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

@Getter
public class Main extends JavaPlugin {

    @Getter
    private static Main plugin;

    private FrameManager frameManager;

    private CommandMap commandMap;

    private final Gson gson = new Gson();

    @Override
    public void onLoad() {
        plugin = this;
        super.onLoad();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        saveDefaultConfig();
        saveConfig();
        this.commandMap = getCommandMap();
        frameManager = new FrameManager(getDataFolder());
        frameManager.enable();
    }

    public void registerCommand(Command command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        getCommandMap().register(getName().toLowerCase(), command);
    }

    public void registerListener(Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        getServer().getPluginManager().registerEvents(listener, this);
    }

    private CommandMap getCommandMap() {
        if (this.commandMap == null) {
            try {
                Class<?> craftServerClazz = MinecraftReflection.getCraftBukkitClass("CraftServer");
                Method getCommandMap = craftServerClazz.getDeclaredMethod("getCommandMap");
                this.commandMap = (CommandMap) getCommandMap.invoke(getServer());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return this.commandMap;
    }
}
