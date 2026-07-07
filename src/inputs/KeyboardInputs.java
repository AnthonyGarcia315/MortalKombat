package inputs;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import main.GamePanel;
import main.GameState;
import main.InputEvent; // Make sure to import this!

public class KeyboardInputs implements KeyListener {

    private GamePanel gamePanel;

    public KeyboardInputs(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }

    @Override
    public void keyTyped(KeyEvent e) { }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A:
                gamePanel.getGame().getPlayer().setLeft(false);
                break;
            case KeyEvent.VK_D:
                gamePanel.getGame().getPlayer().setRight(false);
                break;
            case KeyEvent.VK_W:
                gamePanel.getGame().getPlayer().setUp(false);
                break;
            case KeyEvent.VK_S:
                gamePanel.getGame().getPlayer().setDown(false);
                break;
            case KeyEvent.VK_SHIFT:
                // If you hold shift to block, release it to stop blocking
                gamePanel.getGame().getPlayer().setBlocking(false);
                break;
            case KeyEvent.VK_ENTER:
                if (GameState.state == GameState.MENU) {
                    GameState.state = GameState.PLAYING;
                } else if (GameState.state == GameState.GAME_OVER) {
// Later, you will also need to reset health to 100 here!
                    GameState.state = GameState.MENU;
                }
                break;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A:
                gamePanel.getGame().getPlayer().setLeft(true);
                // NEW: Log 'A' to the combo buffer
                gamePanel.getGame().getPlayer().registerInput(InputEvent.Button.LEFT);
                break;

            case KeyEvent.VK_D:
                gamePanel.getGame().getPlayer().setRight(true);
                // NEW: Log 'D' to the combo buffer
                gamePanel.getGame().getPlayer().registerInput(InputEvent.Button.RIGHT);
                break;

            case KeyEvent.VK_W:
                gamePanel.getGame().getPlayer().setUp(true);
                // NEW: Log 'W' to the combo buffer
                gamePanel.getGame().getPlayer().registerInput(InputEvent.Button.UP);
                break;

            case KeyEvent.VK_S:
                gamePanel.getGame().getPlayer().setDown(true);
                // NEW: Log 'S' to the combo buffer
                gamePanel.getGame().getPlayer().registerInput(InputEvent.Button.DOWN);
                break;

            case KeyEvent.VK_SHIFT:
                gamePanel.getGame().getPlayer().setBlocking(true);
                break;

            // --- THE ATTACKS ---
            case KeyEvent.VK_SPACE:
                gamePanel.getGame().getPlayer().registerInput(InputEvent.Button.PUNCH);
                break;

            case KeyEvent.VK_ENTER:
                gamePanel.getGame().getPlayer().registerInput(InputEvent.Button.KICK);
                break;

            case KeyEvent.VK_TAB:
                gamePanel.getGame().getPlayer().registerInput(InputEvent.Button.THROW);
                break;
        }
    }
}