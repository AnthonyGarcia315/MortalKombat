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
    protected float gravity = 0.05f;
    protected float jumpSpeed = -4.5f;

    protected boolean inAir = false;
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
    protected boolean isFrozen = false; // --- NEW: Separate freeze state from hit invincibility ---
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

        // Default hitbox before any move-specific sizing kicks in (see
        // Fighter.updateAttackHitbox(), which overrides this per-attack).
        hitbox = new Rectangle(0, 0, 45, 30);
    }

    protected void updateCollisionBoxes() {
        // 1. Set the dimensions dynamically based on state BEFORE positioning
        // (Include CROUCH_KICK or SWEEP here if you want them to stay low during attacks)
        if (currentState.equals("CROUCH")|| currentState.equals("SLIDE")) {
            hurtbox.height = 77; // roughly half of your original 155
        } else if (currentState.equals("CROUCH_BLOCK")) {
            hurtbox.height=120;
        } else {
            hurtbox.height = 155; // Reset to standing height
        }

        // 2. Sync Hurtbox (Body) position
        hurtbox.x = (int) (x - (hurtbox.width / 2));
        // Because Y is anchored to the bottom (y - height), shrinking the height
        // automatically pulls the top of the box downward!
        hurtbox.y = (int) (y - hurtbox.height);

        // 3. Sync Hitbox (Attack Box)
        hitbox.y = (int) (y - hurtbox.height);

        // Adjust the attack hitbox downward if they punch while crouching
        if (currentState.equals("CROUCH")) {
            hitbox.y += hitbox.height / 2;
        }

        // 4. Sync Hitbox X orientation
        if (facingRight) {
            hitbox.x = hurtbox.x + hurtbox.width;
        } else {
            hitbox.x = hurtbox.x - hitbox.width;
        }
    }
    public String getCurrentState() { return currentState; }
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