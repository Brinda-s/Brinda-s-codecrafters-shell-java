

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
    
    // Enhanced parse method to handle more complex scenarios
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
            
            // Handle single quotes context
            if (inSingleQuotes) {
                if (c == SINGLE) {
                    inSingleQuotes = false;
                } else {
                    currentToken.append(c);
                }
                index++;
                continue;
            }
            
            // Handle escaped characters
            if (escaped) {
                handleEscapedCharacter(currentToken, c);
                escaped = false;
                index++;
                continue;
            }
            
            // Check for escape character
            if (c == ESCAPE) {
                escaped = true;
                index++;
                continue;
            }
            
            // Handle quote contexts
            if (c == SINGLE) {
                inSingleQuotes = true;
                index++;
                continue;
            }
            
            if (c == DOUBLE) {
                inDoubleQuotes = !inDoubleQuotes;
                index++;
                continue;
            }
            
            // Handle redirection operators
            if (handleRedirectionOperators(tokens, currentToken, foundRedirect, isErrorRedirect, 
                                          appendOutput, appendError)) {
                index++;
                continue;
            }
            
            // Handle whitespace
            if (Character.isWhitespace(c)) {
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
                index++;
                continue;
            }
            
            // Append character to current token
            currentToken.append(c);
            index++;
        }
        
        // Handle remaining token
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
    
    private void handleEscapedCharacter(StringBuilder currentToken, char c) {
        switch (c) {
            case 'n': currentToken.append('\n'); break;
            case 't': currentToken.append('\t'); break;
            case 'r': currentToken.append('\r'); break;
            case '"': 
            case '\'': 
            case '\\': 
                currentToken.append(c); 
                break;
            default: 
                currentToken.append(ESCAPE).append(c);
        }
    }
    
    private boolean handleRedirectionOperators(List<String> tokens, 
                                                StringBuilder currentToken,
                                                boolean foundRedirect,
                                                boolean isErrorRedirect,
                                                boolean appendOutput,
                                                boolean appendError) {
        char c = input.charAt(index);
        
        // Detailed redirection operator handling logic
        // Similar to the existing implementation in the second file
        // This is a placeholder - you'd need to implement the full logic here
        
        return false; // Placeholder return
    }
}