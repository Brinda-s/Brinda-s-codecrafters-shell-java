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
        

        System.out.println(input + ": command not found");
        System.out.print("$ ");
        }


    }
}
