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

            if (commandTokens.isEmpty()) {
                System.out.print("$ ");
                continue;
            }

            String command = commandTokens.get(0);
            boolean isBuiltin = builtins.contains(command);

            // Create directories and files for redirection
            if (errorFile != null) {
                try {
                    File errorFileObj = new File(errorFile);
                    File parentDir = errorFileObj.getParentFile();
                    if (parentDir != null) {
                        parentDir.mkdirs();
                    }
                    if (!errorFileObj.exists()) {
                        errorFileObj.createNewFile();
                    }
                } catch (IOException e) {
                    System.err.println(command + ": " + errorFile + ": No such file or directory");
                    System.out.print("$ ");
                    continue;
                }
            }

            if (outputFile != null) {
                try {
                    File outputFileObj = new File(outputFile);
                    File parentDir = outputFileObj.getParentFile();
                    if (parentDir != null) {
                        parentDir.mkdirs();
                    }
                    if (!outputFileObj.exists()) {
                        outputFileObj.createNewFile();
                    }
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
                    for (int i = 1; i < commandTokens.size(); i++) {
                        output.append(commandTokens.get(i));
                        if (i < commandTokens.size() - 1) {
                            output.append(" ");
                        }
                    }

                    try {
                        if (outputFile != null) {
                            try (FileWriter writer = new FileWriter(outputFile, appendOutput)) {
                                writer.write(output.toString() + "\n");
                            }
                        } else {
                            System.out.println(output);
                        }
                    } catch (IOException e) {
                        handleError("echo: " + outputFile + ": No such file or directory", errorFile, appendOutput);
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

                            // Set up output redirection
                            if (outputFile != null) {
                                pb.redirectOutput(appendOutput ? 
                                    ProcessBuilder.Redirect.appendTo(new File(outputFile)) : 
                                    ProcessBuilder.Redirect.to(new File(outputFile)));
                            }

                            // Set up error redirection
                            if (errorFile != null) {
                                pb.redirectError(appendOutput ? 
                                    ProcessBuilder.Redirect.appendTo(new File(errorFile)) : 
                                    ProcessBuilder.Redirect.to(new File(errorFile)));
                            }

                            Process process = pb.start();
                            
                            // If no redirection, handle output and error streams
                            if (outputFile == null) {
                                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        System.out.println(line);
                                    }
                                }
                            }
                            
                            if (errorFile == null) {
                                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        System.err.println(line);
                                    }
                                }
                            }

                            process.waitFor();
                            executed = true;
                            break;
                        } catch (IOException | InterruptedException e) {
                            handleError(command + ": " + e.getMessage(), errorFile, appendOutput);
                        }
                    }
                }
            }

            if (!executed) {
                handleError(command + ": command not found", errorFile, appendOutput);
            }

            System.out.print("$ ");
        }
    }

    private static void handleError(String message, String errorFile, boolean append) {
        try {
            if (errorFile != null) {
                try (FileWriter writer = new FileWriter(errorFile, append)) {
                    writer.write(message + "\n");
                }
            } else {
                System.err.println(message);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}