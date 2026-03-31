import java.io.*;
import java.util.Scanner;

public class FileHandlingUtility {
    private static final String FILE_NAME = "example.txt";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n===== FILE HANDLING UTILITY =====");
            System.out.println("1. Write to File");
            System.out.println("2. Read from File");
            System.out.println("3. Append/Modify File");
            System.out.println("4. Exit");
            System.out.print("Choose an option: ");

            int choice = sc.nextInt();
            sc.nextLine(); 

            switch (choice) {
                case 1:
                    System.out.print("Enter text to write into the file: ");
                    String writeText = sc.nextLine();
                    writeToFile(writeText);
                    break;

                case 2:
                    readFromFile();
                    break;

                case 3:
                    System.out.print("Enter text to append to the file: ");
                    String appendText = sc.nextLine();
                    appendToFile(appendText);
                    break;

                case 4:
                    System.out.println("Exiting...");
                    return;

                default:
                    System.out.println("Invalid choice. Try again!");
            }
        }
    }

    // Write to file (overwrite)
    public static void writeToFile(String text) {
        try (FileWriter writer = new FileWriter(FILE_NAME)) {
            writer.write(text);
            System.out.println("Successfully written to the file!");
        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        }
    }

    // Read from file
    public static void readFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            System.out.println("\n--- File Content ---");
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("--------------------");
        } catch (IOException e) {
            System.out.println("Error reading from file: " + e.getMessage());
        }
    }

    // Append/Modify file
    public static void appendToFile(String text) {
        try (FileWriter writer = new FileWriter(FILE_NAME, true)) {
            writer.write("\n" + text);
            System.out.println("Successfully appended to the file!");
        } catch (IOException e) {
            System.out.println("Error appending to file: " + e.getMessage());
        }
    }
}