package de.htw.ds.board.chess;

import de.htw.ds.board.Board;
import de.htw.ds.board.MovePrediction;

public class ChessAnalyzer3 extends ChessAnalyzer {

	public ChessAnalyzer3() {
		super();
	}
	
	public MovePrediction predictMoves (final Board<ChessPieceType> board, final int depth) {
		
		String xfen = board.toString();
		
		String[] xfenElements = xfen.split("\\s+");
		
		if (Integer.parseInt(xfenElements[5]) >= 20 ) {
			return this.predictMovesSingleThreaded(board, depth);
		} else {
			
			
			return null;
		}
	}

}
