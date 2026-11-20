package utils;

import it.unibo.ai.didattica.competition.tablut.domain.Action;

public class Transition {
	
	private boolean[][][] tensor;
	private Action move;
	private double reward;
	
	public Transition(boolean[][][] tensor, Action move) {
		this.setTensor(tensor);
		this.setMove(move);
		reward = 0;
	}

	public boolean[][][] getTensor() {
		return tensor;
	}

	public void setTensor(boolean[][][] tensor) {
		this.tensor = tensor;
	}

	public Action getMove() {
		return move;
	}

	public void setMove(Action move) {
		this.move = move;
	}

	public double getReward() {
		return reward;
	}

	public void addReward(double reward) {
		this.reward += reward;
	}
	
	public void setReward(double r) {
		reward = r;
	}
	
	@Override
	public String toString() {
		return "FROM: " + move.getFrom() + " -> TO: " + move.getTo();
	}
}
