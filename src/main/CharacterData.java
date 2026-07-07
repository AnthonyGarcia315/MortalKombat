package main;

import java.util.Map;
import java.util.List;

/**
 * Everything about ONE character that used to be hardcoded identically for
 * every fighter in Fighter.java: how many frames each animation has, the
 * damage/hitbox/timing for each move, and which special-move input sequences
 * this character actually knows.
 *
 * specialFolders maps an extra (non-base) animation state name, e.g.
 * "ICE_BALL", to the sprite folder/prefix segment it lives under, e.g.
 * "iceBall" -> loads "/subzero/iceBall/subzero_iceBall_0.gif" etc. The base
 * 19 states (IDLE, WALK, ATTACK_PUNCH...) already have their folder names
 * hardcoded in Fighter.loadAnimations() and don't need an entry here.
 *
 * holdLastFrameStates lets a character-specific move (e.g. a projectile with
 * a long recovery) freeze on its last frame like ICE_BALL does, without
 * editing Fighter's shared list for every character.
 */
public class CharacterData {
    public final String characterName;
    public final Map<String, Integer> frameCounts;
    public final Map<String, Move> moveSet;
    public final List<SpecialMove> specialMoves;
    public final Map<String, String> specialFolders;
    public final List<String> holdLastFrameStates;

    public CharacterData(String characterName,
                         Map<String, Integer> frameCounts,
                         Map<String, Move> moveSet,
                         List<SpecialMove> specialMoves,
                         Map<String, String> specialFolders,
                         List<String> holdLastFrameStates) {
        this.characterName = characterName;
        this.frameCounts = frameCounts;
        this.moveSet = moveSet;
        this.specialMoves = specialMoves;
        this.specialFolders = specialFolders;
        this.holdLastFrameStates = holdLastFrameStates;
    }
}