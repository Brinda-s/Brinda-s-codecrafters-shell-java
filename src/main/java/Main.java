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

            // Parse input preserving quoted strings with proper escape handling
            LineParser lineParser = new LineParser(input);
            CommandLine commandLine = lineParser.parse();

            List<String> tokens = commandLine.getTokens();
            String outputFile = processFilename(commandLine.getOutputFile());
            String errorFile = processFilename(commandLine.getErrorFile());
            boolean appendOutput = commandLine.isAppendOutput();
            boolean appendError = commandLine.isAppendError();

            if (tokens.isEmpty()) {
                System.out.print("$ ");
                continue;
            }

            String command = tokens.get(0);
            boolean isBuiltin = builtins.contains(command);

            // Handle 'cd' command
            if (command.equals("cd")) {
                if (tokens.size() > 1) {
                    File newDir = new File(tokens.get(1));
                    if (newDir.isDirectory()) {
                        currentDirectory = newDir.getAbsolutePath();
                        System.setProperty("user.dir", currentDirectory);
                    } else {
                        System.err.println("cd: " + tokens.get(1) + ": No such directory");
                    }
                }
                System.out.print("$ ");
                continue;
            }

            // Create directories for redirection files
            if (errorFile != null) {
                File errorFileObj = new File(errorFile);
                File parentDir = errorFileObj.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                try {
                    errorFileObj.createNewFile();
                } catch (IOException e) {
                    System.err.println(command + ": " + errorFile + ": No such file or directory");
                    System.out.print("$ ");
                    continue;
                }
            }

            if (outputFile != null) {
                File outputFileObj = new File(outputFile);
                File parentDir = outputFileObj.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                try {
                    outputFileObj.createNewFile();
                } catch (IOException e) {
                    System.err.println(command + ": " + outputFile + ": No such file or directory");
                    System.out.print("$ ");
                    continue;
                }
            }

            // Handle builtin commands
            if (isBuiltin) {
                if (command.equals("echo")) {
                    StringBuilder output = new StringBuilder();
                    for (int i = 1; i < tokens.size(); i++) {
                        output.append(tokens.get(i));
                        if (i < tokens.size() - 1) {
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
                String[] directories = path.split(File.pathSeparator);
                for (String dir : directories) {
                    File file = new File(dir, command);
                    if (file.exists() && file.canExecute()) {
                        try {
                            ProcessBuilder pb = new ProcessBuilder(tokens);
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

                            // Handle stdout redirection
                            if (outputFile != null) {
                                try (
                                    BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                                    FileWriter outputWriter = new FileWriter(outputFile, appendOutput)
                                ) {
                                    String outputLine;
                                    while ((outputLine = outputReader.readLine()) != null) {
                                        outputWriter.write(outputLine + "\n");
                                    }
                                }
                            } else {
                                try (BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                                    String outputLine;
                                    while ((outputLine = outputReader.readLine()) != null) {
                                        System.out.println(outputLine);
                                    }
                                }
                            }

                            executed = true;
                            break;
                        } catch (IOException e) {
                            System.err.println("Error executing command: " + e.getMessage());
                        }
                    }
                }
            }

            if (!executed) {
                System.err.println("Command not found: " + command);
            }

            System.out.print("$ ");
        }
    }

    // Helper method to process filenames with backslashes
    private static String processFilename(String filename) {
        if (filename == null) return null;
        // Normalize backslashes while preserving the actual backslash character
        return filename.replace("\\", File.separator);
    }
}