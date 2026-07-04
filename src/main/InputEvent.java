package main;

public class InputEvent {
    public enum Button {
        UP, DOWN, LEFT, RIGHT, FORWARD, BACK, PUNCH, KICK, NONE
    }

    public Button button;
    public long frame;

    public InputEvent(Button button, long frame) {
        this.button = button;
        this.frame = frame;
    }
}