import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");

        // Uncomment this block to pass the first stage

        ServerSocket serverSocket;
        Socket clientSocket;

        try {
            serverSocket = new ServerSocket(4221);
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            clientSocket = serverSocket.accept(); // Wait for connection from client.
            System.out.println("accepted new connection");

            var reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // request line
            String[] requestLine = reader.readLine().split(" ");
            String method = requestLine[0];
            String target = requestLine[1];
            String version = requestLine[2];

            // headers
            Map<String, String> headers = new HashMap<>();
            String header;
            while ((header = reader.readLine()) != null && !header.isBlank()) {
                var arr = header.split(" ");
                headers.put(arr[0], arr[1]);
            }

            String response;

            if (target.equals("/")) {
                response = "HTTP/1.1 200 OK\r\n\r\n";
            } else {
                response = "HTTP/1.1 404 Not Found\r\n\r\n";
            }

            clientSocket.getOutputStream().write(response.getBytes());
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

}
