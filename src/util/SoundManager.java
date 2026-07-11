package util;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads and plays short sound effects from the classpath (same /res folder
 * your sprites already live under). Mirrors LoadSave's "missing asset never
 * crashes the game" approach: a missing .wav prints one warning the first
 * time it's requested and is silently skipped every time after that --
 * nothing throws, nothing halts the game loop.
 *
 * DROP YOUR FILES HERE (in /res, same place as your sprite folders):
 *   /sounds/punch.wav
 *   /sounds/kick.wav
 *   /sounds/uppercut.wav
 *   /sounds/sweep.wav
 *   /sounds/throw.wav
 *   /sounds/special.wav      (catch-all whoosh for ICE_BALL/SPEAR/SLIDE/etc.)
 *   /sounds/hit.wav
 *   /sounds/block.wav
 *   /sounds/freeze.wav
 *   /sounds/jump.wav
 *   /sounds/ko.wav
 *   /sounds/menu_move.wav
 *   /sounds/menu_confirm.wav
 *
 * Any file you haven't added yet is simply silent -- you don't need all 13
 * up front. .wav is used because javax.sound.sampled reads it with zero
 * extra dependencies; if you only have .mp3s, convert them first.
 *
 * Usage: SoundManager.play(SoundManager.Sound.PUNCH);
 */
public class SoundManager {

    public enum Sound {
        PUNCH, KICK, UPPERCUT, SWEEP, THROW, SPECIAL,
        HIT, BLOCK, FREEZE, JUMP, KO,
        MENU_MOVE, MENU_CONFIRM
    }

    private static final Map<Sound, String> FILE_PATHS = new HashMap<>();
    static {
        FILE_PATHS.put(Sound.PUNCH, "/sounds/punch.wav");
        FILE_PATHS.put(Sound.KICK, "/sounds/kick.wav");
        FILE_PATHS.put(Sound.UPPERCUT, "/sounds/uppercut.wav");
        FILE_PATHS.put(Sound.SWEEP, "/sounds/sweep.wav");
        FILE_PATHS.put(Sound.THROW, "/sounds/throw.wav");
        FILE_PATHS.put(Sound.SPECIAL, "/sounds/special.wav");
        FILE_PATHS.put(Sound.HIT, "/sounds/hit.wav");
        FILE_PATHS.put(Sound.BLOCK, "/sounds/block.wav");
        FILE_PATHS.put(Sound.FREEZE, "/sounds/freeze.wav");
        FILE_PATHS.put(Sound.JUMP, "/sounds/jump.wav");
        FILE_PATHS.put(Sound.KO, "/sounds/ko.wav");
        FILE_PATHS.put(Sound.MENU_MOVE, "/sounds/menu_move.wav");
        FILE_PATHS.put(Sound.MENU_CONFIRM, "/sounds/menu_confirm.wav");
    }

    /** Cached raw PCM bytes + format per sound, so a Clip can be created fresh every play(). */
    private static final class ClipData {
        final AudioFormat format;
        final byte[] bytes;
        ClipData(AudioFormat format, byte[] bytes) {
            this.format = format;
            this.bytes = bytes;
        }
    }

    // null entries mean "we already tried and it's missing" -- keeps us from
    // re-printing the warning or re-hitting disk every single time a punch lands.
    private static final Map<Sound, ClipData> CACHE = new HashMap<>();
    private static boolean muted = false;

    private static ClipData load(Sound sound) {
        if (CACHE.containsKey(sound)) {
            return CACHE.get(sound);
        }

        String path = FILE_PATHS.get(sound);
        ClipData result = null;
        try {
            URL url = SoundManager.class.getResource(path);
            if (url == null) {
                System.out.println("\u26A0\uFE0F No sound file at " + path + " -- skipping (game keeps running silently for this cue).");
            } else {
                try (AudioInputStream ais = AudioSystem.getAudioInputStream(url)) {
                    result = new ClipData(ais.getFormat(), ais.readAllBytes());
                }
            }
        } catch (Exception e) {
            System.out.println("\u26A0\uFE0F Could not load sound " + path + " (" + e.getMessage() + ") -- skipping.");
        }

        CACHE.put(sound, result);
        return result;
    }

    /**
     * Plays a sound effect. Safe to call every frame/every hit -- opens a
     * brand-new short-lived Clip each time (instead of reusing one Clip)
     * specifically so overlapping cues (two quick jabs, a hit landing while
     * a whoosh is still playing) don't cut each other off.
     */
    public static void play(Sound sound) {
        if (muted) return;

        ClipData data = load(sound);
        if (data == null) return; // missing asset -- already warned once in load()

        try {
            Clip clip = AudioSystem.getClip();
            clip.open(data.format, data.bytes, 0, data.bytes.length);
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close(); // free the native audio line once playback ends
                }
            });
            clip.start();
        } catch (LineUnavailableException e) {
            System.out.println("\u26A0\uFE0F Could not play sound " + sound + ": " + e.getMessage());
        }
    }

    public static void setMuted(boolean m) { muted = m; }
    public static boolean isMuted() { return muted; }
}