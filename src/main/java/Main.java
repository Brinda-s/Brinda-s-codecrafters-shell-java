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
            List<String> tokens = new ArrayList<>();
            
            String[] parts = input.split(" ");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("2>>")) {
                    if (i + 1 < parts.length) {
                        errorFile = parts[i + 1];
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

            String command = tokens.get(0);
            boolean isBuiltin = builtins.contains(command);

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
                        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile, appendOutput))) {
                            writer.println(output);
                        }
                    } else {
                        System.out.println(output);
                    }
                } else if (command.equals("pwd")) {
                    System.out.println(currentDirectory);
                } else if (command.equals("cd")) {
                    if (tokens.size() < 2) {
                        System.err.println("cd: missing argument");
                    } else {
                        File dir = new File(tokens.get(1));
                        if (dir.exists() && dir.isDirectory()) {
                            currentDirectory = dir.getAbsolutePath();
                            System.setProperty("user.dir", currentDirectory);
                        } else {
                            System.err.println("cd: " + tokens.get(1) + ": No such file or directory");
                        }
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
                            ProcessBuilder pb = new ProcessBuilder(tokens);
                            pb.directory(new File(currentDirectory));
                            pb.redirectErrorStream(false);

                            Process process = pb.start();

                            // Handle stderr
                            if (errorFile != null) {
                                File errorFileObj = new File(errorFile);
                                if (errorFileObj.getParentFile() != null) {
                                    errorFileObj.getParentFile().mkdirs();
                                }
                                try (PrintWriter errorWriter = new PrintWriter(new FileWriter(errorFile, true))) {
                                    try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                                        String errorLine;
                                        while ((errorLine = errorReader.readLine()) != null) {
                                            errorWriter.println(errorLine);
                                        }
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
                            String errorMsg = command + ": " + e.getMessage();
                            if (errorFile != null) {
                                try (PrintWriter errorWriter = new PrintWriter(new FileWriter(errorFile, true))) {
                                    errorWriter.println(errorMsg);
                                }
                            } else {
                                System.err.println(errorMsg);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            System.err.println(command + ": " + e.getMessage());
                        }
                    }
                }
            }

            // Handle command not found
            if (!executed) {
                String errorMsg = command + ": command not found";
                if (errorFile != null) {
                    try (PrintWriter errorWriter = new PrintWriter(new FileWriter(errorFile, true))) {
                        errorWriter.println(errorMsg);
                    }
                } else {
                    System.err.println(errorMsg);
                }
            }

            System.out.print("$ ");
        }
    }
}