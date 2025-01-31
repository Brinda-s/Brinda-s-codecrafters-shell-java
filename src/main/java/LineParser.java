import java.util.ArrayList;
import java.util.List;

class CommandLine {
    private final List<String> tokens;
    private final String outputFile;
    private final String errorFile;
    private final boolean appendOutput;

    public CommandLine(List<String> tokens, String outputFile, String errorFile, boolean appendOutput) {
        this.tokens = tokens;
        this.outputFile = outputFile;
        this.errorFile = errorFile;
        this.appendOutput = appendOutput;
    }

    public List<String> getTokens() {
        return tokens;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public String getErrorFile() {
        return errorFile;
    }

    public boolean isAppendOutput() {
        return appendOutput;
    }
}

class LineParser {
    public static final char SINGLE = '\'';
    public static final char DOUBLE = '"';
    public static final char ESCAPE = '\\';

    private final String input;
    private int index;

    public LineParser(String input) {
        this.input = input;
        this.index = 0;
    }

    public CommandLine parse() {
        List<String> tokens = new ArrayList<>();
        String outputFile = null;
        String errorFile = null;
        boolean appendOutput = false;
        StringBuilder currentToken = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        boolean escaped = false;
        boolean foundRedirect = false;
        boolean isErrorRedirect = false;

        while (index < input.length()) {
            char c = input.charAt(index);

            if (inSingleQuotes) {
                if (c == SINGLE) {
                    inSingleQuotes = false;
                } else {
                    currentToken.append(c);
                }
            } else if (inDoubleQuotes) {
                if (escaped) {
                    currentToken.append(c); // Correctly process the escaped character
                    escaped = false;
                } else if (c == ESCAPE) {
                    escaped = true; // Mark the next character for escaping
                } else if (c == DOUBLE) {
                    inDoubleQuotes = false; // Close double quotes
                } else {
                    currentToken.append(c);
                }
            } else {
                if (escaped) {
                    currentToken.append(c); // Preserve backslash escape
                    escaped = false;
                } else if (c == ESCAPE) {
                    escaped = true;
                } else if (c == SINGLE) {
                    inSingleQuotes = true;
                } else if (c == DOUBLE) {
                    inDoubleQuotes = true;
                } else if (Character.isWhitespace(c)) {
                    if (currentToken.length() > 0) {
                        tokens.add(currentToken.toString());
                        currentToken.setLength(0);
                    }
                } else {
                    currentToken.append(c);
                }
            }
            index++;
        }

        if (currentToken.length() > 0) {
            tokens.add(currentToken.toString());
        }

        return new CommandLine(tokens, outputFile, errorFile, appendOutput);
    }
}
