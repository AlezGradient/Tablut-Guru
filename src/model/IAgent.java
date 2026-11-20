package model;

import it.unibo.ai.didattica.competition.tablut.domain.Action;
import it.unibo.ai.didattica.competition.tablut.domain.State;

public interface IAgent {
	public Action getMove(State state);
}
