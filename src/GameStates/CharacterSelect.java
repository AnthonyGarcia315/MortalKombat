package GameStates;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import main.Game;
import main.GameState;
import util.LoadSave;
import util.SoundManager;

public class CharacterSelect {
    private Game game;
    private BufferedImage[] portraits;
    private Rectangle[] slots;
    public int currentSelection = 0;

    // Two-phase select: first pick YOUR fighter, then pick the OPPONENT.
    // p1Locked stays -1 until phase 1 is confirmed with Enter.
    private int p1Locked = -1;
    private static final String[] ROSTER =
            {"johnnycage", "kano", "raiden", "liukang", "scorpion", "subzero", "sonya"};

    public CharacterSelect(Game game) {
        this.game = game;
        this.portraits = new BufferedImage[7];
        loadPortraits();
        initSlots();
    }

    private void loadPortraits() {
        String[] names = {"johnnycage", "kano", "raiden", "liukang", "scorpion", "subzero", "sonya"};
        for (int i = 0; i < names.length; i++) {
            portraits[i] = LoadSave.GetSprite("/" + names[i] + ".png"); // Ensure these exist in /res
        }
    }

    private void initSlots() {
        slots = new Rectangle[7];
        int bw = (int) (118 * Game.SCALE);
        int bh = (int) (95 * Game.SCALE);
        int gapX = (int) (22 * Game.SCALE);
        int startX = (int) (72 * Game.SCALE);
        int startY = (int) (75 * Game.SCALE);
        int gapY = (int) (15 * Game.SCALE);

        slots[0] = new Rectangle(startX, startY, bw, bh);
        slots[1] = new Rectangle(startX + bw + gapX, startY, bw, bh);
        slots[2] = new Rectangle(startX + 3 * (bw + gapX), startY, bw, bh);
        slots[3] = new Rectangle(startX + 4 * (bw + gapX), startY, bw, bh);
        int row2Y = startY + bh + gapY;
        slots[4] = new Rectangle(startX + bw + gapX, row2Y, bw, bh);
        slots[5] = new Rectangle(startX + 2 * (bw + gapX), row2Y, bw, bh);
        slots[6] = new Rectangle(startX + 3 * (bw + gapX), row2Y, bw, bh);
    }

    public void draw(Graphics g) {
        for (int i = 0; i < 7; i++) {
            if (portraits[i] != null) {
                g.drawImage(portraits[i], slots[i].x, slots[i].y, slots[i].width, slots[i].height, null);
            }
        }

        // If player 1's pick is already locked in, show it in green so it's
        // clear that slot is taken while you're now choosing the opponent.
        if (p1Locked != -1) {
            g.setColor(Color.GREEN);
            Rectangle locked = slots[p1Locked];
            g.drawRect(locked.x, locked.y, locked.width, locked.height);
        }

        g.setColor(Color.RED);
        Rectangle s = slots[currentSelection];
        g.drawRect(s.x, s.y, s.width, s.height);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        if (p1Locked == -1) {
            g.drawString("Player 1: choose your fighter", 20, 40);
        } else {
            g.drawString("Player 2: choose the opponent", 20, 40);
        }
    }

    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        // A/D or Left/Right arrows to cycle left/right
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) {
            currentSelection--;
            if (currentSelection < 0) currentSelection = 6; // Loop back to end
            SoundManager.play(SoundManager.Sound.MENU_MOVE);
        }
        else if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) {
            currentSelection++;
            if (currentSelection > 6) currentSelection = 0; // Loop back to start
            SoundManager.play(SoundManager.Sound.MENU_MOVE);
        }

        // Enter to lock in
        else if (key == KeyEvent.VK_ENTER) {
            SoundManager.play(SoundManager.Sound.MENU_CONFIRM);
            if (p1Locked == -1) {
                // Phase 1 confirmed: lock in the player's fighter, then let
                // them pick the opponent from the same grid.
                p1Locked = currentSelection;
            } else {
                // Phase 2 confirmed: start the match with both picks.
                game.startNewMatch(ROSTER[p1Locked], ROSTER[currentSelection]);
                p1Locked = -1; // reset so the select screen works again next time
                //GameState.state = GameState.PLAYING;
            }
        }
    }
}