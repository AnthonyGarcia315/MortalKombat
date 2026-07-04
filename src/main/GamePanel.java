package main;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import inputs.KeyboardInputs;

public class GamePanel extends JPanel {

    private Game game;

    public GamePanel(Game game) {
        this.game = game;

        setPreferredSize(new Dimension(Game.GAME_WIDTH, Game.GAME_HEIGHT));
        setBackground(Color.BLACK);

        addKeyListener(new KeyboardInputs(this));
        setFocusable(true);
        requestFocus();
    }

    public Game getGame() {
        return game;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Cast to Graphics2D
        java.awt.Graphics2D g2d = (java.awt.Graphics2D) g;

        // Calculate how much the user has stretched the window compared to your base width
        double scaleX = (double) getWidth() / Game.GAME_WIDTH;
        double scaleY = (double) getHeight() / Game.GAME_HEIGHT;

        // Scale the canvas before drawing anything
        g2d.scale(scaleX, scaleY);

        game.render(g2d);
    }
}