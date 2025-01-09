import java.util.Scanner;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws Exception {
        
        System.out.print("$ ");

        Scanner scanner = new Scanner(System.in);
        Set<String> builtins = new HashSet<>();
        builtins.add("echo");
        builtins.add("exit");
        builtins.add("type");

        while(true){
            String input = scanner.nextLine().trim();

            if(input.equals("exit 0")){
                System.exit(0);
            }

            if(input.isEmpty()){
                System.out.print("$ ");
                continue;
            }

            if(input.startsWith("echo ")){
                String[] words = input.split(" ",2);

                if (words.length > 1) {
                    System.out.println(words[1]); // Print the part after "echo"
                } else {
                    System.out.println(); // Just an empty echo
                }
                System.out.print("$ "); // Print the prompt for the next command
                continue;
            }

            if(input.startsWith("type: ")){
                String[] parts = input.split(" ",2);

                if(parts.length>1){
                    String command = parts[1];
                    if(builtins.contains(command)){
                        System.out.println(command + " is a shell builtin");
                    }
                    else{
                        System.out.println(command + ":not found");
                    }
                }
                else{
                    System.out.println("type: not found");
                }
                System.out.println("$ ");
                continue;
                }
            }
        

        System.out.println(input + ": command not found");
        System.out.print("$ ");
        }


    }
}
