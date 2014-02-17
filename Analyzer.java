package de.htw.ds.board;

/**
 * Classes implementing this interface serve as board analyzers. Any such class must be stateless in
 * the sense that it's operations must not change it's state, as a given analyzer instance may be
 * used to analyze multiple boards at the same time. Additionally, any subclass is required to
 * provide a public default constructor to allow instance creation using the Java reflection API.
 */
public interface Analyzer<T extends PieceType> {

	/**
	 * Recursively analyzes the valid moves and counter moves up until the given search depth
	 * beginning with the given board's active color, and implementing the minimax game theory
	 * principle. The result contains the next {@code depth} (half) moves predicted given optimum
	 * play from both sides, and the board rating after performing said moves.
	 * @param board the board
	 * @param depth the search depth in half moves
	 * @return the prediction for the next {@code depth} (half) moves, and the board rating after
	 *         performing said moves
	 * @throws NullPointerException if the given board is {@code null}
	 * @throws IllegalArgumentException if the given depth is negative
	 */
	MovePrediction predictMoves (Board<T> board, int depth);
}