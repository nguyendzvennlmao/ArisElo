package me.aris.ariselo;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EloCommand implements CommandExecutor {
    private final ArisElo plugin;

    public EloCommand(ArisElo plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("ariselo.admin")) {
            sender.sendMessage(plugin.translate(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }

        if (args.length < 2) return false;

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) return true;

        switch (args[0].toLowerCase()) {
            case "set":
                if (args.length < 3) return false;
                plugin.setElo(target.getUniqueId(), Integer.parseInt(args[2]));
                break;
            case "giveelo":
                if (args.length < 3) return false;
                int amount = Integer.parseInt(args[2]);
                plugin.setElo(target.getUniqueId(), plugin.getElo(target.getUniqueId()) + amount);
                target.sendMessage(plugin.translate(plugin.getConfig().getString("messages.give-elo").replace("%elo%", String.valueOf(amount))));
                break;
            case "reset":
                plugin.setElo(target.getUniqueId(), 0);
                target.sendMessage(plugin.translate(plugin.getConfig().getString("messages.reset-elo")));
                break;
        }
        return true;
    }
}
