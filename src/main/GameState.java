package main;

public enum GameState {
    MENU,
    PLAYING,
    CHARACTER_SELECT,
    GAME_OVER;

    // We start the game on the Menu screen!
    public static GameState state = MENU;
}