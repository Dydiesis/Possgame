package com.possgame.nexus.engine;

import java.util.Objects;

public final class Move {
    public final int fromRow;
    public final int fromCol;
    public final int toRow;
    public final int toCol;

    public Move(int fromRow, int fromCol, int toRow, int toCol) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof Move)) return false;
        Move m = (Move) o;
        return fromRow == m.fromRow && fromCol == m.fromCol
            && toRow == m.toRow && toCol == m.toCol;
    }

    @Override public int hashCode() {
        return Objects.hash(fromRow, fromCol, toRow, toCol);
    }

    @Override public String toString() {
        return "(" + fromRow + "," + fromCol + ")->(" + toRow + "," + toCol + ")";
    }
}
