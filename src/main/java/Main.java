import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");

        // Uncomment this block to pass the first stage

        Socket clientSocket;

        try(ExecutorService es = Executors.newFixedThreadPool(8);
            ServerSocket serverSocket = new ServerSocket(4221)) {
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
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

class ClientProcessor implements Runnable {

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final Socket client;

    public ClientProcessor(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        System.out.println("accepted new connection");

        try {
            String response = parseRequest(client);

            client.getOutputStream().write(response.getBytes());
            client.close();
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
    }

    private static String parseRequest(Socket clientSocket) throws IOException {
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
            var arr = header.split(": ");
            headers.put(arr[0], arr[1]);
        }

        String response;
        Matcher matcher;

        if (target.equals("/")) {
            response = "HTTP/1.1 200 OK\r\n\r\n";
        } else if ((matcher = Pattern.compile("/echo/(\\w*)").matcher(target)).matches()) {
            String res = matcher.group(1);
            response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + res.length() + "\r\n\r\n" + res;
        } else if (target.equals("/user-agent")) {
            String res = headers.get("User-Agent");
            response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + res.length() + "\r\n\r\n" + res;
        } else {
            response = "HTTP/1.1 404 Not Found\r\n\r\n";
        }
        return response;
    }
}