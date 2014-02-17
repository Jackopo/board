package de.htw.ds.board;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import de.sb.javase.TypeMetadata;


/**
 * Swing based control panel based on an abstract board.
 */
@TypeMetadata(copyright = "2013-2014 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public class ControlPanel extends JPanel {
	static private final long serialVersionUID = 1L;
	static private final String MESSAGE_WHITE_WINS = "White wins the game!";
	static private final String MESSAGE_BLACK_WINS = "Black wins the game!";
	static private final String MESSAGE_DRAW = "Game is a draw!";

	private final Board<? extends PieceType> board;
	private final Deque<String> moveHistory;
	private final Deque<String> boardHistory;

	private final JProgressBar ratingBar;
	private final JTextArea displayArea;
	private final AbstractButton backButton;
	private final AbstractButton resetButton;


	/**
	 * Public constructor.
	 * @param board the board to be visualized
	 * @throws NullPointerException if the given board is {@code null}
	 */
	public ControlPanel (final Board<? extends PieceType> board) {
		super();
		if (board == null) throw new NullPointerException();

		this.board = board;
		this.moveHistory = new ArrayDeque<String>();
		this.boardHistory = new ArrayDeque<String>();
		this.boardHistory.addLast(board.toString());

		this.ratingBar = new JProgressBar(JProgressBar.HORIZONTAL, -2000, 2000);
		this.displayArea = new JTextArea();

		{
			final Component versusLabel, ratingLabel, historyLabel, historyPane;
			final JTextField player1Field, player2Field;
			final SpringLayout layoutManager = new SpringLayout();
			this.setLayout(layoutManager);

			this.add(player1Field = new JTextField("You", 6));
			this.add(player2Field = new JTextField("Computer", 6));
			this.add(versusLabel = new JLabel("vs."));
			this.add(ratingLabel = new JLabel("Current rating"));
			this.add(this.ratingBar);
			this.add(historyLabel = new JLabel("Move History"));
			this.add(historyPane = new JScrollPane(this.displayArea));
			this.add(this.backButton = new JButton("Back"));
			this.add(this.resetButton = new JButton("Reset"));

			this.ratingBar.setForeground(Color.WHITE);
			this.ratingBar.setBackground(Color.BLACK);
			player2Field.setEditable(false);

			// same-faced alignment against parent
			for (final Component component : new Component[] { player1Field, player2Field }) {
				layoutManager.putConstraint(SpringLayout.NORTH, component, 0, SpringLayout.NORTH, this);
			}
			for (final Component component : new Component[] { this.backButton, this.resetButton }) {
				layoutManager.putConstraint(SpringLayout.SOUTH, component, 0, SpringLayout.SOUTH, this);
			}
			for (final Component component : new Component[] { player1Field, ratingLabel, historyLabel, historyPane, this.backButton }) {
				layoutManager.putConstraint(SpringLayout.WEST, component, 0, SpringLayout.WEST, this);
			}
			for (final Component component : new Component[] { this.ratingBar, historyPane, this.resetButton }) {
				layoutManager.putConstraint(SpringLayout.EAST, component, 0, SpringLayout.EAST, this);
			}
			layoutManager.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, player2Field);

			// same-faced alignment against sibling
			layoutManager.putConstraint(SpringLayout.VERTICAL_CENTER, versusLabel, 0, SpringLayout.VERTICAL_CENTER, player1Field);
			layoutManager.putConstraint(SpringLayout.VERTICAL_CENTER, ratingLabel, 0, SpringLayout.VERTICAL_CENTER, this.ratingBar);

			// opposite-faced alignment against sibling
			layoutManager.putConstraint(SpringLayout.NORTH, this.ratingBar, 5, SpringLayout.SOUTH, player1Field);
			layoutManager.putConstraint(SpringLayout.NORTH, historyLabel, 5, SpringLayout.SOUTH, this.ratingBar);
			layoutManager.putConstraint(SpringLayout.NORTH, historyPane, 5, SpringLayout.SOUTH, historyLabel);
			layoutManager.putConstraint(SpringLayout.SOUTH, historyPane, -5, SpringLayout.NORTH, this.backButton);
			layoutManager.putConstraint(SpringLayout.SOUTH, historyPane, -5, SpringLayout.NORTH, this.resetButton);
			layoutManager.putConstraint(SpringLayout.WEST, versusLabel, 15, SpringLayout.EAST, player1Field);
			layoutManager.putConstraint(SpringLayout.WEST, player2Field, 15, SpringLayout.EAST, versusLabel);
			layoutManager.putConstraint(SpringLayout.WEST, this.ratingBar, 5, SpringLayout.EAST, ratingLabel);
			layoutManager.putConstraint(SpringLayout.WEST, this.resetButton, 2, SpringLayout.HORIZONTAL_CENTER, this);
			layoutManager.putConstraint(SpringLayout.EAST, this.backButton, -3, SpringLayout.HORIZONTAL_CENTER, this);
		}

		final ActionListener backAction = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				ControlPanel.this.backButtonPressed();
			}
		};
		this.backButton.addActionListener(backAction);

		final ActionListener resetAction = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				ControlPanel.this.resetButtonPressed();
			}
		};
		this.resetButton.addActionListener(resetAction);

		this.refresh();
	}


	/**
	 * {@inheritDoc}
	 */
	public void setControlsEnabled (final boolean enabled) {
		this.backButton.setEnabled(enabled & this.boardHistory.size() >= 2);
		this.resetButton.setEnabled(enabled);
	}


	/**
	 * Logs the move history to this panel and the console.
	 * @param move the move performed
	 * @param capturing whether or not the move resulted in a capture
	 * @param activeType the type of the piece that moved
	 */
	public void movePerformed (final short[] move, final boolean capturing, final PieceType activeType) {
		this.moveHistory.addLast(displayText(move, capturing, activeType, this.board.getFileCount()));
		this.boardHistory.addLast(this.board.toString());
		this.refresh();
	}


	/**
	 * {@inheritDoc}
	 */
	public void gameOver (final int rating) {
		final String message;
		switch (rating) {
			case +Board.WIN:
				message = MESSAGE_WHITE_WINS;
				break;
			case Board.DRAW:
				message = MESSAGE_DRAW;
				break;
			case -Board.WIN:
				message = MESSAGE_BLACK_WINS;
				break;
			default:
				throw new IllegalArgumentException();
		}

		this.displayArea.setText(this.displayArea.getText() + "\n\n" + message);
		this.ratingBar.setValue(rating == Board.DRAW ? 0 : rating);
		this.ratingBar.setToolTipText(displayText(rating));

		Logger.getGlobal().log(Level.INFO, message);
	}


	/**
	 * Causes this panel to refresh.
	 */
	public void refresh () {
		final int rating = this.board.getRating();

		this.ratingBar.setValue(rating == Board.DRAW ? 0 : rating);
		this.ratingBar.setToolTipText(displayText(rating));
		this.displayArea.setText(displayText(this.moveHistory, this.board.getMoveClock() - this.moveHistory.size()));
		this.backButton.setEnabled(this.boardHistory.size() > 2);

		Logger.getGlobal().log(Level.INFO, "Board changed: \"{0}\"", this.board);
	}


	/**
	 * Handles the pressing of the back button.
	 */
	private void backButtonPressed () {
		if (this.board.isWhiteActive()) {
			this.moveHistory.removeLast();
			this.boardHistory.removeLast();
		}
		this.moveHistory.removeLast();
		this.boardHistory.removeLast();

		this.board.reset(this.boardHistory.peekLast());
		this.refresh();

		final ChangeEvent changeEvent = new ChangeEvent(this);
		for (final ChangeListener listener : this.listenerList.getListeners(ChangeListener.class)) {
			listener.stateChanged(changeEvent);
		}
	}


	/**
	 * Handles the pressing of the reset button.
	 */
	private void resetButtonPressed () {
		this.moveHistory.clear();
		this.boardHistory.clear();
		this.board.reset(null);
		this.boardHistory.addLast(this.board.toString());

		this.refresh();

		final ChangeEvent changeEvent = new ChangeEvent(this);
		for (final ChangeListener listener : this.listenerList.getListeners(ChangeListener.class)) {
			listener.stateChanged(changeEvent);
		}
	}


	/**
	 * Adds the given change listener to this component.
	 * @param listener the change listener
	 */
	public void addChangeListener (final ChangeListener listener) {
		this.listenerList.add(ChangeListener.class, listener);
	}


	/**
	 * Removes the given change listener from this component.
	 * @param listener the change listener
	 */
	public void removeChangeListener (final ChangeListener listener) {
		this.listenerList.remove(ChangeListener.class, listener);
	}


	/**
	 * Returns a text representation for the given move history.
	 * @param moveHistory the move history
	 * @param moveClockOffset the move clock offset
	 */
	static private String displayText (final Deque<String> moveHistory, final int moveClockOffset) {
		final StringWriter writer = new StringWriter();
		int moveClock = moveClockOffset;

		if ((moveClock & 1) == 1) writer.write(String.format("%3d. ... ", (moveClock >> 1) + 1));

		for (final String move : moveHistory) {
			if ((moveClock & 1) == 0) writer.write(String.format("%3d. ", (moveClock >> 1) + 1));
			writer.write(move);
			writer.write((moveClock & 1) == 0 ? " " : "\n");

			moveClock += 1;
		}

		return writer.toString();
	}


	/**
	 * Returns a text representation for the given move. Note that the default type is intentionally
	 * not represented.
	 * @param move the move
	 * @param capturing whether or not the move captured any pieces
	 * @param type the type that moved
	 * @param fileCount the number of files on a board
	 */
	static private String displayText (final short[] move, final boolean capturing, final PieceType activeType, final int fileCount) {
		final StringWriter writer = new StringWriter();

		if (activeType.ordinal() > 0) {
			writer.append(activeType.getAlias());
		}

		for (int positionIndex = 0; positionIndex < move.length; ++positionIndex) {
			final short position = move[positionIndex];
			final int rank = position / fileCount;
			final int file = position % fileCount;

			if (positionIndex > 0) writer.append(capturing ? 'x' : '-');
			writer.append((char) Character.forDigit(file + 10, Character.MAX_RADIX));
			writer.write(Integer.toString(rank + 1));
		}

		return writer.toString();
	}


	/**
	 * Returns a representation for the given rating. Usually that consists of the text equivalent
	 * of the given rating, but will deviate for the mix and max values to indicate the game has
	 * ended.
	 * @param rating the rating
	 * @return the text representation
	 */
	static private String displayText (final int rating) {
		final String text;
		switch (rating) {
			case +Board.WIN:
				text = MESSAGE_WHITE_WINS;
				break;
			case Board.DRAW:
				text = MESSAGE_DRAW;
				break;
			case -Board.WIN:
				text = MESSAGE_BLACK_WINS;
				break;
			default:
				text = String.format("%.2f", 0.01 * rating);
				break;
		}
		return text;
	}
}