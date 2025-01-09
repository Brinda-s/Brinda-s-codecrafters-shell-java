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

            if(input.startsWith("echo")){
                String[] words = str.split(" ");

                if(words.length >1){
                    for(int i=1;i<words.length;i++){
                        System.out.print(words[i] + " ");
                    }
                }
                System.out.println();
            }
        

        System.out.println(input + ": command not found");
        System.out.print("$ ");
        }


    }
}
