package main;

public class Enemy extends Fighter {

    private Player player;
    private int aiActionTimer = 0; // Prevents the AI from spamming attacks every single frame
    private int actionDelay = 60;  // How long the AI waits before making a new decision
    private Game game;
    public Enemy(float x, float y, int width, int height, String characterName, Player player,Game game) {
        super(x, y, width, height, characterName);
        this.player = player;
        this.game=game;
    }

    public void update() {
        if (isHit) {
            updateAnimationTick();
            updateHitStun();
            return;
        }

        // Face the player constantly
        if (player.getX() < this.x) {
            facingRight = false;
        } else {
            facingRight = true;
        }

        // --- AI DECISION LOGIC ---
        if (!attacking && !inAir) {
            aiActionTimer++;

            if (aiActionTimer >= actionDelay) {
                makeDecision();
                aiActionTimer = 0; // Reset timer after making a move
                actionDelay = 20 + (int)(Math.random() * 30); // Randomize the next delay so they aren't totally predictable
            } else if (currentState.equals("IDLE") || currentState.equals("WALK")) {
                // Default behavior while waiting to attack: slowly walk towards the player
                float distance = Math.abs(player.getX() - this.x);
                if (distance > 80) {
                    currentState = "WALK";
                    if (facingRight) x += (speed * 0.5f); // Walk slightly slower than player
                    else x -= (speed * 0.5f);
                } else {
                    currentState = "IDLE";
                }
            }
        }
        updatePhysics();

        enforceScreenBorders();
        updateCollisionBoxes();

        // Only update attack hitboxes if the enemy is throwing a strike
        if (attacking) {
            updateAttackHitbox();
        }

        updateAnimationTick();
    }
    private void updatePhysics() {
        // 1. Slide physics
        if (attacking && currentAttack.equals("SLIDE") && attackActive) {
            if (facingRight) x += speed * 10.5f;
            else x -= speed * 10.5f;
        }
        // 2. Projectile spawning
        else if (attacking && currentAttack.equals("ICE_BALL")) {
            if (aniIndex == 2 && aniTick == 1 && game != null) {
                game.spawnProjectile(x, y - 90, facingRight, characterName);
            }
        }

        // 3. Gravity & Landing (Fixes the mid-air stuck animation)
        if (inAir) {
            y += airSpeed;
            airSpeed += gravity;
            x += horizontalAirSpeed;

            if (y >= floorY) {
                y = floorY;
                inAir = false;
                airSpeed = 0f;
                horizontalAirSpeed = 0f;

                // Break out of airborne attacks upon landing
                if (attacking && (currentAttack.equals("JUMP_KICK") || currentAttack.equals("JUMP_PUNCH"))) {
                    attacking = false;
                    currentAttack = "";
                    attackActive = false;
                    currentState = "IDLE";
                    aniIndex = 0;
                }
            }
        }
    }

    private void makeDecision() {
        float distance = Math.abs(player.getX() - this.x);
        int randomMove = (int) (Math.random() * 100);

        if (distance > 200) {
            // OUTSIDE RANGE: Close the gap or throw a projectile
            if (randomMove < 30) {
                attacking = true;
                currentAttack = "ICE_BALL";
            } else if (randomMove < 60) {
                attacking = true;
                currentAttack = "SLIDE";
            }
        } else if (distance > 70 && distance <= 200) {
            // STRIKING RANGE: Perfect for kicks or sweeps
            attacking = true;
            if (randomMove < 40) {
                currentAttack = "ATTACK_KICK";
            } else if (randomMove < 80) {
                currentAttack = "SWEEP";
            } else {
                currentAttack = "JUMP_KICK";
                inAir = true;
                airSpeed = jumpSpeed;
                horizontalAirSpeed = facingRight ? speed * 2 : -speed * 2;
            }
        } else {
            // IN THE POCKET: Close combat
            attacking = true;
            if (randomMove < 40) {
                currentAttack = "ATTACK_PUNCH";
            } else if (randomMove < 70) {
                currentAttack = "UPPERCUT";
            } else {
                currentAttack = "THROW";
            }
        }
        if (attacking) {
            currentState = currentAttack;
            aniIndex = 0;
            aniTick = 0;
        }
    }
}