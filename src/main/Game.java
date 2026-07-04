package main;

import util.LoadSave;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class Game implements Runnable {

    private GamePanel gamePanel;
    private Thread gameThread;
    private final int FPS_SET = 120;
    private final int UPS_SET = 200;
    private BufferedImage backgroundImg;
    public final static int TILES_DEFAULT_SIZE = 32;
    public final static float SCALE = 1.0f; // Change this to 3.0f or 4.0f to make the window massive!
    public final static int TILES_IN_WIDTH = 26;
    public final static int TILES_IN_HEIGHT = 14;
    public final static int TILES_SIZE = (int) (TILES_DEFAULT_SIZE * SCALE);

    // The final window dimensions are calculated automatically
    public final static int GAME_WIDTH = TILES_SIZE * TILES_IN_WIDTH;
    public final static int GAME_HEIGHT = TILES_SIZE * TILES_IN_HEIGHT;


    private Player player; // The Game class owns the player!

    public Game() {
        player = new Player();
        // Load your background image here!
        backgroundImg = LoadSave.GetSprite("/bg.png");
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
        player.update();
    }

    public void render(Graphics g) {
        // 1. Draw the background FIRST (so it sits in the back)
        if (backgroundImg != null) {
            // Draw it at x:0, y:0, and stretch it to fill the standard 800x600 window
            g.drawImage(backgroundImg, 0, 0, GAME_WIDTH, GAME_HEIGHT, null);
        }
        player.draw(g);
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