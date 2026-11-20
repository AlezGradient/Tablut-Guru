package utils;

import java.util.ArrayList;
import java.util.List;

import it.unibo.ai.didattica.competition.tablut.domain.Action;
import it.unibo.ai.didattica.competition.tablut.domain.Game;
import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.StateTablut;
import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;
import it.unibo.ai.didattica.competition.tablut.exceptions.*;


// Refactor of GameAshtonTablut from: 
// it.unibo.ai.didattica.competition.tablut.domain.GameAshtonTablut

public class ReworkGameAshtonTablut implements Game {

	private int repeated_moves_allowed;
	private int cache_size;
	private int movesWithutCapturing;
	private List<String> citadels;
	// private List<String> strangeCitadels;
	private List<State> drawConditions;

	public ReworkGameAshtonTablut(int repeated_moves_allowed, int cache_size, String whiteName,
			String blackName) {
		this(new StateTablut(), repeated_moves_allowed, cache_size, whiteName, blackName);
	}

	public ReworkGameAshtonTablut(State state, int repeated_moves_allowed, int cache_size,
			String whiteName, String blackName) {
		super();
		this.repeated_moves_allowed = repeated_moves_allowed;
		this.cache_size = cache_size;
		this.movesWithutCapturing = 0;


		drawConditions = new ArrayList<State>();
		this.citadels = new ArrayList<String>();
		this.citadels.add("a4");
		this.citadels.add("a5");
		this.citadels.add("a6");
		this.citadels.add("b5");
		this.citadels.add("d1");
		this.citadels.add("e1");
		this.citadels.add("f1");
		this.citadels.add("e2");
		this.citadels.add("i4");
		this.citadels.add("i5");
		this.citadels.add("i6");
		this.citadels.add("h5");
		this.citadels.add("d9");
		this.citadels.add("e9");
		this.citadels.add("f9");
		this.citadels.add("e8");
	}

	@Override
	public State checkMove(State state, Action a)
			throws BoardException, ActionException, StopException, PawnException, DiagonalException, ClimbingException,
			ThroneException, OccupitedException, ClimbingCitadelException, CitadelException {
		// controllo la mossa
		if (a.getTo().length() != 2 || a.getFrom().length() != 2) {
			throw new ActionException(a);
		}
		int columnFrom = a.getColumnFrom();
		int columnTo = a.getColumnTo();
		int rowFrom = a.getRowFrom();
		int rowTo = a.getRowTo();

		// controllo se sono fuori dal tabellone
		if (columnFrom > state.getBoard().length - 1 || rowFrom > state.getBoard().length - 1
				|| rowTo > state.getBoard().length - 1 || columnTo > state.getBoard().length - 1 || columnFrom < 0
				|| rowFrom < 0 || rowTo < 0 || columnTo < 0) {
			throw new BoardException(a);
		}

		// controllo che non vada sul trono
		if (state.getPawn(rowTo, columnTo).equalsPawn(State.Pawn.THRONE.toString())) {
			throw new ThroneException(a);
		}

		// controllo la casella di arrivo
		if (!state.getPawn(rowTo, columnTo).equalsPawn(State.Pawn.EMPTY.toString())) {
			throw new OccupitedException(a);
		}
		if (this.citadels.contains(state.getBox(rowTo, columnTo))
				&& !this.citadels.contains(state.getBox(rowFrom, columnFrom))) {
			throw new CitadelException(a);
		}
		if (this.citadels.contains(state.getBox(rowTo, columnTo))
				&& this.citadels.contains(state.getBox(rowFrom, columnFrom))) {
			if (rowFrom == rowTo) {
				if (columnFrom - columnTo > 5 || columnFrom - columnTo < -5) {
					throw new CitadelException(a);
				}
			} else {
				if (rowFrom - rowTo > 5 || rowFrom - rowTo < -5) {
					throw new CitadelException(a);
				}
			}

		}

		// controllo se cerco di stare fermo
		if (rowFrom == rowTo && columnFrom == columnTo) {
			throw new StopException(a);
		}

		// controllo se sto muovendo una pedina giusta
		if (state.getTurn().equalsTurn(State.Turn.WHITE.toString())) {
			if (!state.getPawn(rowFrom, columnFrom).equalsPawn("W")
					&& !state.getPawn(rowFrom, columnFrom).equalsPawn("K")) {
				throw new PawnException(a);
			}
		}
		if (state.getTurn().equalsTurn(State.Turn.BLACK.toString())) {
			if (!state.getPawn(rowFrom, columnFrom).equalsPawn("B")) {
				throw new PawnException(a);
			}
		}

		// controllo di non muovere in diagonale
		if (rowFrom != rowTo && columnFrom != columnTo) {
			throw new DiagonalException(a);
		}

		// controllo di non scavalcare pedine
		if (rowFrom == rowTo) {
			if (columnFrom > columnTo) {
				for (int i = columnTo; i < columnFrom; i++) {
					if (!state.getPawn(rowFrom, i).equalsPawn(State.Pawn.EMPTY.toString())) {
						if (state.getPawn(rowFrom, i).equalsPawn(State.Pawn.THRONE.toString())) {
							throw new ClimbingException(a);
						} else {
							throw new ClimbingException(a);
						}
					}
					if (this.citadels.contains(state.getBox(rowFrom, i))
							&& !this.citadels.contains(state.getBox(a.getRowFrom(), a.getColumnFrom()))) {
						throw new ClimbingCitadelException(a);
					}
				}
			} else {
				for (int i = columnFrom + 1; i <= columnTo; i++) {
					if (!state.getPawn(rowFrom, i).equalsPawn(State.Pawn.EMPTY.toString())) {
						if (state.getPawn(rowFrom, i).equalsPawn(State.Pawn.THRONE.toString())) {
							throw new ClimbingException(a);
						} else {
							throw new ClimbingException(a);
						}
					}
					if (this.citadels.contains(state.getBox(rowFrom, i))
							&& !this.citadels.contains(state.getBox(a.getRowFrom(), a.getColumnFrom()))) {
						throw new ClimbingCitadelException(a);
					}
				}
			}
		} else {
			if (rowFrom > rowTo) {
				for (int i = rowTo; i < rowFrom; i++) {
					if (!state.getPawn(i, columnFrom).equalsPawn(State.Pawn.EMPTY.toString())) {
						if (state.getPawn(i, columnFrom).equalsPawn(State.Pawn.THRONE.toString())) {
							throw new ClimbingException(a);
						} else {
							throw new ClimbingException(a);
						}
					}
					if (this.citadels.contains(state.getBox(i, columnFrom))
							&& !this.citadels.contains(state.getBox(a.getRowFrom(), a.getColumnFrom()))) {
						throw new ClimbingCitadelException(a);
					}
				}
			} else {
				for (int i = rowFrom + 1; i <= rowTo; i++) {
					if (!state.getPawn(i, columnFrom).equalsPawn(State.Pawn.EMPTY.toString())) {
						if (state.getPawn(i, columnFrom).equalsPawn(State.Pawn.THRONE.toString())) {
							throw new ClimbingException(a);
						} else {
							throw new ClimbingException(a);
						}
					}
					if (this.citadels.contains(state.getBox(i, columnFrom))
							&& !this.citadels.contains(state.getBox(a.getRowFrom(), a.getColumnFrom()))) {
						throw new ClimbingCitadelException(a);
					}
				}
			}
		}

		return state;
	}

	private State checkCaptureWhite(State state, Action a) {
		// controllo se mangio a destra
		if (a.getColumnTo() < state.getBoard().length - 2
				&& state.getPawn(a.getRowTo(), a.getColumnTo() + 1).equalsPawn("B")
				&& (state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("W")
						|| state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("T")
						|| state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("K")
						|| (this.citadels.contains(state.getBox(a.getRowTo(), a.getColumnTo() + 2))
								&& !(a.getColumnTo() + 2 == 8 && a.getRowTo() == 4)
								&& !(a.getColumnTo() + 2 == 4 && a.getRowTo() == 0)
								&& !(a.getColumnTo() + 2 == 4 && a.getRowTo() == 8)
								&& !(a.getColumnTo() + 2 == 0 && a.getRowTo() == 4)))) {
			state.removePawn(a.getRowTo(), a.getColumnTo() + 1);
			this.movesWithutCapturing = -1;
		}
		// controllo se mangio a sinistra
		if (a.getColumnTo() > 1 && state.getPawn(a.getRowTo(), a.getColumnTo() - 1).equalsPawn("B")
				&& (state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("W")
						|| state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("T")
						|| state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("K")
						|| (this.citadels.contains(state.getBox(a.getRowTo(), a.getColumnTo() - 2))
								&& !(a.getColumnTo() - 2 == 8 && a.getRowTo() == 4)
								&& !(a.getColumnTo() - 2 == 4 && a.getRowTo() == 0)
								&& !(a.getColumnTo() - 2 == 4 && a.getRowTo() == 8)
								&& !(a.getColumnTo() - 2 == 0 && a.getRowTo() == 4)))) {
			state.removePawn(a.getRowTo(), a.getColumnTo() - 1);
			this.movesWithutCapturing = -1;
		}
		// controllo se mangio sopra
		if (a.getRowTo() > 1 && state.getPawn(a.getRowTo() - 1, a.getColumnTo()).equalsPawn("B")
				&& (state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("W")
						|| state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("T")
						|| state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("K")
						|| (this.citadels.contains(state.getBox(a.getRowTo() - 2, a.getColumnTo()))
								&& !(a.getColumnTo() == 8 && a.getRowTo() - 2 == 4)
								&& !(a.getColumnTo() == 4 && a.getRowTo() - 2 == 0)
								&& !(a.getColumnTo() == 4 && a.getRowTo() - 2 == 8)
								&& !(a.getColumnTo() == 0 && a.getRowTo() - 2 == 4)))) {
			state.removePawn(a.getRowTo() - 1, a.getColumnTo());
			this.movesWithutCapturing = -1;
		}
		// controllo se mangio sotto
		if (a.getRowTo() < state.getBoard().length - 2
				&& state.getPawn(a.getRowTo() + 1, a.getColumnTo()).equalsPawn("B")
				&& (state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("W")
						|| state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("T")
						|| state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("K")
						|| (this.citadels.contains(state.getBox(a.getRowTo() + 2, a.getColumnTo()))
								&& !(a.getColumnTo() == 8 && a.getRowTo() + 2 == 4)
								&& !(a.getColumnTo() == 4 && a.getRowTo() + 2 == 0)
								&& !(a.getColumnTo() == 4 && a.getRowTo() + 2 == 8)
								&& !(a.getColumnTo() == 0 && a.getRowTo() + 2 == 4)))) {
			state.removePawn(a.getRowTo() + 1, a.getColumnTo());
			this.movesWithutCapturing = -1;
		}
		// controllo se ho vinto
		if (a.getRowTo() == 0 || a.getRowTo() == state.getBoard().length - 1 || a.getColumnTo() == 0
				|| a.getColumnTo() == state.getBoard().length - 1) {
			if (state.getPawn(a.getRowTo(), a.getColumnTo()).equalsPawn("K")) {
				state.setTurn(State.Turn.WHITEWIN);
			}
		}

		this.movesWithutCapturing++;
		return state;
	}

	/**
	 * Nuovo metodo unificato: il Nero vince se e solo se il re è completamente 
	 * circondato o bloccato su tutti e 4 i lati ortogonali da pedine nere (B), 
	 * caselle speciali/avamposti (citadels) o il trono.
	 */
	private State checkKingCaptureGeneral(State state) {
		int kingRow = -1;
		int kingCol = -1;
		int boardSize = state.getBoard().length;

		// Trova la posizione del re ("K")
		for (int r = 0; r < boardSize; r++) {
			for (int c = 0; c < boardSize; c++) {
				if (state.getPawn(r, c).equalsPawn("K")) {
					kingRow = r;
					kingCol = c;
					break;
				}
			}
			if (kingRow != -1) {
				break;
			}
		}

		// Se per qualche motivo il re non viene trovato
		if (kingRow == -1) {
			return state;
		}

		// Direzioni ortogonali: Nord, Sud, Ovest, Est
		int[] dRow = {-1, 1, 0, 0};
		int[] dCol = {0, 0, -1, 1};

		boolean allBlocked = true;

		for (int i = 0; i < 4; i++) {
			int nr = kingRow + dRow[i];
			int nc = kingCol + dCol[i];

			// Se il re si trova sul bordo esterno e la casella esce dal tabellone
			if (nr < 0 || nr >= boardSize || nc < 0 || nc >= boardSize) {
				allBlocked = false;
				break;
			}

			String box = state.getBox(nr, nc);
			boolean isBlack = state.getPawn(nr, nc).equalsPawn("B");
			boolean isCitadel = this.citadels.contains(box);
			boolean isThrone = box.equals("e5") || state.getPawn(nr, nc).equalsPawn("THRONE");

			// Se anche una sola direzione non è occupata da un blocco valido, il re non è catturato
			if (!isBlack && !isCitadel && !isThrone) {
				allBlocked = false;
				break;
			}
		}

		if (allBlocked) {
			state.setTurn(State.Turn.BLACKWIN);
		}

		return state;
	}

	private State checkCaptureBlackPawnRight(State state, Action a) {
		// mangio a destra
		if (a.getColumnTo() < state.getBoard().length - 2
				&& state.getPawn(a.getRowTo(), a.getColumnTo() + 1).equalsPawn("W")) {
			if (state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("B")) {
				state.removePawn(a.getRowTo(), a.getColumnTo() + 1);
				this.movesWithutCapturing = -1;
			}
			if (state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("T")) {
				state.removePawn(a.getRowTo(), a.getColumnTo() + 1);
				this.movesWithutCapturing = -1;
			}
			if (this.citadels.contains(state.getBox(a.getRowTo(), a.getColumnTo() + 2))) {
				state.removePawn(a.getRowTo(), a.getColumnTo() + 1);
				this.movesWithutCapturing = -1;
			}
			if (state.getBox(a.getRowTo(), a.getColumnTo() + 2).equals("e5")) {
				state.removePawn(a.getRowTo(), a.getColumnTo() + 1);
				this.movesWithutCapturing = -1;
			}
		}
		return state;
	}

	private State checkCaptureBlackPawnLeft(State state, Action a) {
		// mangio a sinistra
		if (a.getColumnTo() > 1 && state.getPawn(a.getRowTo(), a.getColumnTo() - 1).equalsPawn("W")
				&& (state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("B")
						|| state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("T")
						|| this.citadels.contains(state.getBox(a.getRowTo(), a.getColumnTo() - 2))
						|| (state.getBox(a.getRowTo(), a.getColumnTo() - 2).equals("e5")))) {
			state.removePawn(a.getRowTo(), a.getColumnTo() - 1);
			this.movesWithutCapturing = -1;
		}
		return state;
	}

	private State checkCaptureBlackPawnUp(State state, Action a) {
		// controllo se mangio sopra
		if (a.getRowTo() > 1 && state.getPawn(a.getRowTo() - 1, a.getColumnTo()).equalsPawn("W")
				&& (state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("B")
						|| state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("T")
						|| this.citadels.contains(state.getBox(a.getRowTo() - 2, a.getColumnTo()))
						|| (state.getBox(a.getRowTo() - 2, a.getColumnTo()).equals("e5")))) {
			state.removePawn(a.getRowTo() - 1, a.getColumnTo());
			this.movesWithutCapturing = -1;
		}
		return state;
	}

	private State checkCaptureBlackPawnDown(State state, Action a) {
		// controllo se mangio sotto
		if (a.getRowTo() < state.getBoard().length - 2
				&& state.getPawn(a.getRowTo() + 1, a.getColumnTo()).equalsPawn("W")
				&& (state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("B")
						|| state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("T")
						|| this.citadels.contains(state.getBox(a.getRowTo() + 2, a.getColumnTo()))
						|| (state.getBox(a.getRowTo() + 2, a.getColumnTo()).equals("e5")))) {
			state.removePawn(a.getRowTo() + 1, a.getColumnTo());
			this.movesWithutCapturing = -1;
		}
		return state;
	}

	private State checkCaptureBlack(State state, Action a) {
		this.checkCaptureBlackPawnRight(state, a);
		this.checkCaptureBlackPawnLeft(state, a);
		this.checkCaptureBlackPawnUp(state, a);
		this.checkCaptureBlackPawnDown(state, a);
		
		// Sostituito il controllo a tenaglia locale con il controllo globale e rigoroso del re circondato
		this.checkKingCaptureGeneral(state);

		this.movesWithutCapturing++;
		return state;
	}

	public State movePawn(State state, Action a) {
	    boolean isWhiteMove = state.getTurn().equals(State.Turn.WHITE);
	    
	    State.Pawn pawn = state.getPawn(a.getRowFrom(), a.getColumnFrom());
	    State.Pawn[][] newBoard = state.getBoard();
	    

	    
	    // Free the throne or regular square
	    if (a.getColumnFrom() == 4 && a.getRowFrom() == 4) {
	        newBoard[a.getRowFrom()][a.getColumnFrom()] = State.Pawn.THRONE;
	    } else {
	        newBoard[a.getRowFrom()][a.getColumnFrom()] = State.Pawn.EMPTY;
	    }

	    // Place the pawn on the new square
	    newBoard[a.getRowTo()][a.getColumnTo()] = pawn;
	    state.setBoard(newBoard);

	    // TRIGGER CAPTURES & WIN CONDITIONS BASED ON WHO MOVED
	    if (isWhiteMove) {
	        state = checkCaptureWhite(state, a);
	    } else {
	        state = checkCaptureBlack(state, a);
	    }

	    // Change turn only if the game hasn't been won yet
	    if (!state.getTurn().equals(State.Turn.WHITEWIN) && !state.getTurn().equals(State.Turn.BLACKWIN)) {
	        if (isWhiteMove) {
	            state.setTurn(State.Turn.BLACK);
	        } else {
	            state.setTurn(State.Turn.WHITE);
	        }
	    }

	    return state;
	}

	public int getMovesWithutCapturing() {
		return movesWithutCapturing;
	}

	@SuppressWarnings("unused")
	private void setMovesWithutCapturing(int movesWithutCapturing) {
		this.movesWithutCapturing = movesWithutCapturing;
	}

	public int getRepeated_moves_allowed() {
		return repeated_moves_allowed;
	}

	public int getCache_size() {
		return cache_size;
	}

	public List<State> getDrawConditions() {
		return drawConditions;
	}

	public void clearDrawConditions() {
		drawConditions.clear();
	}

	@Override
	public void endGame(State state) {
		// TODO Auto-generated method stub
		
	}
	
	public ArrayList<Action> getActions(State state, boolean[][][] tensor) {
        ArrayList<Action> actions = new ArrayList<>();
        boolean isWhiteTurn = state.getTurn().equalsTurn("WHITE") || state.getTurn().equals(Turn.WHITE);
        int boardSize = state.getBoard().length;

        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                State.Pawn currentPawn = state.getPawn(r, c);
                
                boolean isOurPawn = isWhiteTurn ? 
                    (currentPawn.equalsPawn("W") || currentPawn.equalsPawn("K")) : 
                    currentPawn.equalsPawn("B");

                if (isOurPawn) {
                    String pawnBox = state.getBox(r, c);
                    int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
                    
                    for (int[] dir : directions) {
                        int nextR = r + dir[0];
                        int nextC = c + dir[1];
                        
                        while (nextR >= 0 && nextR < boardSize && nextC >= 0 && nextC < boardSize) {
                            if (tensor[nextR][nextC][2]) {
                                try {
                                    Action a = new Action(pawnBox, state.getBox(nextR, nextC), state.getTurn());
                                    this.checkMove(state, a);
                                    actions.add(a);
                                } catch (Exception e) {}
                            } else {
                                break;
                            }
                            nextR += dir[0];
                            nextC += dir[1];
                        }
                    }
                }
            }
        }
        return actions;
    }
	

}