package org.intocps.maestro.ast;

import org.intocps.maestro.ast.node.ExternalNode;

import java.util.Objects;

public class LexLocation implements ExternalNode {
    private final int line;
    private final int pos;
    private final String filename;

    public LexLocation(String filename, int line, int pos) {
        this.filename = filename;
        this.line = line;
        this.pos = pos;
    }

    public LexLocation(LexLocation other) {
        if (other == null) {
            this.filename = "Unknown";
            this.line = 0;
            this.pos = 0;

        } else {
            this.filename = other.filename;
            this.line = other.line;
            this.pos = other.pos;
        }
    }

    @Override
    public Object clone() {
        return new LexLocation(filename, line, pos);
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LexLocation that = (LexLocation) o;
        return line == that.line && pos == that.pos && Objects.equals(filename, that.filename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(line, pos, filename);
    }

    @Override
    public String toString() {
        return filename + ":" + line + ":" + pos;
    }
}
