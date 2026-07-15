package main;

import util.LoadSave;
import util.SoundManager;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class Game implements Runnable {

    private GamePanel gamePanel;
    private Thread gameThread;
    private final int FPS_SET = 120;
    private final int UPS_SET = 150;
    private BufferedImage titleScreenImg;
    private BufferedImage vsLogoImg;
    private BufferedImage[] numberImgs; // Array to hold individual digits 0-9
    // All 7 stage backgrounds, bg0.png through bg6.png. One is picked at
    // random for each new match (see pickRandomBackground()/startNewMatch())
    // instead of always showing bg0.
    private BufferedImage[] backgroundImgs;
    private BufferedImage backgroundImg;
    private BufferedImage p1PortraitImg;
    private BufferedImage p2PortraitImg;

    // --- Match timer ---
    private final int MATCH_TIME_SECONDS = 99; // classic MK-style 99 second round
    private int matchTimeRemaining = MATCH_TIME_SECONDS;
    private int timerTickCounter = 0; // counts UPDATE ticks up to UPS_SET before docking a second
    private boolean timeExpired = false;

    // True from the instant someone's health hits 0 until the finishing
    // blow's own hit-stun animation has actually finished playing -- see
    // the KO check in update(). Keeps the winner's VICTORY pose / loser's
    // DEFEATED state from stomping the last hit before anyone sees it land.
    private boolean koPending = false;

    public final static int TILES_DEFAULT_SIZE = 32;
    public final static float SCALE = 1.0f; // Change this to 3.0f or 4.0f to make the window massive!
    public final static int TILES_IN_WIDTH = 26;
    public final static int TILES_IN_HEIGHT = 14;
    public final static int TILES_SIZE = (int) (TILES_DEFAULT_SIZE * SCALE);

    // The final window dimensions are calculated automatically
    public final static int GAME_WIDTH = TILES_SIZE * TILES_IN_WIDTH;
    public final static int GAME_HEIGHT = TILES_SIZE * TILES_IN_HEIGHT;
    private Projectile activeProjectile = null;
    private BufferedImage vsScreenBg;
    private int vsScreenTimer = 0; // Timer to automatically start the match


    private Player player;
    private Enemy enemy;
    private GameStates.CharacterSelect characterSelect;
    private BufferedImage charSelectImg;
    public Game() {
        player = new Player(150, 425, 68, 135, "subzero",this);
        // Enemy now takes a characterName the same way Player does — swap
        // "subzero" for any other character folder under /res once you have
        // assets for one (e.g. new Enemy(600, 425, 68, 145, "scorpion")).
        enemy = new Enemy(600, 425, 68, 145, "subzero",player,this);
        // Load every stage background, bg0.png..bg6.png, up front so picking
        // a new one for each match is just an array lookup, not a disk hit.
        backgroundImgs = new BufferedImage[7];
        for (int i = 0; i < backgroundImgs.length; i++) {
            backgroundImgs[i] = LoadSave.GetSprite("/bg" + i + ".png");
        }
        pickRandomBackground();
        titleScreenImg = LoadSave.GetSprite("/title.png");
        vsScreenBg = LoadSave.GetSprite("/vsScreen.png");
        charSelectImg = LoadSave.GetSprite("/Select_mk1.png"); // Load your select background
        characterSelect = new GameStates.CharacterSelect(this);
        // Load the VS Logo
        // vsLogoImg = LoadSave.GetSprite("/vs.gif");

        // Load and slice the number sprite sheet
        BufferedImage numSheet = LoadSave.GetSprite("/0.gif");
        if (numSheet != null) {
            numberImgs = new BufferedImage[10];
            // The sheet has 10 numbers in one row, so divide the total width by 10
            int digitWidth = numSheet.getWidth() / 10;
            int digitHeight = numSheet.getHeight();

            for (int i = 0; i < 10; i++) {
                numberImgs[i] = numSheet.getSubimage(i * digitWidth, 0, digitWidth, digitHeight);
            }
        }
        gamePanel = new GamePanel(this);

        // Create the window right here in the Game class
        javax.swing.JFrame window = new javax.swing.JFrame();
        window.setTitle("Mortal Kombat"); // Sets the text at the top of the window
        window.setIconImage(util.LoadSave.GetSprite("/logo_resized.jpg")); // Sets the taskbar/window logo
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
    public GameStates.CharacterSelect getCharacterSelect() {
        return characterSelect;
    }

    /**
     * Picks a random loaded stage background. Skips any index whose file
     * didn't exist on disk (LoadSave.GetSprite returns null instead of
     * crashing) so a missing bgN.png just narrows the pool instead of
     * putting a null image into rotation; falls back to bg0 -- or a plain
     * fill in render() -- if somehow nothing loaded at all.
     */
    private void pickRandomBackground() {
        java.util.List<BufferedImage> loaded = new java.util.ArrayList<>();
        for (BufferedImage img : backgroundImgs) {
            if (img != null) loaded.add(img);
        }
        if (loaded.isEmpty()) {
            backgroundImg = null;
            return;
        }
        backgroundImg = loaded.get((int) (Math.random() * loaded.size()));
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
    public void startNewMatch(String playerCharacterName, String enemyCharacterName) {
        player = new Player(150, 425, 68, 135, playerCharacterName, this);
        enemy = new Enemy(600, 425, 68, 145, enemyCharacterName, player, this);
        pickRandomBackground();
        matchTimeRemaining = MATCH_TIME_SECONDS;
        timerTickCounter = 0;
        timeExpired = false;
        koPending = false;
        p1PortraitImg = util.LoadSave.GetSprite("/" + playerCharacterName + ".png");
        p2PortraitImg = util.LoadSave.GetSprite("/" + enemyCharacterName + ".png");
        vsScreenTimer=0;
        GameState.state = GameState.VS_SCREEN;
    }
    public void update() {
        switch (GameState.state) {
            case MENU:
                // We will build menu update logic (like moving the mouse) here later
                break;
            case VS_SCREEN:
                // Count up to 3 seconds (UPS_SET * 3) before starting the match
                vsScreenTimer++;
                if (vsScreenTimer >= UPS_SET * 3) {
                    GameState.state = GameState.PLAYING;
                }
                break;
            case PLAYING:
                // --- MATCH TIMER ---
                // update() runs UPS_SET times per second (see run()), so
                // ticking a counter up to UPS_SET before docking one second
                // keeps the countdown accurate regardless of FPS/rendering.
                timerTickCounter++;
                if (timerTickCounter >= UPS_SET) {
                    timerTickCounter = 0;
                    matchTimeRemaining--;
                    if (matchTimeRemaining <= 0) {
                        matchTimeRemaining = 0;
                        timeExpired = true;
                        SoundManager.play(SoundManager.Sound.KO);
                        GameState.state = GameState.GAME_OVER;
                        break;
                    }
                }

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
                    } else if (!activeProjectile.isForming() && !activeProjectile.isHitting()
                            && activeProjectile.getHitbox().intersects(enemy.getHurtbox())) {
                        // Projectile hits! Freeze them and let the impact
                        // burst (frames 5-6) play out in place instead of
                        // the projectile just vanishing on contact.
                        enemy.getFrozen();
                        activeProjectile.hit();
                    }
                }

                // NOTE: player-vs-enemy hit resolution already happens above
                // via checkCombat(player, enemy) / checkCombat(enemy, player),
                // which reads each move's REAL damage and hit level out of
                // moveSet. There used to be a second, player-only collision
                // block here that re-checked the same hitbox/hurtbox overlap
                // and applied a hardcoded 5 damage regardless of the move
                // used. Because checkCombat() already flips attackActive to
                // false the instant it lands a hit, that second block could
                // only ever fire in the edge case where checkCombat() had
                // skipped resolution (e.g. defender.isHit was already true)
                // -- at which point it would apply the WRONG damage number
                // for whatever move was actually thrown. Removed; checkCombat
                // is now the single source of truth for combat resolution.

                // CHECK FOR KNOCKOUT!
                // Health hitting 0 just FLAGS the match as over -- it doesn't
                // cut to VICTORY/DEFEATED/GAME_OVER immediately. checkCombat()
                // (above) already put the loser into their real hit reaction
                // this tick (HIT_HIGH/HIT_LOW/HIT_CROUCH/THROWN via
                // takeDamage()/getThrown()) with isHit=true. Overwriting that
                // instantly used to mean the finishing blow never visibly
                // registered -- the fight just froze the instant health hit
                // 0. Now we wait for isHit to clear on BOTH fighters (i.e.
                // the hit-stun/knockback has actually finished, same as any
                // other hit) before locking in the victory pose and ending
                // the round, so the last hit always gets to land on screen.
                if (!koPending && (enemy.getCurrentHealth() <= 0 || player.getCurrentHealth() <= 0)) {
                    koPending = true;
                }
                if (koPending && !player.isHit && !enemy.isHit) {
                    SoundManager.play(SoundManager.Sound.KO);

                    if (player.getCurrentHealth() > 0) {
                        player.setCurrentState("VICTORY");
                        enemy.setCurrentState("DEFEATED");
                    } else if (enemy.getCurrentHealth() > 0) {
                        enemy.setCurrentState("VICTORY");
                        player.setCurrentState("DEFEATED");
                    } else {
                        player.setCurrentState("DEFEATED");
                        enemy.setCurrentState("DEFEATED");
                    }

                    GameState.state = GameState.GAME_OVER;
                    koPending = false; // reset so the next match's KO check starts clean
                }
                break;

            case GAME_OVER:
                // We MUST keep updating the characters here so their
                // Victory/Defeat animations actually play out frame-by-frame!
                player.update();
                enemy.update();

                // (Optional) If you have projectiles still flying when someone dies,
                // you can also update them here so they don't freeze mid-air:
                if (activeProjectile != null) {
                    activeProjectile.update();
                }
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
            case VS_SCREEN:
                if (vsScreenBg != null) {
                    g.drawImage(vsScreenBg, 0, 0, GAME_WIDTH, GAME_HEIGHT, null);
                } else {
                    // Fallback just in case the image doesn't load
                    g.setColor(java.awt.Color.BLACK);
                    g.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
                }
                int portraitWidth = 125;
                int portraitHeight = 120;
                int p1X = 235; // X position for Player 1's box
                int p2X = 465; // X position for Player 2's box
                int portraitY = 100; // Y position for both boxes

                // 2. Draw Player 1 Portrait
                if (p1PortraitImg != null) {
                    g.drawImage(p1PortraitImg, p1X, portraitY, portraitWidth, portraitHeight, null);
                }

                // 3. Draw Player 2 Portrait
                if (p2PortraitImg != null) {
                    // If you want P2 to face left, you can flip the image horizontally like this:
                    g.drawImage(p2PortraitImg, p2X + portraitWidth, portraitY, -portraitWidth, portraitHeight, null);

                    // Or if you don't need to flip it, just use standard drawing:
                    // g.drawImage(p2PortraitImg, p2X, portraitY, portraitWidth, portraitHeight, null);
                }
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
                if (timeExpired) {
                    g.drawString("TIME UP!", 350, 300);
                    g.setColor(java.awt.Color.WHITE);
                    String winner;
                    if (player.getCurrentHealth() > enemy.getCurrentHealth()) {
                        winner = "Player 1 wins!";
                    } else if (enemy.getCurrentHealth() > player.getCurrentHealth()) {
                        winner = "Player 2 wins!";
                    } else {
                        winner = "Draw!";
                    }
                    g.drawString(winner, 350, 330);
                } else {
                    // 1. Keep drawing the background
                    if (backgroundImg != null) {
                        g.drawImage(backgroundImg, 0, 0, GAME_WIDTH, GAME_HEIGHT, null);
                    }

                    // 2. Keep drawing the characters (so we see the Victory Pose)
                    player.draw(g);
                    enemy.draw(g);

                    // 3. Keep drawing the UI (Optional, you can remove this line if you want
                    // the health bars to vanish during the victory screen)
                    drawUI(g);

                    // 4. Draw your existing text on top of everything
                    g.setColor(java.awt.Color.RED);
                    if (timeExpired) {
                        g.drawString("TIME UP!", 350, 300);
                        g.setColor(java.awt.Color.WHITE);
                        String winner;
                        if (player.getCurrentHealth() > enemy.getCurrentHealth()) {
                            winner = "Player 1 wins!";
                        } else if (enemy.getCurrentHealth() > player.getCurrentHealth()) {
                            winner = "Player 2 wins!";
                        } else {
                            winner = "Draw!";
                        }
                        g.drawString(winner, 350, 330);
                    } else {
                        g.drawString("KNOCKOUT!", 350, 300);
                    }
                    break;
                }
                break;
            case CHARACTER_SELECT:
                if (charSelectImg != null) {
                    g.drawImage(charSelectImg, 0, 0, GAME_WIDTH, GAME_HEIGHT, null);
                }
                // Draw the cursor/portraits on top
                if (characterSelect != null) {
                    characterSelect.draw(g);
                }
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

        g.setColor(java.awt.Color.RED);
        g.fillRect(50, topPadding, barWidth, barHeight);
        g.setColor(java.awt.Color.GREEN);
        g.fillRect(50, topPadding, p1GreenWidth, barHeight);
        g.setColor(java.awt.Color.WHITE);
        g.drawRect(50, topPadding, barWidth, barHeight);

        // --- PLAYER 2 (Top Right) ---
        int p2StartX = GAME_WIDTH - 50 - barWidth;

        float p2HealthPercentage = (float) enemy.getCurrentHealth() / enemy.getMaxHealth();
        int p2GreenWidth = (int) (barWidth * p2HealthPercentage);

        g.setColor(java.awt.Color.RED);
        g.fillRect(p2StartX, topPadding, barWidth, barHeight);
        g.setColor(java.awt.Color.GREEN);
        int emptySpace = barWidth - p2GreenWidth;
        g.fillRect(p2StartX + emptySpace, topPadding, p2GreenWidth, barHeight);
        g.setColor(java.awt.Color.WHITE);
        g.drawRect(p2StartX, topPadding, barWidth, barHeight);

        // --- SPRITE-BASED MATCH TIMER & VS LOGO ---
        if (numberImgs != null && numberImgs[0] != null) {
            // Split the time into tens and ones (e.g., 99 becomes 9 and 9)
            int tens = matchTimeRemaining / 10;
            int ones = matchTimeRemaining % 10;

            // Optional: Scale up the sprites if they look too small on screen
            int spriteScale = 2;
            int digitW = numberImgs[0].getWidth() * spriteScale;
            int digitH = numberImgs[0].getHeight() * spriteScale;

            // Calculate center of the screen
            int centerX = GAME_WIDTH / 2;

            // Draw the Tens digit slightly left of center, Ones digit right of center
            int tensX = centerX - digitW;
            int onesX = centerX;
            int timerY = topPadding - 10; // Nudge timer slightly up to align with bars

            g.drawImage(numberImgs[tens], tensX, timerY, digitW, digitH, null);
            g.drawImage(numberImgs[ones], onesX, timerY, digitW, digitH, null);

            // Draw the VS Logo directly below the timer
            if (vsLogoImg != null) {
                int vsW = vsLogoImg.getWidth() * spriteScale;
                int vsH = vsLogoImg.getHeight() * spriteScale;
                int vsX = centerX - (vsW / 2);
                int vsY = timerY + digitH + 5; // 5 pixels below the timer numbers

                g.drawImage(vsLogoImg, vsX, vsY, vsW, vsH, null);
            }
        }
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