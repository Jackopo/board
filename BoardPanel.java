package de.htw.ds.board;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;
import de.sb.javase.Reflection;
import de.sb.javase.TypeMetadata;


/**
 * Swing based 2D board panel based on an abstract board.
 * @param <T> the type of the board's pieces
 */
@TypeMetadata(copyright = "2013-2014 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public class BoardPanel<T extends PieceType> extends JPanel {
	static private final long serialVersionUID = 1L;

	private final Map<T,Image> whitePieceImages;
	private final Map<T,Image> blackPieceImages;
	private final Analyzer<T> analyzer;
	private final Board<T> board;
	private final int searchDepth;
	private final Runnable moveAnimation;
	private volatile short sourcePosition;
	private volatile short[] sinkPositions;


	/**
	 * Creates a new instance.
	 * @param board the board to be visualized
	 * @param whitePieceImages the white piece images to be visualized
	 * @param blackPieceImages the black piece images to be visualized
	 * @param analyzer the board analyzer
	 * @param searchDepth the search depth in half moves
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given search depth is negative
	 */
	public BoardPanel (final Board<T> board, final Map<T,Image> whitePieceImages, final Map<T,Image> blackPieceImages, final Analyzer<T> analyzer, final int searchDepth) {
		super();
		if (board == null | whitePieceImages == null | blackPieceImages == null | analyzer == null) throw new NullPointerException();
		if (searchDepth <= 0) throw new IllegalArgumentException();

		this.analyzer = analyzer;
		this.board = board;
		this.searchDepth = searchDepth;
		this.whitePieceImages = whitePieceImages;
		this.blackPieceImages = blackPieceImages;

		this.sourcePosition = -1;
		this.sinkPositions = new short[0];
		this.moveAnimation = new Runnable() {
			public void run () {
				BoardPanel.this.setGlobalControlsEnabled(false);
				try {
					BoardPanel.this.performComputerMove();
				} finally {
					BoardPanel.this.setGlobalControlsEnabled(true);
				}
			}
		};

		final byte rankCount = board.getRankCount();
		final byte fileCount = board.getFileCount();
		this.setLayout(new GridLayout(rankCount, fileCount));
		for (int rank = rankCount - 1; rank >= 0; --rank) {
			for (int file = 0; file < fileCount; ++file) {
				final short position = (short) (rank * fileCount + file);

				final JButton button = new JButton();
				final ActionListener actionListener = new ActionListener() {
					public void actionPerformed (final ActionEvent event) {
						if (BoardPanel.this.performPlayerMove(position)) {
							new Thread(BoardPanel.this.moveAnimation).start();
						}
					}
				};
				button.addActionListener(actionListener);

				this.add(button);
				this.refreshButton(position, false);
			}
		}
	}


	/**
	 * Adds the given change event listener.
	 * @param listener the change event listener
	 */
	public void addChangeListener (final ChangeListener listener) {
		this.listenerList.add(ChangeListener.class, listener);
	}


	/**
	 * Removes the given change event listener.
	 * @param listener the change event listener
	 */
	public void removeChangeListener (final ChangeListener listener) {
		this.listenerList.remove(ChangeListener.class, listener);
	}


	/**
	 * {@inheritDoc}
	 */
	public void setControlsEnabled (final boolean enabled) {
		for (final Component component : BoardPanel.this.getComponents()) {
			component.setEnabled(enabled);
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public void refresh () {
		this.sourcePosition = -1;
		this.sinkPositions = new short[0];

		final byte rankCount = this.board.getRankCount();
		final byte fileCount = this.board.getFileCount();
		for (short boardSize = (short) (rankCount * fileCount), position = 0; position < boardSize; ++position) {
			this.refreshButton(position, false);
		}
	}


	/**
	 * Sets whether or not the controls of this component, and any of it's listeners, shall be
	 * enabled.
	 * @param enabled whether or not the controls shall be enabled
	 */
	private void setGlobalControlsEnabled (final boolean enabled) {
		this.setControlsEnabled(enabled);

		final PropertyChangeEvent event = new PropertyChangeEvent(this, "controlsEnabled", !enabled, enabled);
		for (final PropertyChangeListener listener : this.getListeners(PropertyChangeListener.class)) {
			listener.propertyChange(event);
		}
	}


	/**
	 * Refreshes the given button depending on the checker-board layout, the given marking, and the
	 * piece image to be displayed.
	 * @param position the field position
	 * @param marked whether or not the position is marked
	 * @throws IndexOutOfBoundsException if the given position is out of bounds
	 */
	private void refreshButton (final short position, final boolean marked) {
		final byte rankCount = this.board.getRankCount();
		final byte fileCount = this.board.getFileCount();
		final byte rank = (byte) (position / fileCount);
		final byte file = (byte) (position % fileCount);

		final boolean whiteField = ((rank ^ file) & 1) == 1;
		final Color color = whiteField ? (marked ? Color.LIGHT_GRAY : Color.WHITE) : (marked ? Color.DARK_GRAY : Color.BLACK);
		final Piece<?> piece = this.board.getPiece(position);
		final ImageIcon icon;
		if (piece == null) {
			icon = null;
		} else {
			final Map<T,Image> pieceImages = piece.isWhite() ? this.whitePieceImages : this.blackPieceImages;
			final Image pieceImage = pieceImages.get(piece.getType()).getScaledInstance(60, 60, Image.SCALE_SMOOTH);
			icon = new ImageIcon(pieceImage);
		}

		final JButton button = (JButton) this.getComponent((rankCount - rank - 1) * fileCount + file);
		button.setBackground(color);
		button.setIcon(icon);
		button.setDisabledIcon(icon);
	}


	/**
	 * Performs a move if the given position completes a valid move, or memorizes said position in
	 * case it does not. Returns {@code true} if a player move has been completed, {@code false}
	 * otherwise.
	 * @param position a board position
	 * @return whether or not a player move has been completed
	 * @throws IndexOutOfBoundsException if the given position is out of bounds
	 */
	private boolean performPlayerMove (final short position) {
		if (this.sourcePosition == -1) {
			final Piece<?> piece = this.board.getPiece(position);
			if (piece != null && piece.isWhite()) {
				this.refreshButton(position, true);
				this.sourcePosition = position;
			}
			return false;
		}

		final int previousPosition = this.sinkPositions.length == 0 ? this.sourcePosition : this.sinkPositions[this.sinkPositions.length - 1];
		if (position == previousPosition) {
			this.refresh();
			return false;
		}

		this.refreshButton(position, true);
		this.sinkPositions = Reflection.arrayInsert(this.sinkPositions, this.sinkPositions.length, position);
		final Piece<?> activePiece = this.board.getPiece(this.sourcePosition);
		if (activePiece == null) return false;

		final short[] move = Reflection.arrayInsert(this.sinkPositions, 0, this.sourcePosition);

		// check move validity, move piece, and add capture information!
		boolean moveValid = false;
		for (final short[] activeMove : this.board.getActiveMoves()) {
			if (Arrays.equals(activeMove, move)) {
				moveValid = true;
				break;
			}
		}
		if (!moveValid) return false;

		final short pieceCount = this.board.getPieceCount();
		this.board.performMove(move);
		this.refresh();
		Logger.getGlobal().log(Level.INFO, "Player moves {0}", Arrays.toString(move));

		final MovePrediction movePrediction = this.analyzer.predictMoves(this.board, 2);
		final int rating = movePrediction.getRating();
		final boolean gameOver = movePrediction.getMoveCount() == 0;
		final boolean capture = this.board.getPieceCount() < pieceCount;
		final MoveEvent event = new MoveEvent(this, activePiece.getType(), move, capture, gameOver, rating);
		for (final ChangeListener listener : this.getListeners(ChangeListener.class)) {
			listener.stateChanged(event);
		}
		return true;
	}


	/**
	 * Analyzes the board and performs the best minimax counter move for black.
	 */
	private void performComputerMove () {
		final long before = System.currentTimeMillis();
		final MovePrediction movePrediction = this.analyzer.predictMoves(this.board, this.searchDepth);
		final long after = System.currentTimeMillis();
		Logger.getGlobal().log(Level.INFO, "Prediction: {0} after {1} seconds.", new Object[] { movePrediction, new Double(0.001 * (after - before)) });

		final short[] move = movePrediction.getMoves().get(0);
		if (move != null) {
			final PieceType activeType = this.board.getPiece(move[0]).getType();

			final short pieceCount = this.board.getPieceCount();
			this.board.performMove(move);
			this.refresh();
			Logger.getGlobal().log(Level.INFO, "Computer moves {0}", Arrays.toString(move));

			final int rating = movePrediction.getRating();
			final boolean gameOver = movePrediction.getMoveCount() == 1;
			final boolean capture = this.board.getPieceCount() < pieceCount;
			final MoveEvent event = new MoveEvent(this, activeType, move, capture, gameOver, rating);
			for (final ChangeListener listener : this.getListeners(ChangeListener.class)) {
				listener.stateChanged(event);
			}
		}
	}
}