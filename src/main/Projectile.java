package main;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import util.LoadSave;

public class Projectile {
    private float x, y;
    private float speed = 5.0f;
    private boolean facingRight;
    private boolean active = true;
    private int width = 30, height = 20;
    private Rectangle hitbox;

    // Simple placeholder animation for the ice ball flying
    private BufferedImage[] frames;
    private int aniIndex = 0, aniTick = 0, aniSpeed = 15;

    public Projectile(float x, float y, boolean facingRight, String characterName) {
        this.x = x;
        this.y = y;
        this.facingRight = facingRight;
        this.hitbox = new Rectangle((int) x, (int) y, width, height);

        // Try to load a generic projectile sequence if you have one, or reuse fallback
        frames = LoadSave.GetSpriteSequence("/" + characterName + "/iceBall/subzero_projectile_", 2);
        if (frames == null || frames[0] == null) {
            frames = new BufferedImage[1]; // Fallback to empty/square if asset doesn't exist
        }
    }

    public void update() {
        // Move the projectile forward
        if (facingRight) x += speed;
        else x -= speed;

        // Sync hitbox
        hitbox.x = (int) x;
        hitbox.y = (int) y;

        // Simple loop animation for the flying ice ball
        if (frames != null && frames[0] != null) {
            aniTick++;
            if (aniTick >= aniSpeed) {
                aniTick = 0;
                aniIndex++;
                if (aniIndex >= frames.length) aniIndex = 0;
            }
        }

        // Deactivate if it flies off-screen
        if (x < 0 || x > Game.GAME_WIDTH) {
            active = false;
        }
    }

    public void draw(Graphics g) {
        if (!active) return;

        if (frames != null && frames[aniIndex] != null) {
            int flipX = facingRight ? 0 : width;
            int flipW = facingRight ? 1 : -1;
            g.drawImage(frames[aniIndex], (int) x + flipX, (int) y, width * flipW, height, null);
        } else {
            // Debug fallback: light blue square if no asset exists yet
            g.setColor(new Color(135, 206, 250));
            g.fillRect((int) x, (int) y, width, height);
        }
    }

    public Rectangle getHitbox() { return hitbox; }
    public boolean isActive() { return active; }
    public void deactivate() { this.active = false; }
}