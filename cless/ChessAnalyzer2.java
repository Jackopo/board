package de.htw.ds.board.cless;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

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


		List<MovePrediction> alternatives = new ArrayList<>();
		final boolean whiteActive = board.isWhiteActive();
		final Set<RunnableFuture<List<MovePrediction>>> futures = new HashSet<>();

		final Collection<short[]> moves = board.getActiveMoves();
		for (final short[] move : moves) {

			final RunnableFuture<List<MovePrediction>> future = new FutureTask<>(doMultiThreading(move, alternatives,  board, depth));
			futures.add(future);
			EXECUTOR_SERVICE.execute(future);


		}

		for (final RunnableFuture<List<MovePrediction>> tempFuture : futures) {

			try {
				// CHOOSING to totally ignore thread interruption (see class comment!)
				// Note that interrupting a resynchronization is always a delicate affair, as the remainder
				// of the interrupted thread implicitly depends on the resynchronization to have taken place!
				while (true) {
					try {
						alternatives = tempFuture.get();
						break;
					} catch (final InterruptedException interrupt) {}
				}
			} catch (final ExecutionException exception) {
				final Throwable cause = exception.getCause();
				if (cause instanceof Error) throw (Error) cause;
				if (cause instanceof RuntimeException) throw (RuntimeException) cause;

				// Note that checked exception type InterruptedException can only occur here because of
				// call to Thread.sleep() in Callable!!! CHOOSING to interrupt parent thread after child
				// interruption and resynchronization (see class comment!)
				if (cause instanceof InterruptedException) throw new ThreadDeath();

				throw new AssertionError();
			}
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

	private static Callable<List<MovePrediction>> doMultiThreading(final short[] move, 
			final List<MovePrediction> alternatives, final Board<ChessPieceType> board,final int depth) {

		final boolean whiteActive = board.isWhiteActive();


		final Callable<List<MovePrediction>> callable = new Callable<List<MovePrediction>>() {
			public List<MovePrediction> call () throws InterruptedException {
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
						movePrediction = predictMovesSingleThreaded(boardClone, depth - 1);
						final short[] counterMove = movePrediction.getMoves().get(0);
						if (counterMove != null) {
							final Piece<ChessPieceType> recapturePiece = boardClone.getPiece(counterMove[1]);
							if (recapturePiece != null && recapturePiece.isWhite() == whiteActive && recapturePiece.getType() == ChessPieceType.KING) return alternatives;
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

				return alternatives;
			}
		};

		return callable;


	}


}
