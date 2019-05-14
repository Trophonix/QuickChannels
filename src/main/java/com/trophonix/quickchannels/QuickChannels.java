package com.trophonix.quickchannels;

import org.apache.commons.validator.routines.UrlValidator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by Lucas on 4/11/17.
 */
public class QuickChannels extends JavaPlugin implements Listener {

    private static Logger logger;

    private String prefix;
    private String format;
    private boolean linksRequirePermission;

    private String[] help;
    private String joined;
    private String leave;
    private String left;
    private String error;
    private String reloaded;
    private String noLinks;

    private Map<UUID, String> channels = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        reloadMessages();

        getServer().getPluginManager().registerEvents(this, this);

        logger = getLogger();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
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
                String sound = getConfig().getString("sounds.leave", "villager_no");
                Sounds.play(player, sound);
                playSoundToChannel(channel, sound);
                return true;
            }
            String current = getChannel(player);
            if (current != null) Bukkit.dispatchCommand(player, "channel leave " + current);
            String channel = args[0].toLowerCase();
            setChannel(player, channel);
            String sound = getConfig().getString("sounds.join", "pling");
            playSoundToChannel(channel, sound);
            return true;
        }
        for (String helpLine : this.help) {
            if (sender.hasPermission("quickchannels.admin") || !helpLine.startsWith("{admin}")) {
                sender.sendMessage(helpLine.replace("{admin}", "").replace("/channel", "/" + label));
            }
        }
        return true;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String msg = event.getMessage();
        if (msg.startsWith(prefix)) {
            event.setCancelled(true);
            msg = msg.substring(prefix.length());
            String channel = getChannel(player);
            if (channel != null) {
                if (linksRequirePermission && !player.hasPermission("quickchannels.links")) {
                    List<String> tests = new ArrayList<>(Arrays.asList(msg.split(" ")));
                    tests.addAll(Arrays.asList(msg.replace(",", ".").split(" ")));
                    for (String s : tests) {
                        if (UrlValidator.getInstance().isValid(s)) {
                            player.sendMessage(noLinks);
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
                msg = ChatColor.translateAlternateColorCodes('&', format)
                        .replace("{channel}", channel)
                        .replace("{player}", player.getName())
                        .replace("{message}", msg);
                for (ChatColor c : ChatColor.values()) {
                    if (player.hasPermission("quickchannels.styles." + c.getChar()) || player.hasPermission("quickchannels.styles." + c.name().toLowerCase())) {
                        msg = msg.replace("&" + c.getChar(), ChatColor.COLOR_CHAR + "" + c.getChar());
                    }
                }
                sendToChannel(channel, msg);
                String sound = getConfig().getString("sounds.message", "click");
                playSoundToChannel(channel, sound);
            } else {
                player.sendMessage(error);
            }
        }
    }

    private String getChannel(@NotNull Player player) {
        return channels.get(player.getUniqueId());
    }

    private void setChannel(@NotNull Player player, String channel) {
        if (channel == null) {
            channels.remove(player.getUniqueId());
        } else {
            channels.put(player.getUniqueId(), channel);
            sendToChannel(channel, joined.replace("{channel}", channel).replace("{player}", player.getName()));
        }
    }

    private void sendToChannel(@NotNull String channel, String message) {
        channels.forEach((uuid, ch) -> {
            if (ch.equals(channel)) Bukkit.getPlayer(uuid).sendMessage(message);
        });
    }

    private void playSoundToChannel(@NotNull String channel, String sound) {
        channels.forEach((uuid, ch) -> {
            if (ch.equals(channel)) Sounds.play(Bukkit.getPlayer(uuid), sound);
        });
    }

    private void reloadMessages() {
        prefix = getConfig().getString("prefix", "!");
        format = getConfig().getString("message-format", "&9[{channel}] {player}: &b{message}");
        linksRequirePermission = getConfig().getBoolean("links-require-permission", false);
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
        this.noLinks = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.no-links", "&cYou don't have permission to send links in channels!"));
    }

    static Logger logger() { return logger; }

}
