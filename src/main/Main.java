package main;

import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) {
//        JFrame window = new JFrame();
//        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        window.setResizable(false);
//        window.setTitle("Java Fighting Game");

        // Just create the Game! It handles the rest.
        Game game = new Game();

        // Wait, we need to add the panel to the window!
        // In Kaarin's tutorial, he creates a separate GameWindow class,
        // but for now, we can just grab the panel from the Game class like this:
        // (If you want to make GameWindow.java later, we can!)

        // window.add(game.getGamePanel());
        // Let's actually adjust this so it matches perfectly.
    }
}