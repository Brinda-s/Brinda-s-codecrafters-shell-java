import java.io.*;
import java.util.*;

class CommandLine {
    private final List<String> tokens;
    private final String outputFile;
    private final String errorFile;
    private final boolean appendOutput;
    private final boolean appendError;

    public CommandLine(List<String> tokens, String outputFile, String errorFile, boolean appendOutput, boolean appendError) {
        this.tokens = tokens;
        this.outputFile = outputFile;
        this.errorFile = errorFile;
        this.appendOutput = appendOutput;
        this.appendError = appendError;
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

    public boolean isAppendError() {
        return appendError;
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
        boolean appendError = false;
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
                    if (c == DOUBLE || c == ESCAPE || c == '$' || c == '`') {
                        currentToken.append(c);
                    } else if (c == '\n') {
                        // Line continuation, do nothing
                    } else {
                        currentToken.append(ESCAPE).append(c);
                    }
                    escaped = false;
                } else if (c == ESCAPE) {
                    escaped = true;
                } else if (c == DOUBLE) {
                    inDoubleQuotes = false;
                } else {
                    currentToken.append(c);
                }
            } else {
                if (escaped) {
                    if (c != '\n') {
                        currentToken.append(c);
                    }
                    escaped = false;
                } else if (c == ESCAPE) {
                    escaped = true;
                } else if (c == SINGLE) {
                    inSingleQuotes = true;
                } else if (c == DOUBLE) {
                    inDoubleQuotes = true;
                } else if (c == '2' && index + 2 < input.length() && 
                         input.charAt(index + 1) == '>' && input.charAt(index + 2) == '>') {
                    handleRedirect(currentToken, tokens, foundRedirect);
                    foundRedirect = true;
                    isErrorRedirect = true;
                    appendError = true;
                    index += 2;
                } else if (c == '2' && index + 1 < input.length() && input.charAt(index + 1) == '>') {
                    handleRedirect(currentToken, tokens, foundRedirect);
                    foundRedirect = true;
                    isErrorRedirect = true;
                    index++;
                } else if (c == '1' && index + 2 < input.length() && 
                         input.charAt(index + 1) == '>' && input.charAt(index + 2) == '>') {
                    handleRedirect(currentToken, tokens, foundRedirect);
                    foundRedirect = true;
                    isErrorRedirect = false;
                    appendOutput = true;
                    index += 2;
                } else if (c == '>' && index + 1 < input.length() && input.charAt(index + 1) == '>') {
                    handleRedirect(currentToken, tokens, foundRedirect);
                    foundRedirect = true;
                    isErrorRedirect = false;
                    appendOutput = true;
                    index++;
                } else if (c == '>' || (c == '1' && index + 1 < input.length() && 
                         input.charAt(index + 1) == '>')) {
                    handleRedirect(currentToken, tokens, foundRedirect);
                    foundRedirect = true;
                    isErrorRedirect = false;
                    if (c == '1') index++;
                } else if (Character.isWhitespace(c)) {
                    if (currentToken.length() > 0) {
                        if (foundRedirect) {
                            setRedirectFile(currentToken, isErrorRedirect, errorFile, outputFile);
                            currentToken.setLength(0);
                            foundRedirect = false;
                        } else {
                            tokens.add(currentToken.toString());
                            currentToken.setLength(0);
                        }
                    }
                } else {
                    currentToken.append(c);
                }
            }
            
            index++;
        }
        
        if (currentToken.length() > 0) {
            if (foundRedirect) {
                setRedirectFile(currentToken, isErrorRedirect, errorFile, outputFile);
            } else {
                tokens.add(currentToken.toString());
            }
        }
        
        return new CommandLine(tokens, outputFile, errorFile, appendOutput, appendError);
    }

    private void handleRedirect(StringBuilder currentToken, List<String> tokens, boolean foundRedirect) {
        if (currentToken.length() > 0 && !foundRedirect) {
            tokens.add(currentToken.toString());
            currentToken.setLength(0);
        }
    }

    private void setRedirectFile(StringBuilder currentToken, boolean isErrorRedirect, String errorFile, String outputFile) {
        if (isErrorRedirect) {
            errorFile = currentToken.toString();
        } else {
            outputFile = currentToken.toString();
        }
    }
}
