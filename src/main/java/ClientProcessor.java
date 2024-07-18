import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientProcessor implements Runnable {

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final Socket client;

    public ClientProcessor(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        System.out.println("accepted new connection");

        try {
            byte[] response = parseRequest(client);

            client.getOutputStream().write(response);
            client.close();
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
    }

    private static byte[] parseRequest(Socket clientSocket) throws IOException {
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
        byte[] nonStringBody = new byte[0];
        Matcher matcher;

        if (target.equals("/")) {
            response = "HTTP/1.1 200 OK\r\n\r\n";
        } else if ((matcher = Pattern.compile("/echo/(\\w*)").matcher(target)).matches()) {
            String res = matcher.group(1);
            response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + res.length() + "\r\n\r\n" + res;
        } else if (target.equals("/user-agent")) {
            String res = headers.get("User-Agent");
            response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + res.length() + "\r\n\r\n" + res;
        } else if ((matcher = Pattern.compile("/files/(\\w+)").matcher(target)).matches()) {
            String fileName = matcher.group(1);
            Path file = FileManager.getInstance().getFile(fileName);
            if (!file.toFile().exists()) {
                response = "HTTP/1.1 404 Not Found\r\n\r\n";
            } else {
                response = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " + file.toFile().length() + "\r\n\r\n";
                nonStringBody = Files.readAllBytes(file);
            }
        } else {
            response = "HTTP/1.1 404 Not Found\r\n\r\n";
        }

        byte[] responseBytes = response.getBytes();
        byte[] toReturn = new byte[responseBytes.length + nonStringBody.length];
        System.arraycopy(responseBytes, 0, toReturn, 0, responseBytes.length);
        System.arraycopy(nonStringBody, 0, toReturn, responseBytes.length, nonStringBody.length);

        return toReturn;
    }
}
