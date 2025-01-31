import java.io.*;
import java.util.*;

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
                    if (c == DOUBLE || c == ESCAPE) {
                        currentToken.append(c);
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
                    currentToken.append(c);
                    escaped = false;
                } else if (c == ESCAPE) {
                    escaped = true;
                    currentToken.append(ESCAPE);
                } else if (c == SINGLE) {
                    inSingleQuotes = true;
                } else if (c == DOUBLE) {
                    inDoubleQuotes = true;
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
                } else if (c == '2' && index + 1 < input.length() && input.charAt(index + 1) == '>') {
                    if (currentToken.length() > 0) {
                        tokens.add(currentToken.toString());
                        currentToken.setLength(0);
                    }
                    foundRedirect = true;
                    isErrorRedirect = true;
                    index++;
                } else if (c == '>' && index + 1 < input.length() && input.charAt(index + 1) == '>') {
                    if (currentToken.length() > 0) {
                        tokens.add(currentToken.toString());
                        currentToken.setLength(0);
                    }
                    foundRedirect = true;
                    isErrorRedirect = false;
                    appendOutput = true;
                    index++;
                } else if (c == '>' && !foundRedirect) {
                    if (currentToken.length() > 0) {
                        if (currentToken.toString().equals("1")) {
                            currentToken.setLength(0);
                        } else {
                            tokens.add(currentToken.toString());
                            currentToken.setLength(0);
                        }
                    }
                    foundRedirect = true;
                    isErrorRedirect = false;
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
        
        return new CommandLine(tokens, outputFile, errorFile, appendOutput);
    }
}

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

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.print("$ ");
        
        Scanner scanner = new Scanner(System.in);
        Set<String> builtins = new HashSet<>();
        builtins.add("echo");
        builtins.add("exit");
        builtins.add("type");
        builtins.add("pwd");
        builtins.add("cd");

        String currentDirectory = System.getProperty("user.dir");

        while (true) {
            String input = scanner.nextLine().trim();

            if (input.equals("exit 0")) {
                System.exit(0);
            }

            if (input.isEmpty()) {
                System.out.print("$ ");
                continue;
            }

            LineParser parser = new LineParser(input);
            CommandLine cmdLine = parser.parse();
            
            List<String> commandTokens = cmdLine.getTokens();
            String outputFile = cmdLine.getOutputFile();
            String errorFile = cmdLine.getErrorFile();
            boolean appendOutput = cmdLine.isAppendOutput();
            boolean appendError = false;  // Default value for error redirection

            if (commandTokens.isEmpty()) {
                System.out.print("$ ");
                continue;
            }

            String command = commandTokens.get(0);
            boolean isBuiltin = builtins.contains(command);

            // Create directories for redirection files before executing commands
            if (errorFile != null) {
                File errorFileObj = new File(errorFile);
                File parentDir = errorFileObj.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        System.err.println(command + ": " + errorFile + ": No such file or directory");
                        System.out.print("$ ");
                        continue;
                    }
                }
                if (!errorFileObj.exists()) {
                    try {
                        errorFileObj.createNewFile();
                    } catch (IOException e) {
                        System.err.println(command + ": " + errorFile + ": No such file or directory");
                        System.out.print("$ ");
                        continue;
                    }
                }
            }

            if (outputFile != null) {
                File outputFileObj = new File(outputFile);
                File parentDir = outputFileObj.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        System.err.println(command + ": " + outputFile + ": No such file or directory");
                        System.out.print("$ ");
                        continue;
                    }
                }
                if (!outputFileObj.exists()) {
                    try {
                        outputFileObj.createNewFile();
                    } catch (IOException e) {
                        System.err.println(command + ": " + outputFile + ": No such file or directory");
                        System.out.print("$ ");
                        continue;
                    }
                }
            }

            // Handle builtin commands
            if (isBuiltin) {
                if (command.equals("echo")) {
                    StringBuilder output = new StringBuilder();
                    for (int i = 1; i < commandTokens.size(); i++) {
                        output.append(commandTokens.get(i));
                        if (i < commandTokens.size() - 1) {
                            output.append(" ");
                        }
                    }

                    if (outputFile != null) {
                        try (FileWriter outputWriter = new FileWriter(outputFile, appendOutput)) {
                            outputWriter.write(output.toString() + "\n");
                        } catch (IOException e) {
                            if (errorFile != null) {
                                try (FileWriter errorWriter = new FileWriter(errorFile, appendError)) {
                                    errorWriter.write("echo: " + outputFile + ": No such file or directory\n");
                                } catch (IOException ignored) {}
                            } else {
                                System.err.println("echo: " + outputFile + ": No such file or directory");
                            }
                        }
                    } else {
                        System.out.println(output);
                    }
                }
                System.out.print("$ ");
                continue;
            }

            // Handle external commands
            String path = System.getenv("PATH");
            boolean executed = false;

            if (path != null) {
                String[] directories = path.split(":");
                for (String dir : directories) {
                    File file = new File(dir, command);
                    if (file.exists() && file.canExecute()) {
                        try {
                            ProcessBuilder pb = new ProcessBuilder(commandTokens);
                            pb.directory(new File(currentDirectory));
                            pb.redirectErrorStream(false);

                            Process process = pb.start();

                            // Handle stderr redirection
                            if (errorFile != null) {
                                try (
                                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                                    FileWriter errorWriter = new FileWriter(errorFile, appendError)
                                ) {
                                    String errorLine;
                                    while ((errorLine = errorReader.readLine()) != null) {
                                        errorWriter.write(errorLine + "\n");
                                    }
                                }
                            } else {
                                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                                    String errorLine;
                                    while ((errorLine = errorReader.readLine()) != null) {
                                        System.err.println(errorLine);
                                    }
                                }
                            }

                            // Handle stdout
                            try (BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                                String outputLine;
                                List<String> outputLines = new ArrayList<>();

                                while ((outputLine = outputReader.readLine()) != null) {
                                    outputLines.add(outputLine);
                                }

                                if (outputFile != null) {
                                    try (FileWriter outputWriter = new FileWriter(outputFile, appendOutput)) {
                                        for (String line : outputLines) {
                                            outputWriter.write(line + "\n");
                                        }
                                    }
                                } else {
                                    for (String line : outputLines) {
                                        System.out.println(line);
                                    }
                                }
                            }

                            process.waitFor();
                            executed = true;
                            break;
                        } catch (IOException | InterruptedException e) {
                            String errorMsg = command + ": " + e.getMessage();
                            if (errorFile != null) {
                                try (FileWriter errorWriter = new FileWriter(errorFile, appendError)) {
                                    errorWriter.write(errorMsg + "\n");
                                } catch (IOException ignored) {}
                            } else {
                                System.err.println(errorMsg);
                            }
                        }
                    }
                }
            }

            if (!executed) {
                String errorMsg = command + ": command not found";
                if (errorFile != null) {
                    try (FileWriter errorWriter = new FileWriter(errorFile, appendError)) {
                        errorWriter.write(errorMsg + "\n");
                    } catch (IOException e) {
                        System.err.println(command + ": " + errorFile + ": No such file or directory");
                    }
                } else {
                    System.err.println(errorMsg);
                }
            }

            System.out.print("$ ");
        }
    }
}