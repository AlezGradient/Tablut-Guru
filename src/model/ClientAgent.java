package model;
import java.io.IOException;
import java.net.UnknownHostException;

import it.unibo.ai.didattica.competition.tablut.client.TablutClient;
import it.unibo.ai.didattica.competition.tablut.domain.Action;
import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.StateTablut;

public class ClientAgent extends TablutClient {
	boolean firstTurn;
	private IAgent model;
	
	public ClientAgent(String player, String name, int timeout, String ipAddress, int blackDepth) throws UnknownHostException, IOException {

		super(player, name, timeout, ipAddress);
		
		if(player.toLowerCase().equals("black")) model = new BlackAgent(blackDepth);
		else model = new WhiteAgent();
	}

	@Override
	public void run() {

		try {
			this.declareName();
		} catch (Exception e) {
			e.printStackTrace();
		}

		State state;
		while (true) {
			try {
				this.read();
			} catch (ClassNotFoundException | IOException e1) {
				break;
			}
			state = this.getCurrentState();


			Action a = null;

			if (state.getTurn().equals(this.getPlayer())) {
				
				
				try {
					a = model.getMove(state);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				
				try {
					this.write(a);
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}
			}
			
			else if (state.getTurn().equals(StateTablut.Turn.WHITEWIN)) {
				break;
			}
			else if (state.getTurn().equals(StateTablut.Turn.BLACKWIN)) {
				break;
			}
			else if (state.getTurn().equals(StateTablut.Turn.DRAW)) {
				break;
			} 
		}
	}

}
