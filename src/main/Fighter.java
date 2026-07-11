package main;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import util.LoadSave;
import util.SoundManager;

/**
 * Shared base for any character in the game, whether it's keyboard-controlled
 * (Player) or not (Enemy). Everything that depends on "which character sprite
 * set is this" lives here, so a character swap never requires touching
 * control logic, and control logic never needs to know character details.
 *
 * Previously Player hardcoded its own animation loading and Enemy hardcoded
 * "subzero" idle-only with no attacks at all. Now both take a characterName
 * and get the full move set — Enemy just doesn't drive it with keyboard input
 * (yet), but the architecture no longer stops it from being any character or
 * from eventually throwing punches/kicks like Player can.
 */
public abstract class Fighter extends Entity {

    protected String characterName;
    protected HashMap<String, BufferedImage[]> animations = new HashMap<>();
    protected HashMap<String, Move> moveSet = new HashMap<>();
    protected CharacterData characterData;
    protected java.util.List<SpecialMove> specialMoves;

    protected int aniTick, aniIndex;
    protected int aniSpeed = 20;

    protected boolean attacking = false;
    protected String currentAttack = "";

    // States that hold on their last frame instead of looping back to frame 0
    // or snapping back to IDLE.
    // JUMP_FLIP was missing from this list originally, which meant a jump
    // lasting longer than one 8-frame animation cycle (160 ticks) would loop
    // the flip mid-air instead of holding the pose.
    // JUMP_KICK/JUMP_PUNCH are airborne attacks that are often shorter than
    // the time actually spent in the air, so they now freeze on their final
    // frame (still swinging) until landing — see the landing check in
    // Player.updatePosition(), which is what actually ends the attack, since
    // holding the last frame means "attacking" never resets on its own.
    private static final String[] HOLD_LAST_FRAME_STATES = {
            "CROUCH", "JUMP", "JUMP_FLIP", "BLOCK", "JUMP_KICK", "JUMP_PUNCH","CROUCH_BLOCK"
    };

    public Fighter(float x, float y, int width, int height, String characterName) {
        super(x, y, width, height);
        this.characterName = characterName;
        this.characterData = CharacterRegistry.getData(characterName);
        this.specialMoves = characterData.specialMoves;
        loadAnimations();
        defineMoveSet();
    }

    /**
     * Loads every animation this character might use. Anything missing on
     * disk falls back to IDLE (with a console warning) instead of crashing,
     * so a new character can be dropped in with a partial asset set.
     */
    protected void loadAnimations() {
        String basePath = "/" + characterName + "/";
        String prefix = characterName + "_";

        BufferedImage[] idleAnim = LoadSave.GetSpriteSequence(basePath + "idle/" + prefix + "idle_", frames("IDLE", 9));
        animations.put("IDLE", idleAnim);
        animations.put("CROUCH_BLOCK", loadOrDefault(basePath + "lowBlock/" + prefix + "lblock_", frames("CROUCH_BLOCK", 2), idleAnim));
        animations.put("WALK", loadOrDefault(basePath + "walk/" + prefix + "walk_", frames("WALK", 9), idleAnim));
        animations.put("CROUCH", loadOrDefault(basePath + "crouch/" + prefix + "crouch_", frames("CROUCH", 3), idleAnim));
        animations.put("JUMP", loadOrDefault(basePath + "jump/" + prefix + "jump_", frames("JUMP", 3), idleAnim));
        animations.put("JUMP_FLIP", loadOrDefault(basePath + "jump/" + prefix + "jumpflip_", frames("JUMP_FLIP", 8), idleAnim));
        animations.put("BLOCK", loadOrDefault(basePath + "highBlock/" + prefix + "hblock_", frames("BLOCK", 3), idleAnim));

        animations.put("ATTACK_PUNCH", loadOrDefault(basePath + "attackPunch/" + prefix + "punch_", frames("ATTACK_PUNCH", 9), idleAnim));
        animations.put("ATTACK_KICK", loadOrDefault(basePath + "kicking/" + prefix + "kick_", frames("ATTACK_KICK", 7), idleAnim));
        animations.put("UPPERCUT", loadOrDefault(basePath + "uppercut/" + prefix + "uppercut_", frames("UPPERCUT", 5), idleAnim));
        animations.put("JUMP_PUNCH", loadOrDefault(basePath + "jumpPunch/" + prefix + "jumpPunch_", frames("JUMP_PUNCH", 3), idleAnim));
        animations.put("JUMP_KICK", loadOrDefault(basePath + "jumpKick/" + prefix + "jumpkick_", frames("JUMP_KICK", 4), idleAnim));
        animations.put("CROUCH_KICK", loadOrDefault(basePath + "crouchKick/" + prefix + "crouchKick_", frames("CROUCH_KICK", 3), idleAnim));
        animations.put("SWEEP", loadOrDefault(basePath + "sweep/" + prefix + "sweep_", frames("SWEEP", 6), idleAnim));

        animations.put("HIT_HIGH", loadOrDefault(basePath + "hitHigh/" + prefix + "hitHigh_", frames("HIT_HIGH", 3), idleAnim));
        animations.put("HIT_LOW", loadOrDefault(basePath + "hitLow/" + prefix + "hitLow_", frames("HIT_LOW", 3), idleAnim));
        animations.put("HIT_CROUCH", loadOrDefault(basePath + "hitCrouch/" + prefix + "hitCrouch_", frames("HIT_CROUCH", 3), idleAnim));
        animations.put("THROW", loadOrDefault(basePath + "throw/" + prefix + "throw_", frames("THROW", 6), idleAnim));
        animations.put("THROWN", loadOrDefault(basePath + "thrown/" + prefix + "thrown_", frames("THROWN", 5), idleAnim));

        // Character-specific special moves (ICE_BALL, SPEAR, whatever else
        // this character's CharacterData defines) -- loaded generically so
        // adding a new special never requires touching Fighter.java again.
        for (java.util.Map.Entry<String, String> entry : characterData.specialFolders.entrySet()) {
            String state = entry.getKey();
            String folder = entry.getValue();
            animations.put(state, loadOrDefault(basePath + folder + "/" + prefix + folder + "_", frames(state, 3), idleAnim));
        }
    }

    /** Looks up this character's frame count for a state, falling back to the shared default. */
    private int frames(String state, int fallback) {
        return characterData.frameCounts.getOrDefault(state, fallback);
    }

    protected BufferedImage[] loadOrDefault(String path, int frames, BufferedImage[] defaultAnim) {
        BufferedImage[] anim = LoadSave.GetSpriteSequence(path, frames);
        if (anim == null || anim.length == 0 || anim[0] == null) {
            System.out.println("\u26A0\uFE0F MISSING ASSET for " + characterName + " (using IDLE fallback): " + path);
            return defaultAnim;
        }
        return anim;
    }

    /**
     * Per-move timing + hitbox geometry. Every character currently shares
     * these numbers; if a specific character needs a longer sweep or a
     * bigger uppercut later, override this method in a subclass or make it
     * read from a per-character data file.
     */
    protected void defineMoveSet() {
        moveSet.putAll(characterData.moveSet);
    }

    /**
     * Plays the right whoosh for whatever move is starting, shared by
     * Player.processBuffer() and Enemy.startAttack() so both control paths
     * stay in sync instead of hand-rolling the same switch twice. Anything
     * not explicitly listed is treated as a character-specific special
     * (ICE_BALL, SPEAR, SLIDE, TELEPORT_PUNCH, ...) and gets the generic
     * SPECIAL cue.
     */
    protected void playAttackSound(String moveName) {
        switch (moveName) {
            case "ATTACK_PUNCH":
            case "JUMP_PUNCH":
                SoundManager.play(SoundManager.Sound.PUNCH);
                break;
            case "UPPERCUT":
                SoundManager.play(SoundManager.Sound.UPPERCUT);
                break;
            case "ATTACK_KICK":
            case "JUMP_KICK":
            case "CROUCH_KICK":
                SoundManager.play(SoundManager.Sound.KICK);
                break;
            case "SWEEP":
                SoundManager.play(SoundManager.Sound.SWEEP);
                break;
            case "THROW":
                SoundManager.play(SoundManager.Sound.THROW);
                break;
            default:
                SoundManager.play(SoundManager.Sound.SPECIAL);
                break;
        }
    }

    private boolean holdsLastFrame(String state) {
        for (String s : HOLD_LAST_FRAME_STATES) {
            if (s.equals(state)) return true;
        }
        return characterData.holdLastFrameStates.contains(state);
    }

    /**
     * Advances the current animation and, if an attack is playing, flips
     * attackActive using THAT move's own active-frame window instead of one
     * hardcoded window for every attack. This is the fix for short attacks
     * (CROUCH_KICK, JUMP_PUNCH) whose hit never used to register.
     */
    protected void updateAnimationTick() {
        aniTick++;

        BufferedImage[] currentAnimArray = animations.get(currentState);
        if (currentAnimArray == null) {
            System.out.println("⚠️ WARNING: Missing animations for state -> " + currentState);
            attacking = false;
            currentState = "IDLE";
            currentAttack = "";
            aniIndex = 0;
            attackActive = false;
            return;
        }

        int currentAnimLength = currentAnimArray.length;

        // --- THE FIX: Custom Recovery Timer for Ice Ball ---
        // If we are on the final frame of the Ice Ball, let aniTick count up without resetting
        if (currentState.equals("ICE_BALL") && aniIndex == currentAnimLength - 1) {
            if (aniTick >= 120) { // 120 ticks = 1 second recovery time
                attacking = false;
                currentState = "IDLE";
                currentAttack = "";
                aniIndex = 0;
                aniTick = 0;
                attackActive = false;
            }
            return; // Skip the rest of the method so aniTick doesn't get reset to 0!
        }

        // --- NORMAL ANIMATION LOGIC ---
        if (aniTick < aniSpeed) return;

        aniTick = 0;
        aniIndex++;

        Move move = moveSet.get(currentState);
        if (attacking && move != null) {
            attackActive = move.isActiveOn(aniIndex);
        }

        if (aniIndex >= currentAnimLength) {
            if (holdsLastFrame(currentState)) {
                aniIndex = currentAnimLength - 1;
            } else if (attacking) {
                attacking = false;
                currentState = "IDLE";
                currentAttack = "";
                aniIndex = 0;
                attackActive = false;
            } else {
                aniIndex = 0;
            }
        }
    }

    /**
     * Repositions and resizes the hitbox to match whatever attack is
     * currently playing, using that move's own geometry from moveSet.
     * Call this AFTER updateCollisionBoxes() so hurtbox.x is already fresh.
     */
    protected void updateAttackHitbox() {
        Move move = moveSet.get(currentAttack);
        if (!attacking || move == null) return;

        hitbox.width = move.hitboxWidth;
        hitbox.height = move.hitboxHeight;

        int feetY = (int) y;
        hitbox.y = feetY - move.hitboxOffsetY - (move.hitboxHeight / 2);

        if (facingRight) {
            hitbox.x = hurtbox.x + hurtbox.width + move.hitboxOffsetX;
        } else {
            hitbox.x = hurtbox.x - move.hitboxOffsetX - move.hitboxWidth;
        }
    }

    public void draw(Graphics g) {
        BufferedImage[] currentAnimArray = animations.get(currentState);
        if (currentAnimArray == null || aniIndex >= currentAnimArray.length) return;

        BufferedImage currentFrame = currentAnimArray[aniIndex];
        if (currentFrame == null) return;

        int drawWidth = (int) (currentFrame.getWidth() * scale);
        int drawHeight = (int) (currentFrame.getHeight() * scale);

        int drawX = (int) (x - (drawWidth / 2));
        int drawY = (int) (y - drawHeight);

        // --- NEW FLIP LOGIC ---
        // Determine if we are rendering normally, or if we need to mirror the sprite
        boolean renderFacingRight = facingRight;

        // If the ripped sprites for throws are backwards, we flip them visually here!
        if (currentState.equals("THROW") ){
            renderFacingRight = !facingRight;
        }

        int flipX = renderFacingRight ? 0 : drawWidth;
        int flipW = renderFacingRight ? 1 : -1;

        g.drawImage(currentFrame, drawX + flipX, drawY, drawWidth * flipW, drawHeight, null);
        drawHitboxes(g); // Hitboxes will still draw in the correct, non-flipped locations!
    }
    public void getFrozen() {
        if (isHit && stunDuration == 360) {
            return;
        }
        // --- NEW: Check if blocking to prevent being frozen ---
        boolean isBlocking = currentState.equals("BLOCK") || currentState.equals("CROUCH_BLOCK");
        if (isBlocking) {
            return; // Immune to freeze while blocking!
        }
        SoundManager.play(SoundManager.Sound.FREEZE);
        isHit = true;
        stunTick = 0;
        stunDuration = 360; // 180 ticks = 1.5 seconds of being frozen solid!

        attackActive = false;
        attacking = false;
        currentAttack = "";
        pushBackSpeed = 0; // Frozen enemies don't slide backward

        // If you have a blue frozen sprite, you can change this state to "FROZEN"
        currentState = "HIT_HIGH";
        aniIndex = 0;
        aniTick = 0;
    }

    /**
     * Shared hit-stun physics: slides the character from the pushback
     * impulse and counts down until the stun ends. Both Player and Enemy
     * call this so getting hit (or thrown) behaves identically regardless of
     * who's on the receiving end — previously Player's version of this logic
     * never actually applied the pushback slide at all.
     */
    protected void updateHitStun() {
        x += pushBackSpeed;

        // Only apply horizontal ground friction if they are touching the floor
        if (!inAir) {
            if (pushBackSpeed > 0) {
                pushBackSpeed -= 0.15f;
                if (pushBackSpeed < 0) pushBackSpeed = 0;
            } else if (pushBackSpeed < 0) {
                pushBackSpeed += 0.15f;
                if (pushBackSpeed > 0) pushBackSpeed = 0;
            }
        }

        // Apply gravity if they were thrown into the air
        if (inAir) {
            y += airSpeed;

            // --- THE FIX ---
            // Multiply gravity by 3.0 so they get slammed to the floor quickly!
            // This ensures they land well before the 100-tick stun timer finishes.
            airSpeed += (gravity * 3.0f);

            // Check if they hit the ground
            if (y >= floorY) {
                y = floorY;
                inAir = false;
                airSpeed = 0f;
                pushBackSpeed = 0f; // Stop them from sliding once they hit the dirt
            }
        }

        enforceScreenBorders();
        updateCollisionBoxes();

        stunTick++;

        // --- THE SECOND FIX ---
        // Add "&& !inAir" to absolutely guarantee they never wake up from a stun
        // while hovering off the ground.
        if (stunTick >= stunDuration && !inAir) {
            isHit = false;
        }
    }

    /**
     * Takes damage from a landed attack and switches to the matching
     * reaction animation: HIT_CROUCH if the victim was crouching at the
     * moment of impact (checked before anything gets overwritten), otherwise
     * HIT_HIGH or HIT_LOW depending on the attacking move's HitLevel.
     */
    public void takeDamage(int amount, boolean knockedRight, Move.HitLevel hitLevel) {
        boolean isBlocking = currentState.equals("BLOCK") || currentState.equals("CROUCH_BLOCK");

        // --- NEW: Block Logic ---
        if (isBlocking) {
            SoundManager.play(SoundManager.Sound.BLOCK);
            // Apply pushback and reduce damage by a lot (e.g., divided by 4)
            super.takeDamage(amount / 4, knockedRight);

            // Apply a short block stun so they slide back while holding the block pose
            stunDuration = 15;
            attacking = false;
            currentAttack = "";

            // RETURN EARLY so the state isn't changed to a hit animation!
            return;
        }
        SoundManager.play(SoundManager.Sound.HIT);
        boolean wasCrouching = currentState.equals("CROUCH");
        super.takeDamage(amount, knockedRight);
        // --- NEW: Reset the stun duration just in case they were thrown previously! ---
        stunDuration = 30;
        attacking = false;
        currentAttack = "";
        if (wasCrouching) {
            currentState = "HIT_CROUCH";
        } else if (hitLevel == Move.HitLevel.LOW) {
            currentState = "HIT_LOW";
        } else {
            currentState = "HIT_HIGH";
        }
        aniIndex = 0;
        aniTick = 0;
    }

    /**
     * Puts this character into the THROWN reaction: bigger pushback than a
     * normal hit, since a throw physically flings you rather than just
     * staggering you.
     */
    /**
     * Puts this character into the THROWN reaction: bigger pushback than a
     * normal hit, since a throw physically flings you rather than just
     * staggering you.
     */
    public void getThrown(boolean knockedRight) {
        SoundManager.play(SoundManager.Sound.HIT);
        currentHealth -= 2;
        if (currentHealth < 0) currentHealth = 0;

        isHit = true;
        stunTick = 0;
        // --- NEW: Extend the stun so the 5-frame animation can finish! ---
        // 5 frames * 20 ticks per frame = 100 ticks
        stunDuration = 100;
        attackActive = false;
        attacking = false;
        currentAttack = "";

        // --- THE FIX ---
        // To throw the enemy BEHIND the player, we invert the pushback direction.
        // If the player is facing right (knockedRight = true), we slide the enemy LEFT (negative).
        // The speed is increased to 6.0f so they don't get stuck inside the player's hurtbox.
        pushBackSpeed = knockedRight ? -2.0f : 2.0f;
        inAir = true;
        airSpeed = -4.f;

        currentState = "THROWN";
        aniIndex = 0;
        aniTick = 0;
    }
}