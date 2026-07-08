package main;

import java.util.*;

/**
 * One CharacterData entry per roster member. This is the ONLY place you
 * should need to touch when adding a character, changing their frame
 * counts, or giving them a new special move — Fighter/Player/Enemy all read
 * from this instead of having anything hardcoded.
 *
 * HOW TO ADD/EDIT A CHARACTER:
 *   1. Copy one of the build___() methods below.
 *   2. Update the frame counts to match however many sprites are actually in
 *      that character's folders (LoadSave will otherwise just fall back to
 *      IDLE and print a warning for any mismatch, so nothing crashes — it'll
 *      just look wrong until the numbers match).
 *   3. Add any special moves: give each one a unique moveName, add its
 *      frame count + folder name, add Move data (damage/hitbox/timing), and
 *      add a SpecialMove with its input sequence.
 *   4. Register it in the static block at the top.
 */
public class CharacterRegistry {

    private static final Map<String, CharacterData> DATA = new HashMap<>();

    static {
        register(buildSubzero());
        register(buildScorpion());
        register(buildJohnnyCage());
        register(buildKano());
        register(buildRaiden());
        register(buildLiuKang());
        register(buildSonya());
    }

    private static void register(CharacterData data) {
        DATA.put(data.characterName, data);
    }

    public static CharacterData getData(String characterName) {
        CharacterData data = DATA.get(characterName);
        if (data == null) {
            System.out.println("\u26A0\uFE0F No CharacterData registered for '" + characterName
                    + "' -- falling back to Sub-Zero's data. Add a build" + characterName
                    + "() entry in CharacterRegistry.");
            data = DATA.get("subzero");
        }
        return data;
    }

    // ------------------------------------------------------------------
    // Shared starting point every character gets by default. Every
    // character has ATTACK_PUNCH/ATTACK_KICK/UPPERCUT/etc. so the Enemy AI
    // and Player's normal-move logic never has to guess whether a move
    // exists -- only SPECIAL moves vary character-to-character.
    // ------------------------------------------------------------------

    private static Map<String, Integer> baseFrameCounts() {
        Map<String, Integer> f = new HashMap<>();
        f.put("IDLE", 9);
        f.put("CROUCH_BLOCK", 2);
        f.put("WALK", 9);
        f.put("CROUCH", 3);
        f.put("JUMP", 3);
        f.put("JUMP_FLIP", 8);
        f.put("BLOCK", 3);
        f.put("ATTACK_PUNCH", 9);
        f.put("ATTACK_KICK", 8);
        f.put("UPPERCUT", 5);
        f.put("JUMP_PUNCH", 3);
        f.put("JUMP_KICK", 3);
        f.put("CROUCH_KICK", 3);
        f.put("SWEEP", 6);
        f.put("HIT_HIGH", 3);
        f.put("HIT_LOW", 3);
        f.put("HIT_CROUCH", 3);
        f.put("THROW", 6);
        f.put("THROWN", 5);
        return f;
    }

    private static Map<String, Move> baseMoveSet() {
        Map<String, Move> m = new HashMap<>();
        m.put("ATTACK_PUNCH", new Move(9, 3, 5, 0, 100, 45, 25, Move.HitLevel.HIGH, 5));
        m.put("ATTACK_KICK", new Move(7, 2, 4, 5, 60, 55, 25, Move.HitLevel.HIGH, 8));
        m.put("UPPERCUT", new Move(5, 1, 3, -5, 150, 35, 90, Move.HitLevel.HIGH, 12));
        m.put("JUMP_PUNCH", new Move(3, 0, 2, 0, 90, 45, 25, Move.HitLevel.HIGH, 6));
        m.put("JUMP_KICK", new Move(5, 1, 3, 10, 40, 60, 30, Move.HitLevel.HIGH, 9));
        m.put("CROUCH_KICK", new Move(3, 0, 2, 0, 20, 40, 20, Move.HitLevel.LOW, 4));
        m.put("SWEEP", new Move(6, 2, 4, 15, 10, 70, 20, Move.HitLevel.LOW, 3));
        m.put("THROW", new Move(6, 1, 3, -10, 120, 40, 60, Move.HitLevel.HIGH, 0));
        return m;
    }

    // ------------------------------------------------------------------
    // Individual characters
    // ------------------------------------------------------------------

    private static CharacterData buildSubzero() {
        Map<String, Integer> frames = baseFrameCounts();
        frames.put("ICE_BALL", 3);
        frames.put("SLIDE", 2);

        Map<String, Move> moves = baseMoveSet();
        // ICE_BALL's damage is 0 here on purpose -- it freezes instead of
        // damaging directly, handled in Fighter.getFrozen()/Game's collision.
        moves.put("ICE_BALL", new Move(6, 3, 5, 0, 105, 40, 50, Move.HitLevel.HIGH, 0));
        moves.put("SLIDE", new Move(3, 1, 3, 0, 110, 50, 20, Move.HitLevel.LOW, 8));

        List<SpecialMove> specials = Arrays.asList(
                new SpecialMove("ICE_BALL", InputEvent.Button.DOWN, InputEvent.Button.FORWARD, InputEvent.Button.PUNCH),
                new SpecialMove("SLIDE", InputEvent.Button.BACK, InputEvent.Button.FORWARD, InputEvent.Button.KICK)
        );

        Map<String, String> folders = new HashMap<>();
        folders.put("ICE_BALL", "iceBall");
        folders.put("SLIDE", "slide");

        List<String> holdLast = Arrays.asList("ICE_BALL");

        return new CharacterData("subzero", frames, moves, specials, folders, holdLast);
    }

    private static CharacterData buildScorpion() {
        Map<String, Integer> frames = baseFrameCounts();
        frames.put("SPEAR", 5);           // "Get over here!"
        frames.put("TELEPORT_PUNCH", 4);

        Map<String, Move> moves = baseMoveSet();
        // Long horizontal reach, short vertical band -- adjust once you see
        // it land against real hurtboxes.
        moves.put("SPEAR", new Move(5, 1, 4, 0, 100, 220, 30, Move.HitLevel.HIGH, 6));
        moves.put("TELEPORT_PUNCH", new Move(4, 1, 3, 0, 100, 45, 30, Move.HitLevel.HIGH, 10));

        List<SpecialMove> specials = Arrays.asList(
                new SpecialMove("SPEAR", InputEvent.Button.BACK, InputEvent.Button.FORWARD, InputEvent.Button.PUNCH),
                new SpecialMove("TELEPORT_PUNCH", InputEvent.Button.DOWN, InputEvent.Button.FORWARD, InputEvent.Button.KICK)
        );

        Map<String, String> folders = new HashMap<>();
        folders.put("SPEAR", "spear");
        folders.put("TELEPORT_PUNCH", "teleportPunch");

        List<String> holdLast = Arrays.asList("SPEAR");

        return new CharacterData("scorpion", frames, moves, specials, folders, holdLast);
    }

    // The rest of the roster starts with just the shared base moves and NO
    // specials yet. Give them a real move (copy the Sub-Zero/Scorpion
    // pattern above) once you've got sprites + an idea for their special --
    // nothing else breaks in the meantime, they'll just throw normal
    // punches/kicks/throws like before.
    private static CharacterData buildJohnnyCage() {
        return new CharacterData("johnnycage", baseFrameCounts(), baseMoveSet(),
                new ArrayList<>(), new HashMap<>(), new ArrayList<>());
    }

    private static CharacterData buildKano() {
        return new CharacterData("kano", baseFrameCounts(), baseMoveSet(),
                new ArrayList<>(), new HashMap<>(), new ArrayList<>());
    }

    private static CharacterData buildRaiden() {
        return new CharacterData("raiden", baseFrameCounts(), baseMoveSet(),
                new ArrayList<>(), new HashMap<>(), new ArrayList<>());
    }

    private static CharacterData buildLiuKang() {
        return new CharacterData("liukang", baseFrameCounts(), baseMoveSet(),
                new ArrayList<>(), new HashMap<>(), new ArrayList<>());
    }

    private static CharacterData buildSonya() {
        return new CharacterData("sonya", baseFrameCounts(), baseMoveSet(),
                new ArrayList<>(), new HashMap<>(), new ArrayList<>());
    }
}