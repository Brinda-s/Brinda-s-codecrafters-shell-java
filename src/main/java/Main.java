import java.util.Scanner;
import java.util.Set;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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

            // Use LineParser to handle the entire command line including the executable name
            LineParser parser = new LineParser(input);
            List<String> tokens = parser.parse();
            
            if (tokens.isEmpty()) {
                System.out.print("$ ");
                continue;
            }

            String command = tokens.get(0);

            if (builtins.contains(command)) {
                // Handle built-in commands
                if (command.equals("echo")) {
                    StringBuilder output = new StringBuilder();
                    for (int i = 1; i < tokens.size(); i++) {
                        output.append(output.length() > 0 ? " " : "")
                              .append(tokens.get(i));
                    }
                    System.out.println(output.toString());
                } else if (command.equals("type")) {
                    if (tokens.size() > 1) {
                        String typeCommand = tokens.get(1);
                        if (builtins.contains(typeCommand)) {
                            System.out.println(typeCommand + " is a shell builtin");
                        } else {
                            String path = System.getenv("PATH");
                            boolean found = false;
                            if (path != null) {
                                String[] directories = path.split(":");
                                for (String dir : directories) {
                                    File file = new File(dir, typeCommand);
                                    if (file.exists() && file.canExecute()) {
                                        System.out.println(typeCommand + " is " + file.getAbsolutePath());
                                        found = true;
                                        break;
                                    }
                                }
                            }
                            if (!found) {
                                System.out.println(typeCommand + ": not found");
                            }
                        }
                    }
                } else if (command.equals("pwd")) {
                    System.out.println(currentDirectory);
                } else if (command.equals("cd")) {
                    if (tokens.size() > 1) {
                        String targetDirectory = tokens.get(1);
                        if (targetDirectory.startsWith("~")) {
                            String homeDirectory = System.getenv("HOME");
                            if (homeDirectory == null) {
                                System.out.println("cd: Home not set");
                                System.out.print("$ ");
                                continue;
                            }
                            targetDirectory = homeDirectory + targetDirectory.substring(1);
                        }

                        File newDir = new File(targetDirectory);
                        if (!newDir.isAbsolute()) {
                            newDir = new File(currentDirectory, targetDirectory);
                        }

                        if (newDir.exists() && newDir.isDirectory()) {
                            currentDirectory = newDir.getCanonicalPath();
                        } else {
                            System.out.println("cd: " + targetDirectory + ": No such file or directory");
                        }
                    }
                }
            } else {
                // Handle external commands
                String path = System.getenv("PATH");
                boolean executed = false;

                if (path != null) {
                    String[] directories = path.split(":");
                    for (String dir : directories) {
                        // Try to find the executable with its quoted name
                        File file = new File(dir, command);
                        if (file.exists() && file.canExecute()) {
                            try {
                                ProcessBuilder pb = new ProcessBuilder(tokens);
                                pb.directory(new File(currentDirectory));
                                pb.inheritIO();
                                Process process = pb.start();
                                process.waitFor();
                                executed = true;
                                break;
                            } catch (IOException | InterruptedException e) {
                                System.out.println("Error executing command: " + e.getMessage());
                            }
                        }
                    }
                }

                if (!executed) {
                    System.out.println(command + ": command not found");
                }
            }

            System.out.print("$ ");
        }
    }
}