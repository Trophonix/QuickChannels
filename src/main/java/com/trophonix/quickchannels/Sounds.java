package com.trophonix.quickchannels;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Lucas on 1/20/17.
 */
public class Sounds {

    private static int version;

    private static final Map<String, Sound> sounds = new HashMap<>();

    static {
        String[] split = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].split("_");
        version = Integer.parseInt(split[0].replace("v", "") + split[1]);
        Sound levelUp, click, lavaPop, expOrb, villagerYes, villagerNo, notePling, noteBass, noteSnareDrum, anvilLand;
        if (version < 19) {
            levelUp = Sound.valueOf("LEVEL_UP");
            click = Sound.valueOf("CLICK");
            lavaPop = Sound.valueOf("LAVA_POP");
            expOrb = Sound.valueOf("ORB_PICKUP");
            villagerYes = Sound.valueOf("VILLAGER_YES");
            villagerNo = Sound.valueOf("VILLAGER_NO");
            notePling = Sound.valueOf("NOTE_PLING");
            noteBass = Sound.valueOf("NOTE_BASS");
            noteSnareDrum = Sound.valueOf("NOTE_SNARE_DRUM");
            anvilLand = Sound.valueOf("ANVIL_LAND");
        } else {
            levelUp = Sound.valueOf("ENTITY_PLAYER_LEVELUP");
            click = Sound.valueOf("UI_BUTTON_CLICK");
            lavaPop = Sound.valueOf("BLOCK_LAVA_POP");
            expOrb = Sound.valueOf("ENTITY_EXPERIENCE_ORB_PICKUP");
            villagerYes = Sound.valueOf("ENTITY_VILLAGER_YES");
            villagerNo = Sound.valueOf("ENTITY_VILLAGER_NO");
            anvilLand = Sound.valueOf("BLOCK_ANVIL_LAND");
            if (version < 113) {
                notePling = Sound.valueOf("BLOCK_NOTE_PLING");
                noteBass = Sound.valueOf("BLOCK_NOTE_BASS");
                noteSnareDrum = Sound.valueOf("BLOCK_NOTE_SNARE");
            } else {
                notePling = Sound.valueOf("BLOCK_NOTE_BLOCK_PLING");
                noteBass = Sound.valueOf("BLOCK_NOTE_BLOCK_BASS");
                noteSnareDrum = Sound.valueOf("BLOCK_NOTE_BLOCK_SNARE");
            }
        }
        put("level_up", levelUp);
        put("click", click);
        put("lava_pop", lavaPop);
        put("exp_orb", expOrb);
        put("villager_yes", villagerYes);
        put("villager_no", villagerNo);
        put("note_pling", notePling);
        put("note_bass", noteBass);
        put("note_snare_drum", noteSnareDrum);
        put("anvil_land", anvilLand);
    }

    public static void play(@NotNull Player player, String sound) {
        if (sound == null || sound.isEmpty()) return;
        if (sounds.containsKey(sound)) {
            player.playSound(player.getEyeLocation(), sounds.get(sound), 1, 1);
        } else {
            try {
                player.playSound(player.getEyeLocation(), Sound.valueOf(sound), 1, 1);
            } catch (Exception ex) {
                QuickChannels.logger().warning("Sound does not exist: " + sound);
            }
        }
    }

    private static void put(String s, Sound s1) {
        sounds.put(s, s1);
    }

}
