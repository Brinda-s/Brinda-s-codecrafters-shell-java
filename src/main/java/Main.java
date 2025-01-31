import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        Set<String> builtins = new HashSet<>(Arrays.asList("echo", "cd", "pwd"));
        
        System.out.print("$ ");
        while (scanner.hasNextLine()) {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                System.out.print("$ ");
                continue;
            }
            
            CommandLine cmdLine = new LineParser(input).parse();
            List<String> tokens = cmdLine.getTokens();
            
            if (tokens.isEmpty()) {
                System.out.print("$ ");
                continue;
            }
            
            String command = tokens.get(0);
            if (builtins.contains(command)) {
                if (command.equals("echo")) {
                    // Handle echo builtin
                    if (tokens.size() > 1) {
                        System.out.println(String.join(" ", tokens.subList(1, tokens.size())));
                    } else {
                        System.out.println();
                    }
                }
            } else {
                // Execute external command
                String path = System.getenv("PATH");
                boolean executed = false;
                
                if (path != null) {
                    for (String dir : path.split(":")) {
                        File file = new File(dir, command);
                        if (file.exists() && file.canExecute()) {
                            ProcessBuilder pb = new ProcessBuilder(tokens);
                            pb.redirectErrorStream(true);
                            
                            try {
                                Process process = pb.start();
                                BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(process.getInputStream()));
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    System.out.println(line);
                                }
                                process.waitFor();
                                executed = true;
                                break;
                            } catch (IOException e) {
                                System.err.println(command + ": " + e.getMessage());
                            }
                        }
                    }
                }
                
                if (!executed) {
                    System.err.println(command + ": command not found");
                }
            }
            
            System.out.print("$ ");
        }
    }
}
