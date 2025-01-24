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
                                
                                // Stdout redirection
                                if (outputFile != null) {
                                    File outputFileObj = new File(outputFile);
                                    createParentDirectories(outputFileObj);
                                    if (appendOutput) {
                                        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(outputFileObj));
                                    } else {
                                        pb.redirectOutput(outputFileObj);
                                    }
                                } else {
                                    pb.inheritIO();
                                }
                                
                                // Stderr redirection
                                if (errorFile != null) {
                                    File errorFileObj = new File(errorFile);
                                    createParentDirectories(errorFileObj);
                                    
                                    // Redirect stderr to file
                                    pb.redirectErrorStream(false);
                                    Process process = pb.start();
                                    
                                    try (
                                        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                                        FileWriter errorWriter = new FileWriter(errorFileObj, true)
                                    ) {
                                        String errorLine;
                                        while ((errorLine = errorReader.readLine()) != null) {
                                            errorWriter.write(errorLine + "\n");
                                        }
                                    }
                                    
                                    process.waitFor();
                                    executed = true;
                                    break;
                                } else {
                                    pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                                    pb.start().waitFor();
                                    executed = true;
                                    break;
                                }
                            } catch (IOException | InterruptedException e) {
                                // Ensure error is written to file if error redirection specified
                                if (errorFile != null) {
                                    try (PrintStream err = new PrintStream(new FileOutputStream(errorFile, true))) {
                                        err.println(e.getMessage());
                                    }
                                }
                            }
                        }
                    }
                }

                // Handle command not found
                if (!executed) {
                    if (errorFile != null) {
                        try (PrintStream err = new PrintStream(new FileOutputStream(errorFile, true))) {
                            err.println(command + ": command not found");
                        }
                    } else {
                        System.err.println(command + ": command not found");
                    }
                }
            }

            System.out.print("$ ");
        }
    }

    // Helper method to create parent directories
    private static void createParentDirectories(File file) {
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
    }
}