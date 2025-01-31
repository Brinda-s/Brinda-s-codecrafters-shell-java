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
            String input = readLineWithAutocomplete(scanner, builtins).trim();

            if (input.equals("exit 0")) {
                System.exit(0);
            }

            if (input.isEmpty()) {
                System.out.print("$ ");
                continue;
            }

            // Parse command line with potential redirection
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
                if (command.equals("echo")) {
                    handleEcho(tokens, outputFile, appendOutput, errorFile, appendError);
                } else if (command.equals("pwd")) {
                    handlePwd(currentDirectory, outputFile, appendOutput, errorFile, appendError);
                } else if (command.equals("cd")) {
                    currentDirectory = handleCd(tokens, currentDirectory, outputFile, appendOutput, errorFile, appendError);
                } else if (command.equals("type")) {
                    handleType(tokens, builtins, outputFile, appendOutput, errorFile, appendError);
                }
            } else {
                handleExternalCommand(tokens, command, currentDirectory, outputFile, appendOutput, errorFile, appendError);
            }

            System.out.print("$ ");
        }
    }

    /**
     * Read the input from the user with autocomplete support for built-in commands.
     */
    private static String readLineWithAutocomplete(Scanner scanner, Set<String> builtins) throws IOException {
        StringBuilder input = new StringBuilder();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            int ch = reader.read();

            // Handle TAB key (autocomplete)
            if (ch == 9) { // TAB key ASCII
                String partialCommand = input.toString().trim();
                String completedCommand = autocompleteCommand(partialCommand, builtins);
                if (completedCommand != null) {
                    System.out.print(completedCommand.substring(partialCommand.length()) + " ");
                    input.append(completedCommand.substring(partialCommand.length()) + " ");
                }
            } else if (ch == 10) { // ENTER key (newline)
                System.out.println();
                break;
            } else {
                System.out.print((char) ch);
                input.append((char) ch);
            }
        }
        return input.toString();
    }

    /**
     * Autocomplete the command based on the input and available built-ins.
     */
    private static String autocompleteCommand(String partialCommand, Set<String> builtins) {
        for (String command : builtins) {
            if (command.startsWith(partialCommand)) {
                return command;
            }
        }
        return null; // No match found
    }

    /**
     * Handle 'echo' command logic.
     */
    private static void handleEcho(List<String> tokens, String outputFile, boolean appendOutput, String errorFile, boolean appendError) {
        StringBuilder output = new StringBuilder();
        for (int i = 1; i < tokens.size(); i++) {
            output.append(tokens.get(i));
            if (i < tokens.size() - 1) {
                output.append(" ");
            }
        }
        writeOutput(output.toString(), outputFile, appendOutput, errorFile, appendError);
    }

    /**
     * Handle 'pwd' command logic.
     */
    private static void handlePwd(String currentDirectory, String outputFile, boolean appendOutput, String errorFile, boolean appendError) {
        writeOutput(currentDirectory, outputFile, appendOutput, errorFile, appendError);
    }

    /**
     * Handle 'cd' command logic.
     */
    private static String handleCd(List<String> tokens, String currentDirectory, String outputFile, boolean appendOutput, String errorFile, boolean appendError) {
        if (tokens.size() > 1) {
            String targetDirectory = tokens.get(1);
            if (targetDirectory.startsWith("~")) {
                String homeDirectory = System.getenv("HOME");
                if (homeDirectory != null) {
                    targetDirectory = homeDirectory + targetDirectory.substring(1);
                }
            }
            File newDir = new File(targetDirectory);
            if (!newDir.isAbsolute()) {
                newDir = new File(currentDirectory, targetDirectory);
            }

            if (newDir.exists() && newDir.isDirectory()) {
                try {
                    return newDir.getCanonicalPath();
                } catch (IOException e) {
                    handleError("cd: " + e.getMessage(), errorFile, appendError);
                }
            } else {
                handleError("cd: " + targetDirectory + ": No such file or directory", errorFile, appendError);
            }
        }
        return currentDirectory;
    }

    /**
     * Handle 'type' command logic.
     */
    private static void handleType(List<String> tokens, Set<String> builtins, String outputFile, boolean appendOutput, String errorFile, boolean appendError) {
        if (tokens.size() > 1) {
            String typeCommand = tokens.get(1);
            String output;
            if (builtins.contains(typeCommand)) {
                output = typeCommand + " is a shell builtin";
            } else {
                output = typeCommand + ": not found";
            }
            writeOutput(output, outputFile, appendOutput, errorFile, appendError);
        }
    }

    /**
     * Handle external commands.
     */
    private static void handleExternalCommand(List<String> tokens, String command, String currentDirectory, String outputFile, boolean appendOutput, String errorFile, boolean appendError) {
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

                        if (outputFile != null) {
                            if (appendOutput) {
                                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(outputFile)));
                            } else {
                                pb.redirectOutput(new File(outputFile));
                            }
                        } else {
                            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                        }

                        if (errorFile != null) {
                            if (appendError) {
                                pb.redirectError(ProcessBuilder.Redirect.appendTo(new File(errorFile)));
                            } else {
                                pb.redirectError(new File(errorFile));
                            }
                        } else {
                            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                        }

                        Process process = pb.start();
                        process.waitFor();
                        executed = true;
                        break;
                    } catch (IOException | InterruptedException e) {
                        handleError(command + ": " + e.getMessage(), errorFile, appendError);
                    }
                }
            }
        }

        if (!executed) {
            handleError(command + ": command not found", errorFile, appendError);
        }
    }

    /**
     * Helper method to handle output redirection and display.
     */
    private static void writeOutput(String output, String outputFile, boolean appendOutput, String errorFile, boolean appendError) {
        if (outputFile != null) {
            try (FileWriter outputWriter = new FileWriter(outputFile, appendOutput)) {
                outputWriter.write(output + "\n");
            } catch (IOException e) {
                handleError("Error writing to " + outputFile, errorFile, appendError);
            }
        } else {
            System.out.println(output);
        }
    }

    /**
     * Helper method to handle error redirection and display.
     */
    private static void handleError(String errorMsg, String errorFile, boolean appendError) {
        if (errorFile != null) {
            try (FileWriter errorWriter = new FileWriter(errorFile, appendError)) {
                errorWriter.write(errorMsg + "\n");
            } catch (IOException ignored) {}
        } else {
            System.err.println(errorMsg);
        }
    }
}
