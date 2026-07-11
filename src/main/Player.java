package main;

import java.util.LinkedList;
import util.SoundManager;

public class Player extends Fighter {

    // Movement and action flags
    private boolean up, down, left, right, jump, blocking;

    private LinkedList<InputEvent> inputBuffer = new LinkedList<>();
    private long currentFrame = 0;
    private final long BUFFER_WINDOW = 40;
    private int comboStep = 0;
    private Game game;

    public Player(float x, float y, int width, int height, String characterName, Game game) {
        super(x, y, width, height, characterName);
        this.game=game;
    }

    public void registerInput(InputEvent.Button button) {
        inputBuffer.addLast(new InputEvent(button, currentFrame));
    }
    /**
     * Checks whether the tail of the input buffer matches an arbitrary
     * (already facing-resolved) sequence of buttons, oldest-to-newest.
     */
    private boolean checkSequence(java.util.List<InputEvent.Button> sequence) {
        if (inputBuffer.size() < sequence.size()) return false;

        java.util.Iterator<InputEvent> it = inputBuffer.descendingIterator();
        InputEvent[] recent = new InputEvent[sequence.size()];
        for (int i = 0; i < sequence.size(); i++) {
            recent[i] = it.next(); // recent[0] = newest input
        }
        for (int i = 0; i < sequence.size(); i++) {
            InputEvent.Button expected = sequence.get(sequence.size() - 1 - i);
            if (recent[i].button != expected) return false;
        }
        return true;
    }

    /** Turns a special move's abstract FORWARD/BACK into real LEFT/RIGHT based on current facing. */
    private java.util.List<InputEvent.Button> resolveSequence(java.util.List<InputEvent.Button> abstractSeq) {
        java.util.List<InputEvent.Button> resolved = new java.util.ArrayList<>();
        for (InputEvent.Button b : abstractSeq) {
            if (b == InputEvent.Button.FORWARD) {
                resolved.add(facingRight ? InputEvent.Button.RIGHT : InputEvent.Button.LEFT);
            } else if (b == InputEvent.Button.BACK) {
                resolved.add(facingRight ? InputEvent.Button.LEFT : InputEvent.Button.RIGHT);
            } else {
                resolved.add(b);
            }
        }
        return resolved;
    }

    public void update() {
        currentFrame++;
        cleanBuffer();
        processBuffer();

        if (isHit) {
            // Keep ticking whichever reaction animation takeDamage()/
            // getThrown() set (HIT_HIGH/HIT_LOW/HIT_CROUCH/THROWN) instead of
            // letting setAnimation() immediately overwrite it with whatever
            // the movement keys say — it used to just freeze on the pose the
            // character happened to be in when hit, since nothing ticked the
            // animation or applied the pushback slide during stun.
            updateAnimationTick();
            updateHitStun();
            return;
        }

        // Animation state is decided and advanced FIRST, then physics and
        // hitboxes are derived from it — this fixes the old ordering, where
        // updatePosition() read a stale aniIndex from before setAnimation()
        // and updateAnimationTick() had run for the current tick.
        setAnimation();
        updateAnimationTick();
        updatePosition();
        updateCollisionBoxes();
        updateAttackHitbox();
    }

    private void cleanBuffer() {
        while (!inputBuffer.isEmpty() && (currentFrame - inputBuffer.getFirst().frame > BUFFER_WINDOW)) {
            inputBuffer.removeFirst();
        }
    }

    private void processBuffer() {
        if (inputBuffer.isEmpty()) return;

        if (!attacking || (currentState.equals("ATTACK_PUNCH") && aniIndex >= 6)) {
            // --- SPECIAL MOVES (character-specific; checked before normal moves) ---
            for (SpecialMove special : specialMoves) {
                if (checkSequence(resolveSequence(special.sequence))) {
                    attacking = true;
                    currentAttack = special.moveName;
                    playAttackSound(currentAttack);
                    inputBuffer.clear(); // Clear the buffer so we don't accidentally re-trigger it
                    return;
                }
            }

            // --- NORMAL MOVES ---
            InputEvent oldestValidInput = inputBuffer.getFirst();

            // PUNCH LOGIC
            if (oldestValidInput.button == InputEvent.Button.PUNCH) {
                attacking = true;
                if (inAir) currentAttack = "JUMP_PUNCH";
                else if (down) currentAttack = "UPPERCUT";
                else currentAttack = "ATTACK_PUNCH";
                playAttackSound(currentAttack);
                inputBuffer.removeFirst();
            }
            // KICK LOGIC
            else if (oldestValidInput.button == InputEvent.Button.KICK) {
                attacking = true;
                if (inAir) currentAttack = "JUMP_KICK";
                else if (down && (left || right)) currentAttack = "SWEEP";
                else if (down) currentAttack = "CROUCH_KICK";
                else currentAttack = "ATTACK_KICK";
                playAttackSound(currentAttack);
                inputBuffer.removeFirst();
            }
            // THROW LOGIC
            else if (oldestValidInput.button == InputEvent.Button.THROW) {
                attacking = true;
                currentAttack = "THROW";
                playAttackSound(currentAttack);
                inputBuffer.removeFirst();
            }
        }
    }

    private void setAnimation() {
        String startAnim = currentState;

        // 1. HIGHEST PRIORITY: Attacks
        if (attacking) {
            currentState = currentAttack;
        }
        // 2. BLOCKING
        else if (blocking && !inAir) {
            if (down) {
                currentState = "CROUCH_BLOCK";
            } else {
                currentState = "BLOCK";
            }
        }
        // 3. NORMAL MOVEMENT
        else {
            if (inAir) {
                currentState = (horizontalAirSpeed != 0) ? "JUMP_FLIP" : "JUMP";
            } else if (down) {
                currentState = "CROUCH";
            } else if (left || right) {
                currentState = "WALK";
            } else {
                currentState = "IDLE";
            }
        }

        if (!startAnim.equals(currentState)) {
            aniIndex = 0;
            aniTick = 0;
        }
    }

    private void updatePosition() {
        // Prevent movement if ducking or blocking, unless in the air
        if ((down || blocking) && !inAir) {
            return;
        }

        if (up && !inAir &&!attacking) {
            inAir = true;
            // Apply the jump impulse the instant takeoff happens instead of
            // waiting for aniIndex to reach a "takeoff frame" on the slow
            // shared animation clock. That old gating meant the character
            // hung motionless in the air for ~40-60 ticks (0.2-0.3s) after
            // pressing jump, since gravity/airSpeed never engaged until the
            // wind-up animation had ticked forward — it looked like the
            // jump was broken/laggy. Now the character actually leaves the
            // ground the moment the key is pressed, and the animation just
            // plays out over the arc like it should.
            airSpeed = jumpSpeed;
            SoundManager.play(SoundManager.Sound.JUMP);
            if (left) {
                horizontalAirSpeed = (float) (-speed * 2.);
            } else if (right) {
                horizontalAirSpeed = (float) (speed * 2.);
            } else {
                horizontalAirSpeed = 0;
            }
        }

// --- GROUND MOVEMENT & SLIDE PHYSICS ---
        if (attacking && currentAttack.equals("SLIDE") && attackActive) {
            // Physically launch the player forward during the active frames of the slide
            if (facingRight) x += speed * 10.5f;
            else x -= speed * 10.5f;
        }
        else if (attacking && currentAttack.equals("ICE_BALL")) {
            // Trigger the projectile precisely on its active frame (frame index 3)
            // We use static access or a game reference depending on how your engine passes things
            if (aniIndex == 2 && aniTick == 1) {
                // Safe default height check (chest level roughly y - 100)
                // Assuming you can access Game through a reference, or pass it via main setup.
                // If you don't have static access, adjust this line to your project structure.
                // example: game.spawnProjectile(...)
                game.spawnProjectile(x,y-90,facingRight,characterName);
            }
        }
        else if (!inAir && !attacking) {
            if (left) x -= speed;
            if (right) x += speed;
        }

        if (inAir) {
            y += airSpeed;
            airSpeed += gravity;
            x += horizontalAirSpeed;

            if (y >= floorY) {
                y = floorY;
                inAir = false;
                airSpeed = 0f;
                horizontalAirSpeed = 0f;

                // JUMP_KICK and JUMP_PUNCH now hold their last frame while
                // airborne (see Fighter.HOLD_LAST_FRAME_STATES) instead of
                // resetting to IDLE mid-air, so nothing else clears
                // "attacking" for them anymore — landing has to do it.
                if (attacking && (currentAttack.equals("JUMP_KICK") || currentAttack.equals("JUMP_PUNCH"))) {
                    attacking = false;
                    currentAttack = "";
                    attackActive = false;
                }
            }
        }
        enforceScreenBorders();
    }

    // Setters
    public void setLeft(boolean left) { this.left = left; }
    public void setRight(boolean right) { this.right = right; }
    public void setUp(boolean up) { this.up = up; }
    public void setDown(boolean down) { this.down = down; }
    public void setJump(boolean jump) { this.jump = jump; }
    public void setCurrentState(String state){ currentState=state; }
    public void setAttacking(boolean b) { attacking = b; }
    public void setBlocking(boolean b) { blocking = b; }
}