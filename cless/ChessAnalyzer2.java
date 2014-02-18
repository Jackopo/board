package de.htw.ds.board.cless;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import de.htw.ds.board.Board;
import de.htw.ds.board.MovePrediction;
import de.htw.ds.board.Piece;
import de.sb.javase.sync.DaemonThreadFactory;

public class ChessAnalyzer2 extends ChessAnalyzer {
	private static final int PROCESSOR_COUNT = Runtime.getRuntime().availableProcessors();
	private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(PROCESSOR_COUNT);
	
	public MovePrediction predictMoves (final Board<ChessPieceType> board, final int depth) {
		return predictMovesMultiThreaded(board, depth);
	}
	
	static protected final MovePrediction predictMovesMultiThreaded (final Board<ChessPieceType> board, final int depth) {
		if (depth <= 0) throw new IllegalArgumentException();

		final boolean whiteActive = board.isWhiteActive();
		final List<MovePrediction> alternatives = new ArrayList<>();

		final Collection<short[]> moves = board.getActiveMoves();
		for (final short[] move : moves) {

			final MovePrediction movePrediction;
			final Piece<ChessPieceType> capturePiece = board.getPiece(move[1]);
			if (capturePiece != null && capturePiece.isWhite() != whiteActive && capturePiece.getType() == ChessPieceType.KING) {
				movePrediction = new MovePrediction(whiteActive ? +Board.WIN : -Board.WIN);
				movePrediction.getMoves().add(move);
			} else {
				final Board<ChessPieceType> boardClone = board.clone();
				boardClone.performMove(move);

				if (depth > 1) {
					// perform single threaded recursive analysis, but filter move sequences resulting in own king's capture
					movePrediction = predictMovesMultiThreaded(boardClone, depth - 1);
					final short[] counterMove = movePrediction.getMoves().get(0);
					if (counterMove != null) {
						final Piece<ChessPieceType> recapturePiece = boardClone.getPiece(counterMove[1]);
						if (recapturePiece != null && recapturePiece.isWhite() == whiteActive && recapturePiece.getType() == ChessPieceType.KING) continue;
					}
					movePrediction.getMoves().add(0, move);
				} else {
					
					movePrediction = new MovePrediction(boardClone.getRating());
					movePrediction.getMoves().add(move);
				}
			}

			final int comparison = compareMovePredictions(whiteActive, movePrediction, alternatives.isEmpty() ? null : alternatives.get(0));
			if (comparison > 0) alternatives.clear();
			if (comparison >= 0) alternatives.add(movePrediction);
		}

		if (alternatives.isEmpty()) { // distinguish check mate and stale mate
			final short[] activeKingPositions = board.getPositions(true, whiteActive, ChessPieceType.KING);
			final boolean kingCheckedOrMissing = activeKingPositions.length == 0 ? true : board.isPositionThreatened(activeKingPositions[0]);
			final int rating = kingCheckedOrMissing ? (whiteActive ? -Board.WIN : +Board.WIN) : Board.DRAW;

			final MovePrediction movePrediction = new MovePrediction(rating);
			for (int loop = depth; loop > 0; --loop)
				movePrediction.getMoves().add(null);
			return movePrediction;
		}
		return alternatives.get(ThreadLocalRandom.current().nextInt(alternatives.size()));
	}
	

}
