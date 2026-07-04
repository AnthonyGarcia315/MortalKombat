package main;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import util.LoadSave;

public class Player {

    private int x = 150, y = 400;
    private int width = 128, height = 128;
    private float scale = 1.5f;// Scale it up a bit so it's visible

    // Movement flags from earlier
    private boolean up, down, left, right, jump,attacking;
    private int speed = 1;

    // --- Sprite Management ---

    private int aniTick, aniIndex, aniSpeed = 20; // Lower number means faster animation
    //private int playerAction = 0; // 0 = IDLE, 1 = RUNNING, etc.
    private String currentState = "IDLE";

    // --- Physics & Gravity ---
    private int floorY = 400; // The absolute bottom line of your screen
    private float airSpeed = 0f; // Current upward/downward momentum
    private float gravity = 0.15f; // How fast he gets pulled down
    private float jumpSpeed = -7.5f; // Initial upward burst (Negative moves UP)
    private boolean inAir = false; // Is he currently jumping/falling?
    private boolean jumpImpulseApplied = false; // Has the physics kicked in yet?
    private int jumpTakeoffFrame = 2; // Which frame of the animation do his feet leave the floor?
    private float horizontalAirSpeed = 0f; // Stores left/right momentum during a jump

    private HashMap<String, BufferedImage[]> animations = new HashMap<>();

    public Player() {
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
        }
    }

    private void updatePosition() {
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
        int logicalBodyWidth = 60;

        // 2. The Left Wall
        if (x - (logicalBodyWidth / 2) < 0) {
            x = logicalBodyWidth / 2;
        }

        // 3. The Right Wall
        // (Assuming your window is 800 pixels wide. If you used Kaarin's Game.GAME_WIDTH constant, replace 800 with Game.GAME_WIDTH)
        if (x + (logicalBodyWidth / 2) > 800) {
            x = 800 - (logicalBodyWidth / 2);
        }
    }

    public void draw(Graphics g) {
        // Instead of g.fillRect(Color.BLUE), we draw the exact subimage frame!
        BufferedImage[] currentAnimArray = animations.get(currentState);
        if (currentAnimArray != null && currentAnimArray[aniIndex] != null) {
            int actualFrameIndex = aniIndex;
            if (currentState.equals("JUMP_FLIP") && horizontalAirSpeed < 0) {
                actualFrameIndex = (currentAnimArray.length - 1) - aniIndex;
            }
            BufferedImage currentFrame = currentAnimArray[actualFrameIndex];
            // 1. Get the natural size of THIS specific frame and scale it up
            int drawWidth = (int) (currentFrame.getWidth() * scale);
            int drawHeight = (int) (currentFrame.getHeight() * scale);
            // 2. Calculate the Anchor Point (Center his body, plant his feet on the 'y' line)
            int drawX = x - (drawWidth / 2);
            int drawY = y - drawHeight;
            g.drawImage(currentFrame, drawX, drawY, drawWidth, drawHeight, null);        }
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


