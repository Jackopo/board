package de.htw.ds.board.cless;

import java.util.Arrays;
import de.htw.ds.board.Board;
import de.htw.ds.board.MovePrediction;
import de.htw.ds.board.Analyzer;
import de.sb.javase.TypeMetadata;


/**
 * This class serves as a text-based test client for chess boards. It generates a chess board,
 * analyzes it, and performs one move for the active color, printing performance and result data in
 * the process. Note that this class is declared final because it provides an application entry
 * point, and is therefore not supposed to be extended by subclassing.
 */
@TypeMetadata(copyright = "2013-2014 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public final class ChessCommand {

	/**
	 * Console based client to test board analysis for the active player on a given board. Note that
	 * this method expects an analyzer search depth and an X-FEN board representation as arguments.
	 * @param args the search depth (4-6 are good values to start with), and an X-FEN board
	 *        representation
	 * @throws IllegalArgumentException if the given search depth is negative, or if the given X-FEN
	 *         representation is invalid
	 * @throws NumberFormatException if the given searchDepth is not a number
	 * @see ChessXfenCodec#decode(String,String)
	 */
	static public void main (final String[] args) {
		final int searchDepth = Integer.parseInt(args[0]);
		final String xfen = args[1];

		final Analyzer<ChessPieceType> analyzer = new ChessAnalyzer2b();
		final Board<ChessPieceType> board = ChessXfenCodec.decode(ChessTableBoard.class, xfen);

		System.out.println(board.toString());
		final long before = System.currentTimeMillis();
		final MovePrediction movePrediction = analyzer.predictMoves(board, searchDepth);
		final long after = System.currentTimeMillis();

		System.out.format("%s moves: %s\n", board.isWhiteActive() ? "White" : "Black", Arrays.toString(movePrediction.getMoves().get(0)));
		System.out.format("Predicted minimax move sequence: %s\n", Arrays.deepToString(movePrediction.getMoves().toArray()));
		System.out.format("Predicted board rating is %s.\n", movePrediction.getRating());
		System.out.format("Analysis time was %sms.\n", after - before);
	}
}