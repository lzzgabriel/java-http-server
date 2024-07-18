import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--directory")) {
                FileManager.initialize(args[i + 1]);
            }
        }
        Socket clientSocket;

        try(ExecutorService es = Executors.newFixedThreadPool(8);
            ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);

            while (true) {
                clientSocket = serverSocket.accept(); // Wait for connection from client.
                es.submit(new ClientProcessor(clientSocket));
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

}