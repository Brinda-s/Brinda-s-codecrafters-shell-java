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

            // Parse input to extract command tokens and redirection
            String outputFile = null;
            String errorFile = null;
            boolean appendOutput = false;
            boolean appendError = false;
            List<String> tokens = new ArrayList<>();

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

            if (tokens.isEmpty()) {
                System.out.print("$ ");
                continue;
            }

            // Create directories for output and error files if needed
            if (outputFile != null) {
                File outFile = new File(outputFile);
                if (outFile.getParentFile() != null) {
                    outFile.getParentFile().mkdirs();
                }
            }
            if (errorFile != null) {
                File errFile = new File(errorFile);
                if (errFile.getParentFile() != null) {
                    errFile.getParentFile().mkdirs();
                }
            }

            String command = tokens.get(0);
            boolean isBuiltin = builtins.contains(command);

            // Handle builtin commands
            if (isBuiltin) {
                handleBuiltinCommand(tokens, outputFile, errorFile, appendOutput, appendError, currentDirectory);
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
                            Process process = pb.start();

                            // Create final copies of the file paths for the lambda
                            final String finalErrorFile = errorFile; // Final copy for lambda
                            final String finalOutputFile = outputFile; // Final copy for lambda
                            final boolean finalAppendOutput = appendOutput; // Final copy for lambda
                            final boolean finalAppendError = appendError; // Final copy for lambda

                            // Handle stderr first
                            Thread errorThread = new Thread(() -> {
                                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                                    String errorLine;
                                    while ((errorLine = errorReader.readLine()) != null) {
                                        if (finalErrorFile != null) {
                                            try (FileWriter errorWriter = new FileWriter(finalErrorFile, finalAppendError)) {
                                                errorWriter.write(errorLine + "\n");
                                                errorWriter.flush();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                            System.err.println(errorLine);
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                            errorThread.start();

                            // Handle stdout
                            try (BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                                String outputLine;
                                while ((outputLine = outputReader.readLine()) != null) {
                                    if (finalOutputFile != null) {
                                        try (FileWriter outputWriter = new FileWriter(finalOutputFile, finalAppendOutput)) {
                                            outputWriter.write(outputLine + "\n");
                                            outputWriter.flush();
                                        }
                                    } else {
                                        System.out.println(outputLine);
                                    }
                                }
                            }

                            errorThread.join();
                            process.waitFor();
                            executed = true;
                            break;

                        } catch (IOException | InterruptedException e) {
                            String errorMsg = command + ": " + e.getMessage();
                            if (errorFile != null) {
                                try (FileWriter errorWriter = new FileWriter(errorFile, appendError)) {
                                    errorWriter.write(errorMsg + "\n");
                                }
                            } else {
                                System.err.println(errorMsg);
                            }
                        }
                    }
                }
            }

            // Handle command not found
            if (!executed) {
                String errorMsg = command + ": command not found";
                if (errorFile != null) {
                    try (FileWriter errorWriter = new FileWriter(errorFile, appendError)) {
                        errorWriter.write(errorMsg + "\n");
                    }
                } else {
                    System.err.println(errorMsg);
                }
            }

            System.out.print("$ ");
        }
    }

    private static void handleBuiltinCommand(List<String> tokens, String outputFile, String errorFile, boolean appendOutput, boolean appendError, String currentDirectory) throws IOException {
        String command = tokens.get(0);
        switch (command) {
            case "echo":
                StringBuilder output = new StringBuilder();
                for (int i = 1; i < tokens.size(); i++) {
                    output.append(tokens.get(i));
                    if (i < tokens.size() - 1) {
                        output.append(" ");
                    }
                }
                if (outputFile != null) {
                    try (FileWriter writer = new FileWriter(outputFile, appendOutput)) {
                        writer.write(output.toString() + "\n");
                    }
                } else {
                    System.out.println(output);
                }
                break;
            case "pwd":
                String pwdOutput = currentDirectory;
                if (outputFile != null) {
                    try (FileWriter writer = new FileWriter(outputFile, appendOutput)) {
                        writer.write(pwdOutput + "\n");
                    }
                } else {
                    System.out.println(pwdOutput);
                }
                break;
            case "cd":
                if (tokens.size() < 2) {
                    String errorMsg = "cd: missing argument";
                    if (errorFile != null) {
                        try (FileWriter errorWriter = new FileWriter(errorFile, appendError)) {
                            errorWriter.write(errorMsg + "\n");
                        }
                    } else {
                        System.err.println(errorMsg);
                    }
                } else {
                    File dir = new File(tokens.get(1));
                    if (dir.exists() && dir.isDirectory()) {
                        System.setProperty("user.dir", dir.getAbsolutePath());
                    } else {
                        String errorMsg = "cd: " + tokens.get(1) + ": No such file or directory";
                        if (errorFile != null) {
                            try (FileWriter errorWriter = new FileWriter(errorFile, appendError)) {
                                errorWriter.write(errorMsg + "\n");
                            }
                        } else {
                            System.err.println(errorMsg);
                        }
                    }
                }
                break;
        }
    }
}