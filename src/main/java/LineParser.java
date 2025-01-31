

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
                    // Inside single quotes, everything is literal, including backslashes
                    currentToken.append(c);
                }
            } else if (inDoubleQuotes) {
                if (escaped) {
                    // In double quotes, only certain characters are escaped
                    if (c == 'n') {
                        currentToken.append('\n');
                    } else if (c == 't') {
                        currentToken.append('\t');
                    } else if (c == '"' || c == '\\' || c == '$' || c == '`') {
                        currentToken.append(c);
                    } else {
                        // Keep both backslash and character for other cases
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
                    // Outside quotes, handle escape sequences
                    if (c == 'n') {
                        currentToken.append('\n');
                    } else if (c == 't') {
                        currentToken.append('\t');
                    } else if (c == 'r') {
                        currentToken.append('\r');
                    } else if (c == '"' || c == '\'' || c == '\\') {
                        currentToken.append(c);
                    } else {
                        // For other characters, treat the backslash as a literal character
                        currentToken.append(ESCAPE).append(c);
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
                    if (currentToken.length() > 0) {
                        tokens.add(currentToken.toString());
                        currentToken.setLength(0);
                    }
                    foundRedirect = true;
                    isErrorRedirect = true;
                    appendError = true;
                    index += 2;
                } else if (c == '2' && index + 1 < input.length() && input.charAt(index + 1) == '>') {
                    if (currentToken.length() > 0) {
                        tokens.add(currentToken.toString());
                        currentToken.setLength(0);
                    }
                    foundRedirect = true;
                    isErrorRedirect = true;
                    index++;
                } else if (c == '1' && index + 2 < input.length() && 
                         input.charAt(index + 1) == '>' && input.charAt(index + 2) == '>') {
                    if (currentToken.length() > 0) {
                        tokens.add(currentToken.toString());
                        currentToken.setLength(0);
                    }
                    foundRedirect = true;
                    isErrorRedirect = false;
                    appendOutput = true;
                    index += 2;
                } else if (c == '>' && index + 1 < input.length() && input.charAt(index + 1) == '>') {
                    if (currentToken.length() > 0) {
                        tokens.add(currentToken.toString());
                        currentToken.setLength(0);
                    }
                    foundRedirect = true;
                    isErrorRedirect = false;
                    appendOutput = true;
                    index++;
                } else if (c == '>' || (c == '1' && index + 1 < input.length() && 
                         input.charAt(index + 1) == '>')) {
                    if (currentToken.length() > 0) {
                        tokens.add(currentToken.toString());
                        currentToken.setLength(0);
                    }
                    foundRedirect = true;
                    isErrorRedirect = false;
                    if (c == '1') index++;
                } else if (Character.isWhitespace(c)) {
                    if (currentToken.length() > 0) {
                        if (foundRedirect) {
                            if (isErrorRedirect) {
                                errorFile = currentToken.toString();
                            } else {
                                outputFile = currentToken.toString();
                            }
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
        
        // Handle any remaining token
        if (currentToken.length() > 0) {
            if (foundRedirect) {
                if (isErrorRedirect) {
                    errorFile = currentToken.toString();
                } else {
                    outputFile = currentToken.toString();
                }
            } else {
                tokens.add(currentToken.toString());
            }
        }
        
        return new CommandLine(tokens, outputFile, errorFile, appendOutput, appendError);
    }
}