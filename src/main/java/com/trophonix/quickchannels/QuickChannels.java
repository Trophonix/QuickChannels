package com.trophonix.quickchannels;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Lucas on 4/11/17.
 */
public class QuickChannels extends JavaPlugin implements Listener {

    private String prefix;
    private String format;

    private String[] help;
    private String joined;
    private String leave;
    private String left;
    private String error;
    private String reloaded;

    private Map<UUID, String> channels = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        reloadMessages();

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("quickchannels.admin")) {
                reloadConfig();
                reloadMessages();
                sender.sendMessage(reloaded);
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You must be a player to do that.");
                return true;
            }
            Player player = (Player)sender;
            if (args[0].equalsIgnoreCase("leave")) {
                String channel = getChannel(player);
                if (channel == null) {
                    player.sendMessage(error);
                    return true;
                }
                setChannel(player, null);
                player.sendMessage(leave.replace("{channel}", channel));
                sendToChannel(channel, left.replace("{channel}", channel).replace("{player}", player.getName()));
                return true;
            }
            String current = getChannel(player);
            if (current != null) Bukkit.dispatchCommand(player, "channel leave " + current);
            String channel = args[0].toLowerCase();
            setChannel(player, channel);
            return true;
        }
        for (String helpLine : this.help) {
            if (sender.hasPermission("quickchannels.admin") || !helpLine.startsWith("{admin}")) {
                sender.sendMessage(helpLine.replace("{admin}", "").replace("/channel", "/" + label));
            }
        }
        return true;
    }

    private String getChannel(Player player) {
        return channels.get(player.getUniqueId());
    }

    private void setChannel(Player player, String channel) {
        if (channel == null) {
            channels.remove(player.getUniqueId());
        } else {
            channels.put(player.getUniqueId(), channel);
            sendToChannel(channel, joined.replace("{channel}", channel).replace("{player}", player.getName()));
        }
    }

    private void sendToChannel(String channel, String message) {
        channels.forEach((uuid, ch) -> {
            if (ch.equals(channel)) Bukkit.getPlayer(uuid).sendMessage(message);
        });
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        String msg = event.getMessage();
        if (msg.startsWith(prefix)) {
            event.setCancelled(true);
            msg = msg.substring(prefix.length());
            String channel = getChannel(event.getPlayer());
            if (channel != null) {
                sendToChannel(channel, ChatColor.translateAlternateColorCodes('&', format)
                        .replace("{channel}", channel)
                        .replace("{player}", event.getPlayer().getName())
                        .replace("{message}", msg));
            } else {
                event.getPlayer().sendMessage(error);
            }
        }
    }

    private void reloadMessages() {
        prefix = getConfig().getString("prefix", "!");
        format = getConfig().getString("message-format", "&9[{channel}] {player}: &b{message}");
        List<String> help = getConfig().getStringList("messages.help");
        this.help = new String[help.size()];
        for (int i = 0; i < help.size(); i++) {
            this.help[i] = ChatColor.translateAlternateColorCodes('&', help.get(i).replace("{prefix}", prefix));
        }
        this.joined = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.joined", "&f{player} &ajoined the channel!"));
        this.leave = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.leave", "&aSuccessfully left &f{channel}"));
        this.left = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.left", "&7{player} &cleft the channel."));
        this.error = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.error", "&cYou're not in a channel!"));
        this.reloaded = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.reloaded", "&aQuickChannels config reloaded!"));
    }

}
