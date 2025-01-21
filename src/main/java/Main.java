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

            // Use the LineParser for echo and cat commands to handle quotes
            if (input.startsWith("echo ")) {
                String text = input.substring(5).trim();
                LineParser parser = new LineParser(text);
                StringBuilder output = new StringBuilder();
                
                for (String token : parser.parse()) {
                    output.append(output.length() > 0 ? " " : "")
                          .append(token);
                }
                
                System.out.println(output.toString());
                System.out.print("$ ");
                continue;
            }
            if (input.startsWith("cat ")) {
                String filePaths = input.substring(4).trim();
                LineParser parser = new LineParser(filePaths);
                List<String> files = parser.parse();
                boolean hasError = false;
                List<String> allContents = new ArrayList<>();
            
                // First validate all files exist
                for (String filePath : files) {
                    File file = new File(filePath);
                    if (!file.exists() || !file.isFile()) {
                        System.out.println("cat: " + filePath + ": No such file or directory");
                        hasError = true;
                        break;
                    }
                }
            
                // If all files exist, read their contents
                if (!hasError) {
                    for (String filePath : files) {
                        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                            StringBuilder fileContent = new StringBuilder();
                            int c;
                            while ((c = reader.read()) != -1) {
                                fileContent.append((char) c);
                            }
                            allContents.add(fileContent.toString());
                        } catch (IOException e) {
                            System.out.println("cat: " + filePath + ": Error reading file");
                            hasError = true;
                            break;
                        }
                    }
                }
            
                // Only output if no errors occurred
                if (!hasError) {
                    String result = String.join(".", allContents);
                    System.out.println(result);
                }
                
                System.out.print("$ ");
                continue;
            }
            
               
            if (input.startsWith("type ")) {
                String[] parts = input.split(" ", 2);
                if (parts.length > 1) {
                    String command = parts[1];
                    if (builtins.contains(command)) {
                        System.out.println(command + " is a shell builtin");
                    } else {
                        String path = System.getenv("PATH");
                        boolean found = false;
                        if (path != null) {
                            String[] directories = path.split(":");
                            for (String dir : directories) {
                                File file = new File(dir, command);
                                if (file.exists() && file.canExecute()) {
                                    System.out.println(command + " is " + file.getAbsolutePath());
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            System.out.println(command + ": not found");
                        }
                    }
                } else {
                    System.out.println("type: not found");
                }
                System.out.print("$ ");
                continue;
            }

            if (input.equals("pwd")) {
                System.out.println(currentDirectory);
                System.out.print("$ ");
                continue;
            }

            if (input.startsWith("cd")) {
                String[] parts = input.split(" ", 2);
                if (parts.length > 1) {
                    String targetDirectory = parts[1];

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
                    } else if (targetDirectory.equals("./")) {
                        continue;
                    } else if (targetDirectory.equals("..")) {
                        File parentDir = new File(currentDirectory).getParentFile();
                        if (parentDir != null) {
                            currentDirectory = parentDir.getCanonicalPath();
                        }
                    } else {
                        System.out.println("cd: " + targetDirectory + ": No such file or directory");
                    }
                } else {
                    System.out.println("cd: No directory specified");
                }
                System.out.print("$ ");
                continue;
            }

            String[] commandParts = input.split("\\s+");
            String command = commandParts[0];
            String path = System.getenv("PATH");
            boolean executed = false;

            if (path != null) {
                String[] directories = path.split(":");
                for (String dir : directories) {
                    File file = new File(dir, command);
                    if (file.exists() && file.canExecute()) {
                        try {
                            ProcessBuilder pb = new ProcessBuilder(commandParts);
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
                System.out.println(input + ": command not found");
            }

            System.out.print("$ ");
        }
    }

}
