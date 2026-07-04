package util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class LoadSave {

    // Define the file path string as a constant
    //public static final String PLAYER_ATLAS = "/subzero_spritesheet.png";
    public static final String SUBZERO_IDLE = "/subzero/idle/subzero_idle_";

    public static BufferedImage GetSpriteAtlas(String fileName) {
        BufferedImage img = null;

        // This grabs the file from your project's "res" (resources) folder
        InputStream is = LoadSave.class.getResourceAsStream(fileName);

        try {
            if (is == null) {
                System.err.println("Error: Could not find file at " + fileName);
            } else {
                img = ImageIO.read(is);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return img;
    }
    public static BufferedImage GetSprite(String filePath) {
        BufferedImage img = null;
        InputStream is = LoadSave.class.getResourceAsStream(filePath);
        try {
            if (is != null) {
                img = ImageIO.read(is);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return img;
    }
    public static BufferedImage[] GetSpriteSequence(String baseName, int frameCount) {
        BufferedImage[] arr = new BufferedImage[frameCount];

        for (int i = 0; i < frameCount; i++) {
            // This pieces together the file name: "/subzero_idle_" + "0" + ".png"
            String fileName = baseName + i + ".gif";
            InputStream is = LoadSave.class.getResourceAsStream(fileName);

            try {
                if (is == null) {
                    System.err.println("Error: Could not find file at " + fileName);
                } else {
                    arr[i] = ImageIO.read(is);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (is != null) is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return arr;
    }
}