package andrew.a5.logic;

import java.util.Objects;

/**
 * An immutable position (row, column) on the board. Row and column
 * numbers start from 0.
 */
public class Position {

    /**
     * Represent the board position ({@code rowNo}, {@code colNo}).
     */
    final private int rowNo, colNo;

    /**
     * Creates: the position ({@code row}, {@code column}).
     */
    public Position(int row, int column) {
        rowNo = row;
        colNo = column;
    }

    /**
     * Returns: the row
     */
    public int row() {
        return rowNo;
    }

    /**
     * Returns: the column
     */
    public int col() {
        return colNo;
    }

    /**
     * Equality is state equality.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Position boardMove = (Position) o;
        return rowNo == boardMove.rowNo && colNo == boardMove.colNo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowNo, colNo);
    }

    @Override
    public String toString() {
        return "(" + rowNo + ", " + colNo + ")";
    }
}
