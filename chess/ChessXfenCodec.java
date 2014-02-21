package de.htw.ds.board.chess;

import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import de.htw.ds.board.XfenCodec;
import de.sb.javase.TypeMetadata;


/**
 * This facade provides encoding/decoding capabilities for XFEN chess board representations.
 */
@TypeMetadata(copyright = "2013-2014 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public final class ChessXfenCodec {
	static private final byte XFEN_SECTION_COUNT = 6;


	/**
	 * Prevents instantiation.
	 */
	private ChessXfenCodec () {}


	/**
	 * Returns a chess board for the given XFEN text representation.
	 * @param boardClass the board class
	 * @param xfen the XFEN text representation
	 * @return the chess board
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given text representation is syntactically malformed,
	 *         or would result in an illegal board state
	 */
	static public <T extends ChessBoard> T decode (final Class<T> boardClass, final String xfen) {
		final String[] xfenSections = xfen.split("\\s+");
		if (xfenSections.length != XFEN_SECTION_COUNT) throw new IllegalArgumentException();

		final char[][] pieceMatrix = XfenCodec.decodePieceMatrix(xfenSections[0]);
		final byte rankCount = (byte) pieceMatrix.length;
		final byte fileCount = (byte) pieceMatrix[0].length;
		if (rankCount < ChessBoard.MIN_RANK_COUNT | rankCount > Byte.MAX_VALUE | fileCount < ChessBoard.MIN_FILE_COUNT | fileCount > Byte.MAX_VALUE) throw new IllegalArgumentException();

		final boolean[] castlingAbilities = new boolean[4];
		if (!xfenSections[2].equals("-")) {
			for (final char alias : xfenSections[2].toCharArray()) {
				switch (alias) {
					case 'Q':
						castlingAbilities[0] = true;
						break;
					case 'K':
						castlingAbilities[1] = true;
						break;
					case 'q':
						castlingAbilities[2] = true;
						break;
					case 'k':
						castlingAbilities[3] = true;
						break;
					default:
						throw new IllegalArgumentException();
				}
			}
		}

		final short passingPawnPosition;
		if (xfenSections[3].equals("-")) {
			passingPawnPosition = -1;
		} else {
			try {
				final byte file = (byte) (Character.digit(xfenSections[3].charAt(0), Character.MAX_RADIX) - 10);
				final byte rank = (byte) (Byte.parseByte(xfenSections[3].substring(1)) - 1);
				passingPawnPosition = (short) (rank * fileCount + file);
			} catch (final NumberFormatException | IndexOutOfBoundsException exception) {
				throw new IllegalArgumentException();
			}
		}

		final boolean whiteActive = XfenCodec.decodeColor(xfenSections[1]);
		final byte reversibleMoveClock = XfenCodec.decodeReversibleMoveClock(xfenSections[4]);
		final short moveClock = XfenCodec.decodeMoveClock(xfenSections[5], whiteActive);

		try {
			final Constructor<T> constructor = boardClass.getConstructor(char[][].class, short.class, byte.class, boolean[].class, short.class);
			return constructor.newInstance(pieceMatrix, moveClock, reversibleMoveClock, castlingAbilities, passingPawnPosition);
		} catch (final InvocationTargetException exception) {
			final Throwable cause = exception.getCause();
			if (cause instanceof Error) throw (Error) cause;
			if (cause instanceof RuntimeException) throw (RuntimeException) cause;
			throw new AssertionError();
		} catch (final Exception exception) {
			throw new IllegalArgumentException();
		}
	}


	/**
	 * Returns an XFEN text representation for the given chess board.
	 * @param board the chess board
	 * @return the XFEN text representation
	 * @throws NullPointerException if the given chess board is {@code null}
	 */
	static public String encode (final ChessBoard board) {
		final StringWriter writer = new StringWriter();
		writer.write(XfenCodec.encodePieceMatrix(board.toCharacters()));
		writer.write(XfenCodec.encodeColor(board.isWhiteActive()));
		writer.write(' ');

		if ((board.getCastlingAbilities() & (ChessBoard.MASK_CASTLE_WHITE_LEFT | ChessBoard.MASK_CASTLE_WHITE_RIGHT | ChessBoard.MASK_CASTLE_BLACK_LEFT | ChessBoard.MASK_CASTLE_BLACK_RIGHT)) == 0) {
			writer.write('-');
		} else {
			if ((board.getCastlingAbilities() & ChessBoard.MASK_CASTLE_WHITE_RIGHT) != 0) writer.write('K');
			if ((board.getCastlingAbilities() & ChessBoard.MASK_CASTLE_WHITE_LEFT) != 0) writer.write('Q');
			if ((board.getCastlingAbilities() & ChessBoard.MASK_CASTLE_BLACK_RIGHT) != 0) writer.write('k');
			if ((board.getCastlingAbilities() & ChessBoard.MASK_CASTLE_BLACK_LEFT) != 0) writer.write('q');
		}
		writer.write(' ');

		if (board.getPassingPawnPosition() == -1) {
			writer.write('-');
		} else {
			writer.write(Character.forDigit(board.getPassingPawnPosition() % board.getFileCount() + 10, Character.MAX_RADIX));
			writer.write(Integer.toString(board.getPassingPawnPosition() / board.getFileCount() + 1));
		}
		writer.write(' ');

		writer.write(Integer.toString(board.getReversibleMoveClock()));
		writer.write(' ');

		writer.write(XfenCodec.encodeMoveClock(board.getMoveClock()));
		return writer.toString();
	}
}