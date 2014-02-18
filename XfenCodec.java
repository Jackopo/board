package de.htw.ds.board;

import java.io.StringWriter;
import de.sb.javase.TypeMetadata;


/**
 * This facade provides basic algorithms to encode and decode X-FEN style single-line text
 * representations of boards. Note that this class is declared final because it is a facade, and
 * therefore not supposed to be extended by subclassing.
 */
@TypeMetadata(copyright = "2013-2014 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public final class XfenCodec {

	/**
	 * Prevent instantiation.
	 */
	private XfenCodec () {}


	/**
	 * Returns a character based and game neutral piece matrix decoded from the given X-FEN piece
	 * section. Note that the ranks of the resulting piece matrix are ordered bottom to top, while
	 * X-FEN specifies the opposite!
	 * @param pieceSection the X-FEN piece section
	 * @return the piece matrix in natural rank order
	 * @throws NullPointerException if the given section is {@code null}
	 */
	static public char[][] decodePieceMatrix (final String pieceSection) {
		final String[] rankSections = pieceSection.split("/");
		if (rankSections.length == 0) return new char[0][0];

		final int fileCount = XfenCodec.decodeFileCount(rankSections[0]);
		final char[][] pieceMatrix = new char[rankSections.length][fileCount];
		if (fileCount == 0) return pieceMatrix;

		try {
			for (int spaceCount = 0, fileIndex = 0, rankIndex = 0; rankIndex < pieceMatrix.length; ++rankIndex, spaceCount = 0, fileIndex = 0) {
				final String rowSection = rankSections[rankSections.length - rankIndex - 1];
				for (final char pieceCharacter : rowSection.toCharArray()) {
					if (Character.isDigit(pieceCharacter)) {
						spaceCount = 10 * spaceCount + Character.digit(pieceCharacter, 10);
					} else {
						fileIndex += spaceCount;
						pieceMatrix[rankIndex][fileIndex] = pieceCharacter;
						spaceCount = 0;
						fileIndex += 1;
					}
				}
				if (fileIndex + spaceCount != fileCount) throw new IndexOutOfBoundsException();
			}
		} catch (final IndexOutOfBoundsException exception) {
			throw new IllegalArgumentException();
		}

		return pieceMatrix;
	}


	/**
	 * Returns an X-FEN piece section encoded from the given character based and game neutral piece
	 * matrix. Note that the ranks of the given piece matrix are expected to be ordered bottom to
	 * top, while X-FEN specifies the opposite.
	 * @param pieceMatrix the piece matrix in natural rank order
	 * @return an X-FEN piece section
	 * @throws NullPointerException if the given piece matrix, or any of it's elements, is
	 *         {@code null}
	 */
	static public String encodePieceMatrix (final char[][] pieceMatrix) {
		final int rankCount = pieceMatrix.length;
		if (rankCount == 0) return "";
		final int fileCount = pieceMatrix[0].length;
		if (fileCount == 0) return "";

		final StringWriter writer = new StringWriter();
		for (int emptyCounter = 0, rank = rankCount - 1; rank >= 0; --rank, emptyCounter = 0) {
			for (int file = 0; file < fileCount; ++file) {
				final char pieceRepresentation = pieceMatrix[rank][file];
				if (pieceRepresentation == 0) {
					emptyCounter += 1;
				} else {
					if (emptyCounter > 0) writer.write(Integer.toString(emptyCounter));
					writer.write(pieceRepresentation);
					emptyCounter = 0;
				}
			}

			if (emptyCounter > 0) writer.write(Integer.toString(emptyCounter));
			writer.write(rank == 0 ? ' ' : '/');
		}

		return writer.toString();
	}


	/**
	 * Returns {@code true} if the given X-FEN color section is {@code "w"}, or {@code false} if it
	 * is {@code "b"}.
	 * @param colorSection the X-FEN color section
	 * @return whether white is active or not
	 * @throws NullPointerException if the given section is {@code null}
	 * @throws IllegalArgumentException if the given section is neither {@code "w"} nor {@code "b"}
	 */
	static public boolean decodeColor (final String colorSection) {
		if (colorSection.length() != 1) throw new IllegalArgumentException();
		final char character = Character.toLowerCase(colorSection.charAt(0));
		if (character != 'w' & character != 'b') throw new IllegalArgumentException();
		return character == 'w';
	}


	/**
	 * Returns an X-FEN color section consisting of {@code "w"} for white, or {@code "b"} for black.
	 * @param color the color
	 * @return an X-FEN color section
	 */
	static public String encodeColor (final boolean white) {
		return white ? "w" : "b";
	}


	/**
	 * Returns the (half) move clock decoded from the given X-FEN (full) move index section, in
	 * conjunction with white being active or not. Note that the result equals the given move index
	 * decremented by one, then doubled, and then incremented by one if black is active.
	 * @param moveIndexSection the X-FEN move index section
	 * @param whiteActive whether or not white is active
	 * @return the move count
	 * @throws NullPointerException if the given section or color is {@code null}
	 * @throws IllegalArgumentException if the given section is illegal, or if the resulting move
	 *         count exceeds {code 32767}
	 */
	static public short decodeMoveClock (final String moveIndexSection, final boolean whiteActive) {
		try {
			final int moveIndex = Integer.parseInt(moveIndexSection);
			final int moveCount = ((moveIndex - 1) << 1) + (whiteActive ? 0 : 1);
			if (moveIndex < 0 | moveCount > Short.MAX_VALUE) throw new IllegalArgumentException();

			return (short) moveCount;
		} catch (final NumberFormatException exception) {
			throw new IllegalArgumentException();
		}
	}


	/**
	 * Returns the X-FEN (full) move index section encoded from the given (half) move clock. Note
	 * that the result equals half the given move clock plus one.
	 * @param moveClock the move clock
	 * @return the X-FEN move index section
	 */
	static public String encodeMoveClock (final short moveClock) {
		final int moveIndex = (moveClock >>> 1) + 1;

		return Integer.toString(moveIndex);
	}


	/**
	 * Returns the number of (half) moves performed since the last irreversible move, decoded from
	 * the given X-FEN reversible move clock section.
	 * @param reversibleMoveClockSection the X-FEN reversible move clock section
	 * @return the reversible move clock
	 * @throws NullPointerException if the given section is {@code null}
	 * @throws IllegalArgumentException if the resulting clock would be strictly negative, or exceed
	 *         {@code 127}
	 */
	static public byte decodeReversibleMoveClock (final String reversibleMoveClockSection) {
		try {
			final byte reversibleMoveCount = Byte.parseByte(reversibleMoveClockSection);
			if (reversibleMoveCount < 0) throw new IllegalArgumentException();

			return reversibleMoveCount;
		} catch (final NumberFormatException exception) {
			throw new IllegalArgumentException();
		}
	}


	/**
	 * Returns an X-FEN reversible move clock section, encoded from the given move clock.
	 * @param reversibleMoveClock the reversible move clock
	 * @return the X-FEN reversible move clock section
	 */
	static public String encodeReversibleMoveClock (final byte reversibleMoveClock) {
		return Byte.toString(reversibleMoveClock);
	}


	/**
	 * Returns the number of files decoded from the given X-FEN rank section.
	 * @param rankSection X-FEN rank section
	 * @return the file count
	 * @throws NullPointerException if the given section is {@code null}
	 * @throw IllegalArgumentException if the resulting file count would be strictly negative
	 */
	static private int decodeFileCount (final String rankSection) {
		int fileCount = 0, spaceCount = 0;
		for (final char pieceCharacter : rankSection.toCharArray()) {
			if (Character.isDigit(pieceCharacter)) {
				spaceCount = 10 * spaceCount + Character.digit(pieceCharacter, 10);
			} else {
				fileCount += spaceCount + 1;
				spaceCount = 0;
			}
		}
		fileCount += spaceCount;

		if (fileCount < 0) throw new IllegalArgumentException();
		return fileCount;
	}
}