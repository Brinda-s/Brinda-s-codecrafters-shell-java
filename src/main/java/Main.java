import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        
        System.out.print("$ ");

        Scanner scanner = new Scanner(System.in);
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
        

        System.out.println(input + ": command not found");
        System.out.print("$ ");
        }


    }
}
