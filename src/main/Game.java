package main;

import util.LoadSave;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class Game implements Runnable {

    private GamePanel gamePanel;
    private Thread gameThread;
    private final int FPS_SET = 120;
    private final int UPS_SET = 200;
    private BufferedImage titleScreenImg;
    private BufferedImage backgroundImg;
    public final static int TILES_DEFAULT_SIZE = 32;
    public final static float SCALE = 1.0f; // Change this to 3.0f or 4.0f to make the window massive!
    public final static int TILES_IN_WIDTH = 26;
    public final static int TILES_IN_HEIGHT = 14;
    public final static int TILES_SIZE = (int) (TILES_DEFAULT_SIZE * SCALE);

    // The final window dimensions are calculated automatically
    public final static int GAME_WIDTH = TILES_SIZE * TILES_IN_WIDTH;
    public final static int GAME_HEIGHT = TILES_SIZE * TILES_IN_HEIGHT;
    private Projectile activeProjectile = null;


    private Player player;
    private Enemy enemy;
    public Game() {
        player = new Player(150, 425, 68, 135, "subzero",this);
        // Enemy now takes a characterName the same way Player does — swap
        // "subzero" for any other character folder under /res once you have
        // assets for one (e.g. new Enemy(600, 425, 68, 145, "scorpion")).
        enemy = new Enemy(600, 425, 68, 145, "subzero",player,this);
        // Load your background image here!
        backgroundImg = LoadSave.GetSprite("/bg.png");
        titleScreenImg = LoadSave.GetSprite("/title.png");
        gamePanel = new GamePanel(this);

        // Create the window right here in the Game class
        javax.swing.JFrame window = new javax.swing.JFrame();
        window.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        window.add(gamePanel);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        window.setResizable(true);

        startGameLoop();
    }

    private void startGameLoop() {
        gameThread = new Thread(this);
        gameThread.start();
    }
    private void checkCombat(Fighter attacker, Fighter defender) {
        // 1. Is the attacker actually attacking?
        if (!attacker.attackActive || attacker.isHit) return;

        // 2. Is the defender already stunned? (Prevents multi-hit glitches from one punch)
        if (defender.isHit) return;

        // 3. Did the hitboxes intersect?
        if (attacker.hitbox.intersects(defender.hurtbox)) {

            boolean knockedRight = attacker.facingRight;
            String moveName = attacker.currentAttack;
            Move moveData = attacker.moveSet.get(moveName);

            // Special Case: Throws
            if (moveName.equals("THROW")) {
                defender.getThrown(knockedRight);
                return;
            }

            // Special Case: Projectiles (if your ICE_BALL is physical and not spawned)
            if (moveName.equals("ICE_BALL")) {
                defender.getFrozen();
                return;
            }

            // Normal Strikes
            if (moveData != null) {
                defender.takeDamage(moveData.damage, knockedRight, moveData.hitLevel);
            } else {
                // Fallback just in case
                defender.takeDamage(10, knockedRight, Move.HitLevel.HIGH);
            }

            // Turn off the attacker's active hitbox so they don't hit 3 times in one swing
            attacker.attackActive = false;
        }
    }

    public void update() {
        switch (GameState.state) {
            case MENU:
                // We will build menu update logic (like moving the mouse) here later
                break;

            case PLAYING:
                // --- YOUR EXISTING COMBAT LOGIC GOES HERE ---
                player.update();
                enemy.update();
                checkCombat(player, enemy);
                checkCombat(enemy, player); // Do it both ways!
                // Referee Logic (Facing Direction)
                if (!player.getCurrentState().equals("THROW") && !enemy.getCurrentState().equals("THROWN")) {
                    if (player.getX() < enemy.getX()) {
                        player.setFacingRight(true);
                        enemy.setFacingRight(false);
                    } else {
                        player.setFacingRight(false);
                        enemy.setFacingRight(true);
                    }
                }
                // --- NEW: Update Projectile ---
                if (activeProjectile != null) {
                    activeProjectile.update();
                    if (!activeProjectile.isActive()) {
                        activeProjectile = null;
                    } else if (activeProjectile.getHitbox().intersects(enemy.getHurtbox())) {
                        // Projectile hits! Freeze them
                        enemy.getFrozen();
                        activeProjectile = null;
                    }
                }

                // Collision & Damage Logic
                if (player.attackActive) {
                    if (player.hitbox.intersects(enemy.getHurtbox())) {
                        if (player.currentAttack.equals("THROW")) {
                            enemy.getThrown(player.facingRight);
                        }else if (player.currentAttack.equals("ICE_BALL")) {

                        }
                        else {
                            Move usedMove = player.moveSet.get(player.currentAttack);
                            Move.HitLevel level = (usedMove != null) ? usedMove.hitLevel : Move.HitLevel.HIGH;
                            enemy.takeDamage(5, player.facingRight, level);
                        }
                        player.attackActive = false;
                    }
                }

                // CHECK FOR KNOCKOUT!
                if (enemy.getCurrentHealth() <= 0) {
                    GameState.state = GameState.GAME_OVER;
                }
                break;

            case GAME_OVER:
                // Logic for resetting the round goes here
                break;
        }
    }

    public void render(Graphics g) {
        switch (GameState.state) {
            case MENU:
                // Draw ONLY the title screen image when in the MENU state
                if (titleScreenImg != null) {
                    g.drawImage(titleScreenImg, 0, 0, GAME_WIDTH, GAME_HEIGHT, null);
                } else {
                    // Fallback if image fails to load
                    g.setColor(java.awt.Color.BLACK);
                    g.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
                }

                g.setColor(java.awt.Color.WHITE);
                g.drawString("Press ENTER to Start", 320, 500);
                break;

            case PLAYING:
                // Draw ONLY the fighting stage background when in the PLAYING state
                if (backgroundImg != null) {
                    g.drawImage(backgroundImg, 0, 0, GAME_WIDTH, GAME_HEIGHT, null);
                }

                // Draw characters and UI on top of the stage
                player.draw(g);
                enemy.draw(g);
                if (activeProjectile != null) {
                    activeProjectile.draw(g);
                }
                drawUI(g); // Your health bars method
                break;

            case GAME_OVER:
                g.setColor(java.awt.Color.RED);
                g.drawString("KNOCKOUT!", 350, 300);
                break;
        }
    }
    public void spawnProjectile(float x, float y, boolean facingRight, String characterName) {
        this.activeProjectile = new Projectile(x, y, facingRight, characterName);
    }
    // Paste this anywhere inside your Game class (usually below render)
    private void drawUI(Graphics g) {
        // Define how big the bars should be on your screen
        int barWidth = 250;
        int barHeight = 25;
        int topPadding = 40;

        // --- PLAYER 1 (Top Left) ---
        float p1HealthPercentage = (float) player.getCurrentHealth() / player.getMaxHealth();
        int p1GreenWidth = (int) (barWidth * p1HealthPercentage);

        // Red Background
        g.setColor(java.awt.Color.RED);
        g.fillRect(50, topPadding, barWidth, barHeight);

        // Green Foreground
        g.setColor(java.awt.Color.GREEN);
        g.fillRect(50, topPadding, p1GreenWidth, barHeight);

        // White Border
        g.setColor(java.awt.Color.WHITE);
        g.drawRect(50, topPadding, barWidth, barHeight);


        // --- PLAYER 2 (Top Right) ---
        // Assuming your window is 800 pixels wide. (Swap to Game.GAME_WIDTH if you used Kaarin's scaling)
        int p2StartX = 800 - 50 - barWidth;

        float p2HealthPercentage = (float) enemy.getCurrentHealth() / enemy.getMaxHealth();
        int p2GreenWidth = (int) (barWidth * p2HealthPercentage);

        // Red Background
        g.setColor(java.awt.Color.RED);
        g.fillRect(p2StartX, topPadding, barWidth, barHeight);

        // Green Foreground (Anchored to the right so it depletes toward the center!)
        g.setColor(java.awt.Color.GREEN);
        int emptySpace = barWidth - p2GreenWidth;
        g.fillRect(p2StartX + emptySpace, topPadding, p2GreenWidth, barHeight);

        // White Border
        g.setColor(java.awt.Color.WHITE);
        g.drawRect(p2StartX, topPadding, barWidth, barHeight);
    }

    @Override
    public void run() {
        // 1,000,000,000 nanoseconds = 1 second
        // How many nanoseconds should pass before we draw/update?
        double timePerFrame = 1000000000.0 / FPS_SET;
        double timePerUpdate = 1000000000.0 / UPS_SET;

        long previousTime = System.nanoTime();

        int frames = 0;
        int updates = 0;
        long lastCheck = System.currentTimeMillis();

        double deltaU = 0;
        double deltaF = 0;

        while (gameThread != null) {
            long currentTime = System.nanoTime();

            // Calculate how much time has passed since the last loop
            deltaU += (currentTime - previousTime) / timePerUpdate;
            deltaF += (currentTime - previousTime) / timePerFrame;
            previousTime = currentTime;

            // If enough time has passed for an UPDATE, do the math
            if (deltaU >= 1) {
                update();
                updates++;
                deltaU--;
            }

            // If enough time has passed for a FRAME, draw the screen
            if (deltaF >= 1) {
                gamePanel.repaint();
                frames++;
                deltaF--;
            }

            // Print the FPS/UPS to the console every 1 second
            if (System.currentTimeMillis() - lastCheck >= 1000) {
                lastCheck = System.currentTimeMillis();
                System.out.println("FPS: " + frames + " | UPS: " + updates);
                frames = 0;
                updates = 0;
            }
        }
    }

    public Player getPlayer() {
        return player;
    }
}