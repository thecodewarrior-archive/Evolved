package thecodewarrior.evolved.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import thecodewarrior.evolved.EvolvedGameAdapter;

public class DesktopLauncher {
	public static final int WIDTH = 1800;
	public static final int HEIGHT = 1200;
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = WIDTH;
		config.height = HEIGHT;
		new LwjglApplication(new EvolvedGameAdapter(WIDTH, HEIGHT), config);
	}
}
