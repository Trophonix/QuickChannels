package com.trophonix.quickchannels;

import org.apache.commons.validator.routines.UrlValidator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by Lucas on 4/11/17.
 */
public class QuickChannels extends JavaPlugin implements Listener {

    private static Logger logger;

    private String prefix;
    private boolean prefixSendsToChannel;
    private ConfigMessage format;
    private boolean linksRequirePermission;
    private boolean consoleOutput;
    private boolean removeOnQuit;

    private ConfigMessage help;
    private ConfigMessage joined;
    private ConfigMessage join;
    private ConfigMessage leave;
    private ConfigMessage left;
    private ConfigMessage error;
    private ConfigMessage reloaded;
    private ConfigMessage noLinks;
    private ConfigMessage forcedIn;
    private ConfigMessage forcedOut;
    private ConfigMessage playerNotFound;
    private ConfigMessage forcedSuccess;
    private ConfigMessage forcedNoChannel;

    private Map<UUID, String> channels = new HashMap<>();

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();

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
                reloaded.send(this, sender);
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You must be a player to do that.");
                return true;
            }
            Player player = (Player) sender;
            if (args[0].equalsIgnoreCase("leave")) {
                String channel = getChannel(player);
                if (channel == null) {
                    error.send(this, sender);
                    return true;
                }
                setChannel(player, null, false);
                return true;
            } else if (args.length >= 2 && args[0].equalsIgnoreCase("force") && sender.hasPermission("quickchannels.admin")) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    playerNotFound.send(this, sender, "{player}", args[1]);
                    return true;
                }
                if (args.length == 2) {
                    if (getChannel(target) == null) {
                        forcedNoChannel.send(this, sender, "{player}", target.getName());
                        return true;
                    }
                    setChannel(target, null, true);
                } else {
                    String channel = args[2].toLowerCase();
                    setChannel(target, channel, true);
                }
                forcedSuccess.send(this, sender, "{player}", target.getName());
                return true;
            }
            String current = getChannel(player);
            if (current != null) Bukkit.dispatchCommand(player, "channel leave " + current);
            String channel = args[0].toLowerCase();
            setChannel(player, channel, false);
            return true;
        }
        help.send(this, sender);
        return true;
    }

    @EventHandler (priority = EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String msg = event.getMessage();
        if (msg.trim().equals(prefix)) return;
        if (prefixSendsToChannel == msg.startsWith(prefix)) {
            if (prefixSendsToChannel) msg = msg.substring(prefix.length());
            String channel = getChannel(player);
            if (channel != null) {
                event.setCancelled(true);
                if (linksRequirePermission && !player.hasPermission("quickchannels.links")) {
                    List<String> tests = new ArrayList<>(Arrays.asList(msg.split(" ")));
                    tests.addAll(Arrays.asList(msg.replace(",", ".").split(" ")));
                    for (String s : tests) {
                        if (UrlValidator.getInstance().isValid(s)) {
                            noLinks.send(this, player);
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
                for (ChatColor c : ChatColor.values()) {
                    if (player.hasPermission("quickchannels.styles." + c.getChar()) || player.hasPermission("quickchannels.styles." + c.name().toLowerCase())) {
                        msg = msg.replace("&" + c.getChar(), ChatColor.COLOR_CHAR + "" + c.getChar());
                    }
                }
                sendToChannel(channel, player, msg);
                String sound = getConfig().getString("sounds.message", "click");
                playSoundToChannel(channel, sound);
            } else if (prefixSendsToChannel) {
                error.send(this, player);
                event.setCancelled(true);
            }
        } else if (!prefixSendsToChannel && msg.startsWith(prefix)) {
            event.setMessage(msg.substring(prefix.length()));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (removeOnQuit) setChannel(event.getPlayer(), null, false);
    }

    private String getChannel(@NotNull Player player) {
        return channels.get(player.getUniqueId());
    }

    private void setChannel(@NotNull Player player, String channel, boolean forced) {
        if (channel == null) {
            String previousChannel = getChannel(player);
            channels.remove(player.getUniqueId());
            if (forced) forcedOut.send(this, player, "{channel}", previousChannel, "{player}", player.getName());
            else leave.send(this, player, "{channel}", previousChannel);
            left.send(this, getChannelMembers(previousChannel), "{channel}", previousChannel, "{player}", player.getName(), "{members}", getChannelMembersString(previousChannel));
            String sound = getConfig().getString("sounds.leave", "villager_no");
            playSoundToChannel(previousChannel, sound);
        } else {
            channels.put(player.getUniqueId(), channel);
            String memberString = getChannelMembersString(channel);
            if (forced) forcedIn.send(this, player, "{channel}", channel, "{player}", player.getName(), "{members}", memberString);
            else join.send(this, player, "{channel}", channel, "{player}", player.getName(), "{members}", memberString);
            joined.send(this, getChannelMembers(channel, player), "{channel}", channel, "{player}", player.getName(), "{members}", memberString);
            String sound = getConfig().getString("sounds.join", "pling");
            playSoundToChannel(channel, sound);
        }
    }

    private void sendToChannel(@NotNull String channel, CommandSender sender, String message) {
        channels.forEach((uuid, ch) -> {
            if (ch.equals(channel)) {
                format.send(this, Bukkit.getPlayer(uuid), "{channel}", ch,
                    "{player}", sender.getName(), "{prefix}", this.prefix, "{message}", message);
            }
        });
        if (consoleOutput) {
            format.send(this, Bukkit.getConsoleSender(), "{channel}", channel,
                "{player}", sender.getName(), "{prefix}", this.prefix, "{message}", message);
        }
    }

    private void playSoundToChannel(@NotNull String channel, String sound) {
        channels.forEach((uuid, ch) -> {
            if (ch.equals(channel)) Sounds.play(Bukkit.getPlayer(uuid), sound);
        });
    }

    private Collection<Player> getChannelMembers(String channel, Player... exclude) {
        List<Player> excludes = Arrays.asList(exclude);
        return channels.entrySet().stream().filter(e -> e.getValue().equals(channel))
            .map(e -> Bukkit.getPlayer(e.getKey()))
                   .filter(p -> !excludes.contains(p)).collect(Collectors.toList());
    }

    private String getChannelMembersString(String channel) {
        return getChannelMembers(channel).stream().map(Player::getName)
                   .collect(Collectors.joining(", "));
    }

    private void reloadMessages() {
        this.prefix = getConfig().getString("prefix", "!");
        this.prefixSendsToChannel = getConfig().getBoolean("prefix-sends-to-channel", true);
        this.format = new ConfigMessage(getConfig(), "message-format");
        this.linksRequirePermission = getConfig().getBoolean("links-require-permission", false);
        this.consoleOutput = getConfig().getBoolean("console-output", true);
        this.removeOnQuit = getConfig().getBoolean("remove-on-quit", true);

        this.help = new ConfigMessage(getConfig(), "messages.help");
        this.join = new ConfigMessage(getConfig(), "messages.join");
        this.joined = new ConfigMessage(getConfig(), "messages.joined");
        this.leave = new ConfigMessage(getConfig(), "messages.leave");
        this.left = new ConfigMessage(getConfig(), "messages.left");
        this.error = new ConfigMessage(getConfig(), "messages.error");
        this.reloaded = new ConfigMessage(getConfig(), "messages.reloaded");
        this.noLinks = new ConfigMessage(getConfig(), "messages.no-links");
        this.forcedIn = new ConfigMessage(getConfig(), "messages.forced-in");
        this.forcedOut = new ConfigMessage(getConfig(), "messages.forced-out");
        this.playerNotFound = new ConfigMessage(getConfig(), "messages.player-not-found");
        this.forcedSuccess = new ConfigMessage(getConfig(), "messages.forced-success");
        this.forcedNoChannel = new ConfigMessage(getConfig(), "messages.forced-no-channel");
    }

    public String getPrefix() {
        return this.prefix;
    }

    static Logger logger() { return logger; }

}
