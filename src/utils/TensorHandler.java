package utils;

import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.State.Pawn;
import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;

public class TensorHandler {
    
    // Template statico pre-calcolato per le caselle speciali (canale 4)
    private static final boolean[][][] SPECIAL_SQUARES_TEMPLATE = new boolean[9][9][6];

    static {
        // Inizializzazione eseguita una sola volta all'avvio della classe
        boolean[][][] t = SPECIAL_SQUARES_TEMPLATE;
        t[3][0][4] = true; t[4][0][4] = true; t[5][0][4] = true; t[4][1][4] = true;
        t[3][8][4] = true; t[4][8][4] = true; t[5][8][4] = true; t[4][7][4] = true;
        t[0][3][4] = true; t[0][4][4] = true; t[0][5][4] = true; t[1][4][4] = true;
        t[8][3][4] = true; t[8][4][4] = true; t[8][5][4] = true; t[7][4][4] = true;
        t[4][4][4] = true;
    }

    public TensorHandler() {}
    
    public boolean[][][] stateToTensor(State state) {
        // Controllo del turno ottimizzato con gli Enum
        Turn turn = state.getTurn();
        boolean isWhiteTurn = (turn == Turn.WHITE || turn.equalsTurn("WHITE"));

        // Allochiamo la nuova matrice
        boolean[][][] new_tensor = new boolean[9][9][6];
        
        // Copia rapida del template delle caselle speciali anziché ricalcolarlo ogni volta
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                new_tensor[i][j][4] = SPECIAL_SQUARES_TEMPLATE[i][j][4];
                new_tensor[i][j][5] = isWhiteTurn; // Assegnazione turno globale sul canale 5
            }
        }

        // Scansione della scacchiera con controlli diretti ed efficienti
        Pawn[][] board = state.getBoard();
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                Pawn pawn = board[i][j];
                
                // Usiamo i confronti diretti sugli Enum o sulle costanti note se possibile
                if (pawn == Pawn.WHITE) {
                    new_tensor[i][j][0] = true;
                } else if (pawn == Pawn.BLACK) {
                    new_tensor[i][j][1] = true;
                } else if (pawn == Pawn.EMPTY) {
                    new_tensor[i][j][2] = true;
                } else if (pawn == Pawn.KING) {
                    new_tensor[i][j][3] = true;
                } else {
                    // Fallback di sicurezza nel caso la libreria usi stringhe o caratteri legacy
                    if (pawn.equalsPawn("W")) new_tensor[i][j][0] = true;
                    else if (pawn.equalsPawn("B")) new_tensor[i][j][1] = true;
                    else if (pawn.equalsPawn("O") || pawn.equalsPawn("E")) new_tensor[i][j][2] = true;
                    else if (pawn.equalsPawn("K")) new_tensor[i][j][3] = true;
                }
            }
        }
        
        return new_tensor;
    }
}