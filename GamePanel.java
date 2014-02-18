package de.htw.ds.board;

import java.awt.BorderLayout;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import de.sb.javase.TypeMetadata;


/**
 * Swing based 2D game panel based on a control panel and a board panel.
 */
@TypeMetadata(copyright = "2013-2014 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public class GamePanel extends JPanel {
	static private final long serialVersionUID = 1L;


	/**
	 * Creates a new instance.
	 * @param board the board to be visualized
	 * @param whitePieceImages the white piece images to be visualized
	 * @param blackPieceImages the black piece images to be visualized
	 * @param analyzer the board analyzer
	 * @param searchDepth the search depth in half moves
	 * @param <T> the piece type
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given search depth is negative
	 */
	public <T extends PieceType> GamePanel (final Board<T> board, final Map<T,Image> whitePieceImages, final Map<T,Image> blackPieceImages, final Analyzer<T> analyzer, final int searchDepth) {
		super();

		final ControlPanel controlPanel = new ControlPanel(board);
		final BoardPanel<T> boardPanel = new BoardPanel<>(board, whitePieceImages, blackPieceImages, analyzer, searchDepth);
		final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, controlPanel, boardPanel);

		this.setLayout(new BorderLayout());
		this.add(splitPane);

		splitPane.setDividerLocation(-1);
		splitPane.setBorder(new EmptyBorder(2, 2, 2, 2));
		((JComponent) splitPane.getTopComponent()).setBorder(new EmptyBorder(0, 0, 0, 2));
		((JComponent) splitPane.getBottomComponent()).setBorder(new EmptyBorder(0, 2, 0, 0));

		final ChangeListener changeListener = new ChangeListener() {
			public void stateChanged (final ChangeEvent event) {
				if (event.getSource() == controlPanel) {
					boardPanel.refresh();
				} else if (event.getSource() == boardPanel && (event instanceof MoveEvent)) {
					final MoveEvent moveEvent = (MoveEvent) event;
					controlPanel.movePerformed(moveEvent.getMove(), moveEvent.getCapture(), moveEvent.getType());
					if (moveEvent.getGameOver()) controlPanel.gameOver(moveEvent.getRating());
				}
			}
		};
		controlPanel.addChangeListener(changeListener);
		boardPanel.addChangeListener(changeListener);

		final PropertyChangeListener propertyListener = new PropertyChangeListener() {
			public void propertyChange (final PropertyChangeEvent event) {
				final Object value = event.getNewValue();
				if (event.getSource() == controlPanel) {
					if ("controlsEnabled".equals(event.getPropertyName())) {
						boardPanel.setControlsEnabled((Boolean) value);
					}
				}
			}
		};
		controlPanel.addPropertyChangeListener(propertyListener);
	}
}