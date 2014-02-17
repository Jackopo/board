package de.htw.ds.board.cless;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.LogManager;
import javax.swing.JComponent;
import javax.swing.JFrame;
import de.htw.ds.board.Analyzer;
import de.htw.ds.board.Board;
import de.htw.ds.board.GamePanel;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.Streams;


/**
 * Chess client class using on a Swing based BoardPanel. Note that this class is declared final
 * because it provides an application entry point, and is therefore not supposed to be extended by
 * subclassing.
 * @see <a href="http://www.chessville.com/downloads/misc_downloads.htm#ChessIcons4">The original
 *      source of the chess piece icons</a>
 */
@TypeMetadata(copyright = "2013-2014 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public final class ChessClient {

	private final JComponent contentPane;
	private final Map<ChessPieceType,Image> whitePieceImages;
	private final Map<ChessPieceType,Image> blackPieceImages;


	/**
	 * Creates a new instance.
	 * @param analyzer the analyzer used for move prediction
	 * @param board the board
	 * @param searchDepth the search depth in half moves
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given search depth is negative or odd
	 */
	public ChessClient (final Analyzer<ChessPieceType> analyzer, final Board<ChessPieceType> board, final int searchDepth) {
		super();

		this.whitePieceImages = defaultPieceImages(true);
		this.blackPieceImages = defaultPieceImages(false);
		this.contentPane = new GamePanel(board, this.whitePieceImages, this.blackPieceImages, analyzer, searchDepth);
	}


	/**
	 * Returns the content pane.
	 * @return the content pane
	 */
	public JComponent getContentPane () {
		return this.contentPane;
	}


	/**
	 * Returns the frame image.
	 * @return the frame image
	 */
	public Image getFrameImage () {
		return this.whitePieceImages.get(ChessPieceType.KING);
	}


	/**
	 * Returns the default mappings of chess piece types to their images.
	 * @param white whether or not the pieces are white
	 * @return a piece type to image map.
	 */
	static private Map<ChessPieceType,Image> defaultPieceImages (final boolean white) {
		final Map<String,byte[]> fileContents;
		try (InputStream fileSource = ChessClient.class.getResourceAsStream("chess-images.zip")) {
			fileContents = Streams.readAllAsZipEntries(fileSource);
		} catch (final IOException exception) {
			throw new ExceptionInInitializerError(exception);
		}

		final Map<ChessPieceType,Image> pieceImages = new HashMap<>();
		for (final ChessPieceType type : ChessPieceType.values()) {
			final String imageName = (white ? "white" : "black") + "-" + type.name().toLowerCase() + ".png";
			final Image image = Toolkit.getDefaultToolkit().createImage(fileContents.get(imageName));
			pieceImages.put(type, image);
		}

		return pieceImages;
	}


	/**
	 * Swing-GUI based client to play chess as WHITE player on a given board. Note that this method
	 * optionally expects a chess analyzer class name, a chess board class name, and an analyzer
	 * search depth as arguments. This must either be followed by one X-FEN board representation, or
	 * alternatively by a rank and file count. If no board representation is given, a default chess
	 * layout will be assumed.
	 * @param args the analyzer class name, the board class name, the search depth (4-6 are good
	 *        values to start with), and then either an X-FEN like board representation, or a row
	 *        count followed by a column count; all arguments are optional
	 * @throws IllegalArgumentException if any of the given class names is illegal, if the given
	 *         search depth is negative, if the given rank or file count is negative, or if the
	 *         given X-FEN board representation is invalid
	 * @throws IllegalStateException if there is no default layout for the given board dimensions
	 * @throws NumberFormatException if the given searchDepth, rank or file count is not a number
	 */
	static public void main (final String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		LogManager.getLogManager();

		final String analyzerClassName	= args.length < 1 ? ChessAnalyzer.class.getName() : args[0];
		final String boardClassName		= args.length < 2 ? ChessTableBoard.class.getName() : args[1];
		final int analyzerSearchDepth	= args.length < 3 ? 5 : Integer.parseInt(args[2]);
		final String[] boardArguments	= args.length < 4 ? new String[0] : Arrays.copyOfRange(args, 3, args.length);

		final Analyzer<ChessPieceType> analyzer = newAnalyzer(analyzerClassName);
		final Board<ChessPieceType> board = newBoard(boardClassName, boardArguments);

		final ChessClient client = new ChessClient(analyzer, board, analyzerSearchDepth);
		final JFrame frame = new JFrame("Distributed Chess");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(client.getContentPane());
		frame.setIconImage(client.getFrameImage());
		frame.pack();
		frame.setVisible(true);
	}


	/**
	 * Returns a board reflectively created using the given class name and arguments.
	 * @param boardClassName the board class name
	 * @return the board created
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given class name is illegal, or the class does not
	 *         feature an accessible constructor (either no-arg, or consuming two byte values)
	 */
	private static ChessBoard newBoard (final String boardClassName, final String... args) {
		try {
			final Class<?> boardClass = Class.forName(boardClassName, true, Thread.currentThread().getContextClassLoader());
			switch (args.length) {
				case 0: {
					final Constructor<?> constructor = boardClass.getConstructor(byte.class, byte.class);
					final ChessBoard board = (ChessBoard) constructor.newInstance((byte) 8, (byte) 8);
					board.reset(null);
					return board;
				}
				case 1: {
					return ChessXfenCodec.decode(ChessTableBoard.class, args[0]);
				}
				default: {
					final byte rankCount = Byte.parseByte(args[0]), fileCount = Byte.parseByte(args[1]);
					final Constructor<?> constructor = boardClass.getConstructor(byte.class, byte.class);
					final ChessBoard board = (ChessBoard) constructor.newInstance(rankCount, fileCount);
					board.reset(null);
					return board;
				}
			}
		} catch (final InvocationTargetException exception) {
			final Throwable cause = exception.getCause();
			if (cause instanceof Error) throw (Error) cause;
			if (cause instanceof RuntimeException) throw (RuntimeException) cause;
			throw new AssertionError();
		} catch (final ReflectiveOperationException exception) {
			throw new IllegalArgumentException(exception);
		}
	}


	/**
	 * Returns an analyzer reflectively created using the given class name.
	 * @param analyzerClassName the analyzer class name
	 * @return the analyzer created
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given class name is illegal, or the class does not
	 *         feature an accessible no-arg constructor
	 */
	private static ChessAnalyzer newAnalyzer (final String analyzerClassName) {
		try {
			final Class<?> analyzerClass = Class.forName(analyzerClassName, true, Thread.currentThread().getContextClassLoader());
			final Constructor<?> constructor = analyzerClass.getConstructor();
			return (ChessAnalyzer) constructor.newInstance();
		} catch (final InvocationTargetException exception) {
			final Throwable cause = exception.getCause();
			if (cause instanceof Error) throw (Error) cause;
			if (cause instanceof RuntimeException) throw (RuntimeException) cause;
			throw new AssertionError();
		} catch (final ReflectiveOperationException exception) {
			throw new IllegalArgumentException(exception);
		}
	}
}