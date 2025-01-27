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

                                // Create parent directories if needed
                                if (errorFile != null) {
                                    File errorFileObj = new File(errorFile);
                                    createParentDirectories(errorFileObj);
                                }
                                if (outputFile != null) {
                                    File outputFileObj = new File(outputFile);
                                    createParentDirectories(outputFileObj);
                                }

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
                                    } catch (IOException e) {
                                        System.err.println(command + ": " + errorFile + ": No such file or directory");
                                        break;
                                    }
                                } else {
                                    // Default error stream handling
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

                                process.waitFor();
                                executed = true;
                                break;
                            } catch (IOException | InterruptedException e) {
                                System.err.println(command + ": " + e.getMessage());
                            }
                        }
                    }
                }

                // Handle command not found
                if (!executed) {
                    if (errorFile != null) {
                        try (FileWriter errorWriter = new FileWriter(errorFile, appendError)) {
                            errorWriter.write(command + ": command not found\n");
                        } catch (IOException e) {
                            System.err.println(command + ": " + errorFile + ": No such file or directory");
                        }
                    } else {
                        System.err.println(command + ": command not found");
                    }
                }
            }

            System.out.print("$ ");
        }
    }

    private static void createParentDirectories(File file) {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
    }
}