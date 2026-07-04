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


    private Player player;
    private Enemy enemy;
    public Game() {
        player = new Player(150,425,68,135);
        enemy = new Enemy(600, 425, 68, 145);
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

    public void update() {
        switch (GameState.state) {
            case MENU:
                // We will build menu update logic (like moving the mouse) here later
                break;

            case PLAYING:
                // --- YOUR EXISTING COMBAT LOGIC GOES HERE ---
                player.update();
                enemy.update();

                // Referee Logic (Facing Direction)
                if (player.getX() < enemy.getX()) {
                    player.setFacingRight(true);
                    enemy.setFacingRight(false);
                } else {
                    player.setFacingRight(false);
                    enemy.setFacingRight(true);
                }

                // Collision & Damage Logic
                if (player.attackActive) {
                    if (player.hitbox.intersects(enemy.getHurtbox())) {
                        enemy.takeDamage(5, player.facingRight);
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
                drawUI(g); // Your health bars method
                break;

            case GAME_OVER:
                g.setColor(java.awt.Color.RED);
                g.drawString("KNOCKOUT!", 350, 300);
                break;
        }
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