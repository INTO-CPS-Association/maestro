package org.intocps.maestro.parser;

import org.antlr.v4.runtime.*;
import org.intocps.maestro.ast.LexToken;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Vector;

public class MablParserUtil {
    final static Logger logger = LoggerFactory.getLogger(MablParserUtil.class);

    private static List<ARootDocument> parseStreams(List<CharStream> specStreams) {
        List<ARootDocument> documentList = new Vector<>();
        for (CharStream specStream : specStreams) {
            documentList.add(parse(specStream));
        }
        return documentList;
    }

    public static ARootDocument parse(CharStream specStreams) {
        MablLexer l = new MablLexer(specStreams);
        MablParser p = new MablParser(new CommonTokenStream(l));
        p.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg,
                    RecognitionException e) {
                System.out.println(specStreams);
                throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
            }
        });
        MablParser.CompilationUnitContext unit = p.compilationUnit();

        ARootDocument root = (ARootDocument) new ParseTree2AstConverter().visit(unit);
        return root;
    }

    public static ARootDocument parse(CharStream specStreams, IErrorReporter reporter) {
        MablLexer l = new MablLexer(specStreams);
        MablParser p = new MablParser(new CommonTokenStream(l));
        p.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg,
                    RecognitionException e) {
                //throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
                reporter.report(0, msg, new LexToken("", line, 0));
            }
        });
        MablParser.CompilationUnitContext unit = p.compilationUnit();

        ARootDocument root = (ARootDocument) new ParseTree2AstConverter().visit(unit);
        return root;
    }


    public static List<ARootDocument> parse(List<File> sourceFiles, IErrorReporter reporter) throws IOException {
        List<ARootDocument> documentList = new Vector<>();

        for (File file : sourceFiles) {
            if (!file.exists()) {
                logger.warn("Unable to parse file. File does not exist: {}", file);
                continue;
            }
            logger.info("Parting file: {}", file);

            documentList.add(parse(CharStreams.fromPath(Paths.get(file.toURI())), reporter));
        }
        return documentList;
    }

    public static List<ARootDocument> parse(List<File> sourceFiles) throws IOException {
        List<ARootDocument> documentList = new Vector<>();

        for (File file : sourceFiles) {
            if (!file.exists()) {
                logger.warn("Unable to parse file. File does not exist: {}", file);
                continue;
            }
            logger.info("Parting file: {}", file);

            documentList.add(parse(CharStreams.fromPath(Paths.get(file.toURI()), StandardCharsets.UTF_8)));
        }
        return documentList;
    }
}
