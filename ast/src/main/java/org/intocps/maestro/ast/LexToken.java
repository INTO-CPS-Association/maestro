package org.intocps.maestro.ast;

import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.intf.IAnalysis;
import org.intocps.maestro.ast.analysis.intf.IAnswer;
import org.intocps.maestro.ast.analysis.intf.IQuestion;
import org.intocps.maestro.ast.analysis.intf.IQuestionAnswer;

import java.util.Map;

public class LexToken extends Token {

    public LexToken(String text, int line, int pos) {
        setLine(line);
        setPos(pos);
        setText(text);
    }

    @Override
    public Object clone() {
        return new LexToken(getText(), getLine(), getPos());
    }

    @Override
    public INode clone(Map<INode, INode> oldToNewMap) {
        LexToken node = (LexToken) clone();
        oldToNewMap.put(this, node);
        return node;
    }

    @Override
    public void apply(IAnalysis analysis) throws AnalysisException {
        analysis.defaultIToken(this);
    }

    @Override
    public <A> A apply(IAnswer<A> caller) throws AnalysisException {
        return caller.defaultIToken(this);
    }

    @Override
    public <Q> void apply(IQuestion<Q> caller, Q question) throws AnalysisException {
        caller.defaultIToken(this, question);
    }

    @Override
    public <Q, A> A apply(IQuestionAnswer<Q, A> caller, Q question) throws AnalysisException {
        return caller.defaultIToken(this, question);
    }

    @Override
    public String toString() {
        return "'" + getText() + "' at " + getLine() + ":" + getPos();
    }
}
