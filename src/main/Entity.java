package main;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Graphics;
public abstract class Entity {

    // --- Core Spatial Data ---
    protected float x, y;
    protected int width, height;
    protected float scale = 1.2f;

    // --- Core Physics & Gravity ---
    protected int speed = 1;
    protected int floorY = 425;
    protected float airSpeed = 0f;
    protected float gravity = 0.1f;
    protected float jumpSpeed = -6.5f;

    protected boolean inAir = false;
    protected boolean jumpImpulseApplied = false;
    protected int jumpTakeoffFrame = 2;
    protected float horizontalAirSpeed = 0f;

    protected boolean facingRight = true;

    protected Rectangle hurtbox;
    protected Rectangle hitbox;
    protected boolean attackActive = false;

    protected int maxHealth = 100;
    protected int currentHealth = 100;
    protected String currentState = "IDLE";

    // --- HIT STUN ---
    protected boolean isHit = false;
    protected int stunTick = 0;
    protected int stunDuration = 30; // 30 ticks = half a second of being paralyzed

    // NEW: Tracks the physical slide when punched
    protected float pushBackSpeed = 0f;

    // ADD THESE TO ENTITY.JAVA
    public int getCurrentHealth() { return currentHealth; }
    public int getMaxHealth() { return maxHealth; }

    // ADD THIS METHOD TO THE BOTTOM OF ENTITY:
    public void takeDamage(int amount, boolean knockedRight) {
        currentHealth -= amount;
        if (currentHealth < 0) {
            currentHealth = 0;
        }

        // Force the character into stun mode!
        isHit = true;
        stunTick = 0;

        // CRITICAL: If they were in the middle of punching, cancel it!
        attackActive = false;
        if (knockedRight) {
            pushBackSpeed = 1.5f; // Slide right
        } else {
            pushBackSpeed = -1.5f; // Slide left
        }
    }
    // ADD THIS TO ENTITY.JAVA
    protected void enforceScreenBorders() {
        // 1. The Left Wall (0)
        if (x - (hurtbox.width / 2) < 0) {
            x = hurtbox.width / 2;
        }

        // 2. The Right Wall (800)
        // (Change 800 to Game.GAME_WIDTH if you are using Kaarin's scaling constants!)
        if (x + (hurtbox.width / 2) > Game.GAME_WIDTH) {
            x = Game.GAME_WIDTH - (hurtbox.width / 2);
        }
    }
    // Constructor forces anyone who extends Entity to provide starting coordinates
    public Entity(float x, float y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        hurtbox = new Rectangle(0, 0, 60, 155);

        // Let's say his punch reaches 45px outward, and is 30px tall
        hitbox = new Rectangle(0, 0, 45, 30);
    }
//    public void takeDamage(int amount) {
//        currentHealth -= amount;
//        if (currentHealth <= 0) {
//            currentHealth = 0;
//            currentState = "DEAD";
//        } else {
//            currentState = "HIT_STUN"; // Forces them to flinch!
//        }
//    }
    protected void updateCollisionBoxes() {
        // Sync Hurtbox (Body)
        hurtbox.x = (int) (x - (hurtbox.width / 2));
        hurtbox.y = (int) (y - hurtbox.height);

        // Sync Hitbox (Fist)
        hitbox.y = (int) (y - (hurtbox.height ));

        if (facingRight) {
            hitbox.x = hurtbox.x + hurtbox.width;
        } else {
            hitbox.x = hurtbox.x - hitbox.width;
        }
    }
    public float getX() { return x; }
    public void setFacingRight(boolean facingRight) { this.facingRight = facingRight; }
    public Rectangle getHurtbox() { return hurtbox; }

    // --- DEBUG RENDERER ---
    // This is incredibly important for testing! We want to SEE the invisible boxes.
    public void drawHitboxes(Graphics g) {
        // Draw Hurtbox in Blue
        g.setColor(Color.BLUE);
        g.drawRect(hurtbox.x, hurtbox.y, hurtbox.width, hurtbox.height);

        // Draw Hitbox in Red (Only if an attack is happening)
        if (attackActive) {
            g.setColor(Color.RED);
            g.drawRect(hitbox.x, hitbox.y, hitbox.width, hitbox.height);
        }
    }
}