import java.io.*;
import java.util.*;

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
            List<String> tokens = cmdLine.getTokens();
            String outputFile = cmdLine.getOutputFile();
            String errorFile = cmdLine.getErrorFile();
            boolean appendOutput = cmdLine.isAppendOutput();
            boolean appendError = cmdLine.isAppendError();
            
            if (tokens.isEmpty()) {
                System.out.print("$ ");
                continue;
            }

            String command = tokens.get(0);
            boolean isBuiltin = builtins.contains(command);

            // Handle builtin commands
            if (isBuiltin) {
                handleBuiltinCommand(command, tokens, errorFile, appendError, currentDirectory);
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
                            ProcessBuilder pb = new ProcessBuilder(tokens);
                            pb.directory(new File(currentDirectory));
                            pb.redirectErrorStream(false);

                            Process process = pb.start();

                            // Handle stderr with append support
                            if (errorFile != null) {
                                File errorFileObj = new File(errorFile);
                                if (errorFileObj.getParentFile() != null) {
                                    errorFileObj.getParentFile().mkdirs();
                                }
                                try (PrintWriter errorWriter = new PrintWriter(new FileWriter(errorFile, appendError))) {
                                    try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                                        String errorLine;
                                        while ((errorLine = errorReader.readLine()) != null) {
                                            errorWriter.println(errorLine);
                                        }
                                    }
                                }
                            } else {
                                // If no error redirection, print to console
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
                                while ((outputLine = outputReader.readLine()) != null) {
                                    if (outputFile != null) {
                                        File outputFileObj = new File(outputFile);
                                        if (outputFileObj.getParentFile() != null) {
                                            outputFileObj.getParentFile().mkdirs();
                                        }
                                        try (FileWriter outputWriter = new FileWriter(outputFile, appendOutput)) {
                                            outputWriter.write(outputLine + "\n");
                                        }
                                    } else {
                                        System.out.println(outputLine);
                                    }
                                }
                            }

                            process.waitFor();
                            executed = true;
                            break;
                        } catch (IOException e) {
                            handleCommandError(command, e.getMessage(), errorFile, appendError);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            handleCommandError(command, e.getMessage(), errorFile, appendError);
                        }
                    }
                }
            }

            // Handle command not found
            if (!executed) {
                handleCommandError(command, "command not found", errorFile, appendError);
            }

            System.out.print("$ ");
        }
    }

    private static void handleBuiltinCommand(String command, List<String> tokens, String errorFile, boolean appendError, String currentDirectory) throws IOException {
        switch (command) {
            case "pwd":
                System.out.println(currentDirectory);
                break;
            case "echo":
                if (tokens.size() > 1) {
                    System.out.println(String.join(" ", tokens.subList(1, tokens.size())));
                } else {
                    System.out.println();
                }
                break;
            // Add other builtin commands as needed
        }
    }

    private static void handleCommandError(String command, String errorMessage, String errorFile, boolean appendError) throws IOException {
        String fullError = command + ": " + errorMessage;
        if (errorFile != null) {
            try (PrintWriter errorWriter = new PrintWriter(new FileWriter(errorFile, appendError))) {
                errorWriter.println(fullError);
            }
        } else {
            System.err.println(fullError);
        }
    }
}

class LineParser {
    private final String input;
    
    public LineParser(String input) {
        this.input = input;
    }
    
    public CommandLine parse() {
        List<String> tokens = new ArrayList<>();
        String outputFile = null;
        String errorFile = null;
        boolean appendOutput = false;
        boolean appendError = false;
        
        String[] parts = input.split(" ");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("2>>")) {
                if (i + 1 < parts.length) {
                    errorFile = parts[i + 1];
                    appendError = true;
                    i++;
                }
            } else if (parts[i].equals(">>")) {
                if (i + 1 < parts.length) {
                    outputFile = parts[i + 1];
                    appendOutput = true;
                    i++;
                }
            } else if (parts[i].equals(">")) {
                if (i + 1 < parts.length) {
                    outputFile = parts[i + 1];
                    i++;
                }
            } else {
                tokens.add(parts[i]);
            }
        }
        
        return new CommandLine(tokens, outputFile, errorFile, appendOutput, appendError);
    }
}

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
    
    public List<String> getTokens() { return tokens; }
    public String getOutputFile() { return outputFile; }
    public String getErrorFile() { return errorFile; }
    public boolean isAppendOutput() { return appendOutput; }
    public boolean isAppendError() { return appendError; }
}