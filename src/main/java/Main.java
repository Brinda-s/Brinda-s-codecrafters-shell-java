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

            if (tokens.isEmpty()) {
                System.out.print("$ ");
                continue;
            }

            String command = tokens.get(0);
            boolean isBuiltin = builtins.contains(command);

            // Handle external commands
            if (!isBuiltin) {
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

                                // Create directories and file for stderr redirection
                                if (errorFile != null) {
                                    File errorFileObj = new File(errorFile);
                                    File parentDir = errorFileObj.getParentFile();
                                    if (parentDir != null && !parentDir.exists()) {
                                        if (!parentDir.mkdirs()) {
                                            System.err.println("ls: " + errorFile + ": No such file or directory");
                                            System.out.print("$ ");
                                            continue;
                                        }
                                    }
                                    if (!errorFileObj.exists()) {
                                        errorFileObj.createNewFile();
                                    }
                                }

                                // Create directories and file for stdout redirection
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
                                        outputFileObj.createNewFile();
                                    }
                                }

                                Process process = pb.start();

                                // Handle stderr redirection
                                if (errorFile != null) {
                                    try (
                                        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                                        FileWriter errorWriter = new FileWriter(errorFile, true)
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
                                    while ((outputLine = outputReader.readLine()) != null) {
                                        if (outputFile != null) {
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
                            } catch (IOException | InterruptedException e) {
                                if (errorFile != null) {
                                    try (FileWriter errorWriter = new FileWriter(errorFile, true)) {
                                        errorWriter.write(command + ": " + e.getMessage() + "\n");
                                    }
                                } else {
                                    System.err.println(command + ": " + e.getMessage());
                                }
                            }
                        }
                    }
                }

                if (!executed) {
                    String errorMsg = command + ": command not found";
                    if (errorFile != null) {
                        try {
                            File errorFileObj = new File(errorFile);
                            File parentDir = errorFileObj.getParentFile();
                            if (parentDir != null && !parentDir.exists()) {
                                if (!parentDir.mkdirs()) {
                                    System.err.println("ls: " + errorFile + ": No such file or directory");
                                    System.out.print("$ ");
                                    continue;
                                }
                            }
                            try (FileWriter errorWriter = new FileWriter(errorFile, true)) {
                                errorWriter.write(errorMsg + "\n");
                            }
                        } catch (IOException e) {
                            System.err.println("ls: " + errorFile + ": No such file or directory");
                        }
                    } else {
                        System.err.println(errorMsg);
                    }
                }
            }

            System.out.print("$ ");
        }
    }
}