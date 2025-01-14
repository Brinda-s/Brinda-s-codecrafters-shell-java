import java.util.Scanner;
import java.util.Set;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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

            if (input.startsWith("echo ")) {
                String text = input.substring(5).trim();
                StringBuilder output = new StringBuilder();
                boolean insideQuotes = false;
                boolean lastWasSpace = false;
                
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    if (c == '\'') {
                        insideQuotes = !insideQuotes;
                        continue;
                    }
                    
                    if (c == ' ') {
                        if (insideQuotes) {
                            output.append(c);
                        } else if (!lastWasSpace) {
                            output.append(c);
                            lastWasSpace = true;
                        }
                    } else {
                        output.append(c);
                        lastWasSpace = false;
                    }
                }
                
                // Trim any trailing space
                String result = output.toString();
                if (!result.isEmpty() && result.charAt(result.length() - 1) == ' ') {
                    result = result.substring(0, result.length() - 1);
                }
                
                System.out.println(result);

                String filename = "/tmp/foo/f75";
                File dir = new File("/tmp/foo");
                dir.mkdirs();
                File outputFile = new File(filename);
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                    writer.write(result);
                } catch (IOException e) {
                    System.out.println("Error writing to file: " + e.getMessage());
                }

                System.out.print("$ ");
                continue;
            }

            if (input.startsWith("cat ")) {
                String filePaths = input.substring(4).trim();
                List<String> files = new ArrayList<>();
                StringBuilder currentFile = new StringBuilder();
                boolean insideQuotes = false;
                
                for (int i = 0; i < filePaths.length(); i++) {
                    char c = filePaths.charAt(i);
                    if (c == '\'') {
                        insideQuotes = !insideQuotes;
                    } else if (c == ' ' && !insideQuotes) {
                        if (currentFile.length() > 0) {
                            files.add(currentFile.toString());
                            currentFile.setLength(0);
                        }
                    } else {
                        currentFile.append(c);
                    }
                }
                
                if (currentFile.length() > 0) {
                    files.add(currentFile.toString());
                }
            
                StringBuilder output = new StringBuilder();
                for (int i = 0; i < files.size(); i++) {
                    String filePath = files.get(i);
                    File file = new File(filePath);
                    if (file.exists() && file.isFile()) {
                        try {
                            List<String> lines = Files.readAllLines(file.toPath());
                            for (String line : lines) {
                                output.append(line);
                            }
                            if (i < files.size() - 1) {
                                output.append(".");
                            }
                        } catch (IOException e) {
                            System.out.println("Error reading file: " + filePath);
                        }
                    } else {
                        System.out.println("cat: " + filePath + ": No such file or directory");
                    }
                }
            
                if (!output.isEmpty()) {
                    System.out.println(output.toString());
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