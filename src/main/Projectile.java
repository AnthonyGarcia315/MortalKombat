package main;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import util.LoadSave;

public class Projectile {
    private float x, y;
    private float speed = 2.5f;  // was 5.0f -- slower flight
    private boolean facingRight;
    private boolean active = true;
    private int width = 65, height = 45; // was 30x20 -- bigger ice ball (hitbox scales with this too)
    private Rectangle hitbox;

    // Frame indices into the loaded sprite sheet (subzero_projectile_0..6):
    // 0-3 is the ball forming in Sub-Zero's hands (played once, in place),
    // 4 is the "detached and flying" pose it holds for the whole travel,
    // 5-6 is the impact burst that only plays once it actually connects.
    private static final int FORM_FRAME_START = 0;
    private static final int FORM_FRAME_END = 3;
    private static final int TRAVEL_FRAME = 4;
    private static final int IMPACT_FRAME_START = 5;
    private static final int IMPACT_FRAME_END = 6;

    private enum State { FORMING, TRAVELING, IMPACT }
    private State state = State.FORMING;

    private BufferedImage[] frames;
    private int aniTick = 0, aniSpeed = 15;

    private int formIndex = FORM_FRAME_START;
    private int impactIndex = IMPACT_FRAME_START;

    public Projectile(float x, float y, boolean facingRight, String characterName) {
        this.x = x;
        this.y = y;
        this.facingRight = facingRight;
        this.hitbox = new Rectangle((int) x, (int) y, width, height);

        // The flying ice ball lives in its own /projectile/ folder, separate
        // from ICE_BALL (Sub-Zero's 3-frame cast animation, loaded by
        // Fighter.loadAnimations() from /iceBall/). This used to point at
        // the iceBall folder with a hardcoded "subzero_projectile_" prefix
        // and request only 2 frames -- so it never found more than the 2
        // frames it happened to ask for, regardless of how many actually
        // existed. Frame count now comes from CharacterData, same as every
        // other animation, so a future character's projectile just needs a
        // PROJECTILE entry in CharacterRegistry -- no code change here.
        CharacterData data = CharacterRegistry.getData(characterName);
        int frameCount = data.frameCounts.getOrDefault("PROJECTILE", 7);
        String path = "/" + characterName + "/projectile/" + characterName + "_projectile_";

        frames = LoadSave.GetSpriteSequence(path, frameCount);
        if (frames == null || frames.length == 0 || frames[0] == null) {
            System.out.println("\u26A0\uFE0F MISSING PROJECTILE ASSET for " + characterName + ": " + path);
            frames = new BufferedImage[1]; // Fallback to empty/square if asset doesn't exist
        }
    }

    /**
     * Call this the instant the projectile's hitbox connects. Freezes its
     * horizontal movement and kicks off the frame-5/6 impact burst; update()
     * marks it inactive once that burst finishes playing.
     */
    public void hit() {
        if (state == State.IMPACT) return; // already playing the impact -- don't restart it
        state = State.IMPACT;
        impactIndex = IMPACT_FRAME_START;
        aniTick = 0;
    }

    /** True once the ball has connected and is playing its impact burst (frames 5-6). */
    public boolean isHitting() { return state == State.IMPACT; }

    /** True while it's still forming in Sub-Zero's hands -- not a hittable/traveling ball yet. */
    public boolean isForming() { return state == State.FORMING; }

    public void update() {
        hitbox.x = (int) x;
        hitbox.y = (int) y;

        switch (state) {
            case FORMING:
                // Sits at the spawn point (in Sub-Zero's hands) and plays
                // frames 0-3 once. No horizontal movement during this phase.
                aniTick++;
                if (aniTick >= aniSpeed) {
                    aniTick = 0;
                    formIndex++;
                    if (formIndex > FORM_FRAME_END) {
                        // Ball detaches and starts flying on frame 4.
                        state = State.TRAVELING;
                    }
                }
                break;

            case TRAVELING:
                // Stays on the static travel frame (4) the whole flight.
                if (facingRight) x += speed;
                else x -= speed;
                hitbox.x = (int) x;
                hitbox.y = (int) y;

                // Deactivate if it flies off-screen without hitting anything
                if (x < 0 || x > Game.GAME_WIDTH) {
                    active = false;
                }
                break;

            case IMPACT:
                // Play the impact burst (frames 5-6) in place, then deactivate.
                aniTick++;
                if (aniTick >= aniSpeed) {
                    aniTick = 0;
                    impactIndex++;
                    if (impactIndex > IMPACT_FRAME_END || impactIndex >= frames.length) {
                        active = false;
                    }
                }
                break;
        }
    }

    public void draw(Graphics g) {
        if (!active) return;

        BufferedImage frame = null;
        switch (state) {
            case FORMING:
                if (formIndex < frames.length) frame = frames[formIndex];
                break;
            case TRAVELING:
                frame = (TRAVEL_FRAME < frames.length) ? frames[TRAVEL_FRAME] : null;
                break;
            case IMPACT:
                if (impactIndex < frames.length) frame = frames[impactIndex];
                break;
        }
        if (frame == null && frames.length > 0) {
            frame = frames[0]; // fallback if this character's sheet is shorter than expected
        }

        if (frame != null) {
            int flipX = facingRight ? 0 : width;
            int flipW = facingRight ? 1 : -1;
            g.drawImage(frame, (int) x + flipX, (int) y, width * flipW, height, null);
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