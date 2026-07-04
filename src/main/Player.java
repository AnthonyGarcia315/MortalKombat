package main;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import util.LoadSave;

public class Player extends Entity{
    // Movement flags from earlier
    private boolean up, down, left, right, jump,attacking;
    // --- Sprite Management ---
    private int aniTick, aniIndex, aniSpeed = 20; // Lower number means faster animation
    //private int playerAction = 0; // 0 = IDLE, 1 = RUNNING, etc.
    //private String currentState = "IDLE";
    private HashMap<String, BufferedImage[]> animations = new HashMap<>();

    public Player(float x, float y,int width, int height) {
        super(x,y,width,height);
        loadAnimations();
    }

    private void loadAnimations() {
        // If you have 9 idle frames named subzero_idle_0.png through subzero_idle_9.png
        animations.put("IDLE", LoadSave.GetSpriteSequence("/subzero/idle/subzero_idle_", 9));

        // If you download his punch and name them subzero_punch_0.png to subzero_punch_4.png
        animations.put("ATTACK_PUNCH", LoadSave.GetSpriteSequence("/subzero/attackPunch/subzero_punch_", 9));
        animations.put("CROUCH", LoadSave.GetSpriteSequence("/subzero/crouch/subzero_crouch_", 3));
        animations.put("JUMP", LoadSave.GetSpriteSequence("/subzero/jump/subzero_jump_", 3));
        animations.put("JUMP_FLIP", LoadSave.GetSpriteSequence("/subzero/jump/subzero_jumpflip_", 8));

        // If you download his walk...
        animations.put("WALK", LoadSave.GetSpriteSequence("/subzero/walk/subzero_walk_", 9));
    }

    public void update() {
        updatePosition();
        updateCollisionBoxes(); // NEW: Move the boxes with the player!
        setAnimation();
        updateAnimationTick();
    }

    private void setAnimation() {
        String startAnim = currentState;

        // 1. HIGHEST PRIORITY: The Attack Lock
        if (attacking) {
            currentState = "ATTACK_PUNCH";
        }
        // 2. NORMAL MOVEMENT (Only happens if we aren't attacking)
        else {
            if (inAir) {
                if (horizontalAirSpeed != 0&& jumpImpulseApplied) {
                    currentState = "JUMP_FLIP";
                } else {
                    currentState = "JUMP";
                }
            } else if (down) {
                currentState = "CROUCH";
            } else if (left || right) {
                currentState = "WALK";
            } else {
                currentState = "IDLE";
            }
        }

        // 3. Reset frame counter ONLY if the state actually changed
        if (!startAnim.equals(currentState)) {
            aniIndex = 0;
            aniTick = 0;
        }
    }

    private void updateAnimationTick() {
        aniTick++;
        if (aniTick >= aniSpeed) {
            aniTick = 0;
            aniIndex++;

            int currentAnimLength = animations.get(currentState).length;

            if (aniIndex >= currentAnimLength) {
                if (currentState.equals("CROUCH") || currentState.equals("JUMP")) {
                    aniIndex = currentAnimLength - 1;
                }
                // THE FIX: Turn off the attacking flag when the punch finishes!
                else if (currentState.equals("ATTACK_PUNCH")) {
                    attacking = false; // This unlocks the state machine!
                    currentState = "IDLE";
                    aniIndex = 0;
                }
                else {
                    aniIndex = 0;
                }
            }
            if (currentState.equals("ATTACK_PUNCH") && (aniIndex >= 3 && aniIndex <= 5)) {
                attackActive = true;
            } else {
                attackActive = false;
            }
        }
    }

    private void updatePosition() {
        if (isHit) {
            stunTick++;
            if (stunTick >= stunDuration) {
                isHit = false; // Break out of stun!
            }
            // EXIT EARLY! Do not process normal animations or movement.
            return;
        }
        if (down && !inAir) {
            return;
        }

        // 1. INTENTION: The player wants to jump!
        if (up && !inAir) {
            inAir = true;
            jumpImpulseApplied = false; // Reset our takeoff flag
            // DO NOT apply airSpeed here anymore! We just prepare for takeoff.
            if (left) {
                horizontalAirSpeed = (float) (-speed*2.); // Forward/Backward Jump (Left)
            } else if (right) {
                horizontalAirSpeed = (float) (speed*2.);  // Forward/Backward Jump (Right)
            } else {
                horizontalAirSpeed = 0;      // Neutral Jump (Straight Up)
            }
        }

        if (!inAir && !currentState.equals("ATTACK_PUNCH")) {
            if (left) x -= speed;
            if (right) x += speed;
        }

        // 2. THE AIRBORNE LOGIC
        if (inAir) {

            if ((currentState.equals("JUMP")||currentState.equals("JUMP_FLIP")) && !jumpImpulseApplied && aniIndex >= jumpTakeoffFrame) {
                airSpeed = jumpSpeed;
                jumpImpulseApplied = true;
            }

            if (jumpImpulseApplied) {
                y += airSpeed;
                airSpeed += gravity;

                // NEW: Apply the locked-in horizontal speed every frame
                x += horizontalAirSpeed;

                // Floor Collision
                if (y >= floorY) {
                    y = floorY;
                    inAir = false;
                    airSpeed = 0f;
                    horizontalAirSpeed = 0f; // Reset horizontal momentum on landing!
                }
            }


        }
        // --- THE SCREEN BORDERS ---
        // 1. Define a static body width (adjust this until he stops perfectly at the edge)
        enforceScreenBorders();
    }

    public void draw(Graphics g) {
        BufferedImage[] currentAnimArray = animations.get(currentState);

        if (currentAnimArray != null && currentAnimArray[aniIndex] != null) {
            BufferedImage currentFrame = currentAnimArray[aniIndex];

            int drawWidth = (int) (currentFrame.getWidth() * scale);
            int drawHeight = (int) (currentFrame.getHeight() * scale);

            int drawX = (int) (x - (drawWidth / 2));
            int drawY = (int) (y - drawHeight);

            // --- THE FLIP MATH ---
            // If facing right, draw normally. If facing left, shift the start point to the right edge and draw backward!
            int flipX = facingRight ? 0 : drawWidth;
            int flipW = facingRight ? 1 : -1;

            g.drawImage(currentFrame, drawX + flipX, drawY, drawWidth * flipW, drawHeight, null);
        }
    }

    // ... Keep your getters and setters down here ...
    public void setLeft(boolean left) { this.left = left; }
    public void setRight(boolean right) { this.right = right; }
    public void setUp(boolean up) { this.up = up; }
    public void setDown(boolean down) { this.down = down; }
    public void setJump(boolean jump) { this.jump = jump; }
    public void setCurrentState(String state){
        currentState=state;
    }

    public void setAttacking(boolean b) {
        attacking=b;
    }
}


