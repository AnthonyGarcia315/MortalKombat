package main;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import util.LoadSave;

public class Enemy extends Entity {

    private int aniTick, aniIndex, aniSpeed = 20;
    private HashMap<String, BufferedImage[]> animations = new HashMap<>();

    public Enemy(float x, float y, int width, int height) {
        super(x, y, width, height);
        loadAnimations();
    }

    private void loadAnimations() {
        // Load the exact same idle animation as Player 1 for now!
        animations.put("IDLE", LoadSave.GetSpriteSequence("/subzero/idle/subzero_idle_", 9));
    }

    public void update() {
        if (isHit) {
            x+=pushBackSpeed;
            if (pushBackSpeed > 0) {
                pushBackSpeed -= 0.15f;
                if (pushBackSpeed < 0) pushBackSpeed = 0;
            } else if (pushBackSpeed < 0) {
                pushBackSpeed += 0.15f;
                if (pushBackSpeed > 0) pushBackSpeed = 0;
            }
            enforceScreenBorders();
            updateCollisionBoxes();
            stunTick++;
            if (stunTick >= stunDuration) {
                isHit = false;
            }
            return; // Exit early so he doesn't play his idle animation

        }
        enforceScreenBorders();
        updateCollisionBoxes();
        // Just tick the animation forward so he breathes
        aniTick++;
        if (aniTick >= aniSpeed) {
            aniTick = 0;
            aniIndex++;
            if (aniIndex >= animations.get("IDLE").length) {
                aniIndex = 0;
            }
        }
    }

    public void draw(Graphics g) {
        BufferedImage currentFrame = animations.get("IDLE")[aniIndex];

        int drawWidth = (int) (currentFrame.getWidth() * scale);
        int drawHeight = (int) (currentFrame.getHeight() * scale);

        int drawX = (int) (x - (drawWidth / 2));
        int drawY = (int) (y - drawHeight);

        // Uses the exact same flip math inherited from Entity!
        int flipX = facingRight ? 0 : drawWidth;
        int flipW = facingRight ? 1 : -1;

        g.drawImage(currentFrame, drawX + flipX, drawY, drawWidth * flipW, drawHeight, null);
    }
}