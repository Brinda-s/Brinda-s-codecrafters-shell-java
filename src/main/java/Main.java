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

                                Process process = pb.start();

                                // Handle stderr
                                if (errorFile != null) {
                                    try {
                                        File errorFileObj = new File(errorFile);
                                        if (errorFileObj.getParentFile() != null) {
                                            errorFileObj.getParentFile().mkdirs();
                                        }
                                        try (
                                            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                                            FileWriter errorWriter = new FileWriter(errorFile, true)
                                        ) {
                                            String errorLine;
                                            while ((errorLine = errorReader.readLine()) != null) {
                                                errorWriter.write(errorLine + "\n");
                                            }
                                        }
                                    } catch (IOException e) {
                                        System.err.println("ls: " + errorFile + ": No such file or directory");
                                        break;
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
                            } catch (IOException | InterruptedException e) {
                                System.err.println(command + ": " + e.getMessage());
                            }
                        }
                    }
                }

                // Handle command not found
                if (!executed) {
                    String errorMsg = command + ": command not found";
                    if (errorFile != null) {
                        try {
                            File errorFileObj = new File(errorFile);
                            if (errorFileObj.getParentFile() != null) {
                                errorFileObj.getParentFile().mkdirs();
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