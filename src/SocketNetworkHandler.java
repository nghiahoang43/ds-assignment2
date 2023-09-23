import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SocketNetworkHandler implements NetworkHandler {
  private ServerSocket serverSocket;
  private Socket clientSocket;
  private PrintWriter out;
  private BufferedReader in;

  // Initialize the server socket and start listening on the specified port
  @Override
  public void initializeServer(int portNumber) {
    try {
      serverSocket = new ServerSocket(portNumber);
      System.out.println("Server listening on port " + portNumber + " " + serverSocket);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Accept an incoming client connection
  @Override
  public Socket acceptIncomingClient() {
    try {
      return serverSocket.accept();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  // Wait for data from the client and read it
  @Override
  public String waitForDataFromClient(Socket clientSocket) {
    // Initialize a StringBuilder to store the client request.
    StringBuilder requestBuilder = new StringBuilder();

    try {
      // Create an InputStream to read from the client socket.
      InputStream input = clientSocket.getInputStream();
      // Wrap the InputStream in a BufferedReader for easier reading.
      BufferedReader in = new BufferedReader(new InputStreamReader(input));

      String line;
      int contentLength = 0;
      boolean isHeader = true;

      // Loop to read the header lines from the client request.
      while (isHeader && (line = in.readLine()) != null) {
        // If the line starts with "Content-Length:", parse and store the content
        // length.
        if (line.startsWith("Content-Length: ")) {
          contentLength = Integer.parseInt(line.split(":")[1].trim());
        }

        // Append each line followed by a CRLF to the request builder.
        requestBuilder.append(line).append("\r\n");

        // A blank line indicates the end of the headers.
        if (line.isEmpty()) {
          isHeader = false;
        }
      }

      // If the request has a body, read it.
      if (contentLength > 0) {
        char[] bodyChars = new char[contentLength];
        in.read(bodyChars, 0, contentLength);
        requestBuilder.append(bodyChars);
      }

      // Return the complete request string.
      return requestBuilder.toString();

    } catch (Exception e) {
      // Print the stack trace for debugging and return null to indicate an error.
      e.printStackTrace();
      return null;
    }
  }

  // Send a response back to the client
  @Override
  public void sendResponseToClient(String response, Socket clientSocket) {
    try {
      out = new PrintWriter(clientSocket.getOutputStream(), true);
      out.println(response);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Send data to the server and wait for the response
  @Override
  public String sendDataToServer(String serverName, int portNumber, String data) {
    System.out.println("Sending data to server...");
    try {
      initializeClientSocket(serverName, portNumber);
      out.println(data);
      System.out.println("Waiting for response from server..." + data);
      return readServerResponse();
    } catch (IOException e) {
      System.out.println("Error while connecting to the server: " + e.getMessage());
      e.printStackTrace();
      return null;
    } finally {
      closeResources();
    }
  }

  // Send a request to the server and receive the response
  @Override
  public String receiveDataFromServer(String serverName, int portNumber, String request) {
    try {
      initializeClientSocket(serverName, portNumber);
      out.println(request);
      return readServerResponse();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    } finally {
      closeResources();
    }
  }

  // Initialize client-side socket and IO streams
  private void initializeClientSocket(String serverName, int portNumber) throws IOException {
    if (clientSocket == null || clientSocket.isClosed()) {
      clientSocket = new Socket(serverName, portNumber);
      out = new PrintWriter(clientSocket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }
  }

  // Read the response from the server
  private String readServerResponse() throws IOException {
    StringBuilder response = new StringBuilder();
    String line;

    // Append '{' to denote the start of the JSON object
    response.append('{');

    while ((line = in.readLine()) != null && !line.isEmpty()) {
      // Check if the line contains a JSON object or array
      if (line.contains("{") || line.contains("[")) {
        // If so, consider the part before the '{' or '[' as the key
        int index = Math.min(line.indexOf('{') != -1 ? line.indexOf('{') : Integer.MAX_VALUE,
            line.indexOf('[') != -1 ? line.indexOf('[') : Integer.MAX_VALUE);
        String key = line.substring(0, index).split(":")[0].trim();
        String value = line.substring(index).trim();

        response.append("\"").append(key).append("\"").append(": ").append(value);
      } else {
        // Otherwise, treat it as a regular key-value pair
        String[] parts = line.split(": ", 2);
        if (parts.length == 2) {
          String key = parts[0].trim();
          String value = parts[1].trim();

          // Enclose keys and values in double quotes
          response.append("\"").append(key).append("\"").append(": ").append("\"").append(value).append("\"");
        }
      }

      response.append(",");
    }

    // Remove trailing comma, if any
    if (response.charAt(response.length() - 1) == ',') {
      response.deleteCharAt(response.length() - 1);
    }

    // Append '}' to denote the end of the JSON object
    response.append('}');

    System.out.println("Response from server: " + response);
    return response.toString();

  }

  // Close all open resources
  @Override
  public void closeResources() {
    try {
      if (in != null)
        in.close();
      if (out != null)
        out.close();
      if (clientSocket != null)
        clientSocket.close();
      if (serverSocket != null)
        serverSocket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
