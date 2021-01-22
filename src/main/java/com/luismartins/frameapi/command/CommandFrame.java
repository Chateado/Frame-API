package com.luismartins.frameapi.command;

import com.luismartins.frameapi.Main;
import com.luismartins.frameapi.frames.FrameManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class CommandFrame extends Command {

    public CommandFrame() {
        super("frame");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("APENAS JOGADORES");
        } else if (!sender.hasPermission("frameapi.command.frame")) {
            sender.sendMessage("§cVocê não tem permissão para executar esse comando!");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("criar")) {
            Player player = (Player) sender;
            player.sendMessage("§aBaixando...");

            Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> {
                try {
                    FrameManager frameManager = Main.getPlugin().getFrameManager();

                    BufferedImage image = frameManager.getPictureDatabase().download(args[1]);
                    frameManager.getCreatingCache().put(player.getUniqueId(), image);

                    player.sendMessage("§aA imagem informada foi baixada com sucesso.");
                    player.sendMessage("§cClique no quadro do topo para realizar a aplicação da imagem!");
                } catch (IOException e) {
                    e.printStackTrace();
                    sender.sendMessage("§cErro ao fazer o download da imagem!");
                } catch (IllegalStateException e) {
                    sender.sendMessage("§c" + e.getMessage());
                }
            });
        } else {
            sender.sendMessage("§cUse o comando: /frame criar <URL>");
        }
        return false;
    }
}
