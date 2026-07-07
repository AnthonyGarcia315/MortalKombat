package main;

/**
 * Describes the timing and hitbox geometry for a single attack animation.
 *
 * This is what actually fixes the "hitboxes don't match the animation" and
 * "some attacks never land" bugs: before this, EVERY attack (punch, kick,
 * sweep, uppercut, crouch kick...) shared one fixed 45x30 hitbox positioned
 * at head height, and one hardcoded "active on frame 2, 3, or 4" window
 * regardless of how many frames that move actually had.
 *
 * That caused two concrete problems:
 *  1. A 3-frame move like CROUCH_KICK or JUMP_PUNCH would hit its "active"
 *     frame on the exact same tick the generic reset-to-idle logic fired
 *     (aniIndex >= currentAnimLength), so attackActive got set true and then
 *     immediately clobbered back to false before Game.update() ever saw it.
 *  2. A sweep's hitbox floated up near the character's head instead of down
 *     near their feet, because there was no per-move vertical offset.
 *
 * Now each move defines its own frame count, its own active window (indices
 * into ITS OWN animation array), and its own hitbox size + position.
 */
public class Move {

    /**
     * Whether this attack should be treated as a high hit or a low hit for
     * purposes of picking the victim's getting-hit reaction (HIT_HIGH vs
     * HIT_LOW). Standing punches/kicks/uppercuts are HIGH; crouching kicks
     * and the sweep are LOW. If the victim happens to be crouching when hit,
     * HIT_CROUCH is used instead of either, regardless of this value.
     */
    public enum HitLevel { HIGH, LOW }

    /** How many frames this move's animation actually has. */
    public final int frameCount;

    /** Inclusive range of animation frames during which the hitbox is live. */
    public final int activeFrameStart;
    public final int activeFrameEnd;

    /**
     * Extra horizontal reach beyond the edge of the hurtbox, in whichever
     * direction the character is currently facing.
     */
    public final int hitboxOffsetX;

    /**
     * Vertical placement, measured as distance UP from the character's feet
     * (their x,y anchor point). Small values (sweep, crouch kick) sit near
     * the ground; large values (uppercut) sit near head height.
     */
    public final int hitboxOffsetY;

    public final int hitboxWidth;
    public final int hitboxHeight;

    public final HitLevel hitLevel;
    public final int damage;

    public Move(int frameCount, int activeFrameStart, int activeFrameEnd,
                int hitboxOffsetX, int hitboxOffsetY, int hitboxWidth, int hitboxHeight,
                HitLevel hitLevel, int damage) {
        this.frameCount = frameCount;
        this.activeFrameStart = activeFrameStart;
        this.activeFrameEnd = activeFrameEnd;
        this.hitboxOffsetX = hitboxOffsetX;
        this.hitboxOffsetY = hitboxOffsetY;
        this.hitboxWidth = hitboxWidth;
        this.hitboxHeight = hitboxHeight;
        this.hitLevel = hitLevel;
        this.damage = damage; // <-- Assign it here
    }

    public boolean isActiveOn(int aniIndex) {
        return aniIndex >= activeFrameStart && aniIndex <= activeFrameEnd;
    }
}