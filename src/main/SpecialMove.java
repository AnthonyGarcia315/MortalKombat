package main;

import java.util.Arrays;
import java.util.List;

/**
 * A special move's input sequence + the name of the move it triggers.
 *
 * Sequences are written using the ABSTRACT directions FORWARD/BACK (not
 * LEFT/RIGHT), so the same SpecialMove works regardless of which side of the
 * screen the character is standing on — Player resolves FORWARD/BACK into
 * LEFT/RIGHT based on facingRight at the moment it checks the buffer.
 *
 * moveName must match:
 *   1. A key in that character's moveSet (damage/hitbox/timing), and
 *   2. A key in that character's animations map (loaded via CharacterData's
 *      frameCounts + specialFolders)
 * so the existing Fighter/Player code (which is already fully name-driven)
 * picks it up with zero further changes.
 */
public class SpecialMove {
    public final List<InputEvent.Button> sequence;
    public final String moveName;

    public SpecialMove(String moveName, InputEvent.Button... sequence) {
        this.moveName = moveName;
        this.sequence = Arrays.asList(sequence);
    }
}