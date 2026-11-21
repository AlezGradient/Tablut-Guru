package model;

import java.io.IOException;

public class Main {

	public static void main(String[] args) {
		try {
			ClientAgent player = new ClientAgent(args[0], "T-GURU", Integer.parseUnsignedInt(args[1]), args[2], Integer.parseUnsignedInt(args[3]));
			player.run();
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
	}

}
