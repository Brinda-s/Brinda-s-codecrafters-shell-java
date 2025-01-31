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
            List<String> commandTokens = cmdLine.getTokens();
            String outputFile = cmdLine.getOutputFile();
            String errorFile = cmdLine.getErrorFile();
            boolean appendOutput = cmdLine.isAppendOutput();
            boolean appendError = cmdLine.isAppendError();

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
                    parentDir.mkdirs();
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
                    parentDir.mkdirs();
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
                            handleError(command, outputFile, errorFile, appendError, "No such file or directory");
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
                            handleError(command, e.getMessage(), errorFile, appendError, e.getMessage());
                        }
                    }
                }
            }

            if (!executed) {
                handleError(command, "command not found", errorFile, appendError, "command not found");
            }

            System.out.print("$ ");
        }
    }

    private static void handleError(String command, String path, String errorFile, boolean appendError, String message) {
        String errorMsg = command + ": " + path + ": " + message;
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
}