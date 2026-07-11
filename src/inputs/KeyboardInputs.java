package inputs;

import main.GamePanel;
import main.GameState;
import main.InputEvent;
import util.SoundManager;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyboardInputs implements KeyListener {

    private GamePanel gamePanel;

    public KeyboardInputs(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Logic for releasing movement keys
        if (GameState.state == GameState.PLAYING) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_A: gamePanel.getGame().getPlayer().setLeft(false); break;
                case KeyEvent.VK_D: gamePanel.getGame().getPlayer().setRight(false); break;
                case KeyEvent.VK_W: gamePanel.getGame().getPlayer().setUp(false); break;
                case KeyEvent.VK_S: gamePanel.getGame().getPlayer().setDown(false); break;
                case KeyEvent.VK_SHIFT: gamePanel.getGame().getPlayer().setBlocking(false); break;
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (GameState.state) {
            case MENU:
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SoundManager.play(SoundManager.Sound.MENU_CONFIRM);
                    GameState.state = GameState.CHARACTER_SELECT;
                }
                break;

            case CHARACTER_SELECT:
                // Forward input to your new CharacterSelect class
                gamePanel.getGame().getCharacterSelect().keyPressed(e);
                break;

            case PLAYING:
                // Your existing combat input logic goes here
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_A:
                        gamePanel.getGame().getPlayer().setLeft(true);
                        gamePanel.getGame().getPlayer().registerInput(InputEvent.Button.LEFT);
                        break;
                    case KeyEvent.VK_D:
                        gamePanel.getGame().getPlayer().setRight(true);
                        gamePanel.getGame().getPlayer().registerInput(InputEvent.Button.RIGHT);
                        break;
                    case KeyEvent.VK_W:
                        // Jump
                        gamePanel.getGame().getPlayer().setUp(true);
                        break;
                    case KeyEvent.VK_S:
                        // Crouch (also feeds the DOWN input needed for ICE_BALL's
                        // DOWN, forward, PUNCH combo)
                        gamePanel.getGame().getPlayer().setDown(true);
                        gamePanel.getGame().getPlayer().registerInput(InputEvent.Button.DOWN);
                        break;
                    case KeyEvent.VK_SHIFT:
                        gamePanel.getGame().getPlayer().setBlocking(true);
                        break;
                    case KeyEvent.VK_SPACE:
                        // Punch
                        gamePanel.getGame().getPlayer().registerInput(InputEvent.Button.PUNCH);
                        break;
                    case KeyEvent.VK_K:
                        // Kick
                        gamePanel.getGame().getPlayer().registerInput(InputEvent.Button.KICK);
                        break;
                    case KeyEvent.VK_TAB:
                        // Throw
                        gamePanel.getGame().getPlayer().registerInput(InputEvent.Button.THROW);
                        break;
                }
                break;

            case GAME_OVER:
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SoundManager.play(SoundManager.Sound.MENU_CONFIRM);
                    GameState.state = GameState.MENU;
                }
                break;
        }
    }
}