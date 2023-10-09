import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketNetworkHandler implements NetworkHandler {
  private ServerSocket serverSocket;
  private Socket clientSocket;
  private PrintWriter out;
  private BufferedReader in;
  // For testing
  private boolean isForTested;
  private String testResponse;

  // constructor
  public SocketNetworkHandler(boolean isForTested) {
    this.isForTested = isForTested;
  }

  @Override
  public void initializeServer(int portNumber) {
    try {
      serverSocket = new ServerSocket(portNumber);
      System.out.println("Server listening on port " + portNumber + " " + serverSocket);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public Socket acceptIncomingClient() {
    try {
      return serverSocket.accept();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public String waitForDataFromClient(Socket clientSocket) {
    StringBuilder requestBuilder = new StringBuilder();

    try {
      InputStream input = clientSocket.getInputStream();
      BufferedReader in = new BufferedReader(new InputStreamReader(input));

      String line;
      int contentLength = 0;
      boolean isHeader = true;
      while (isHeader && (line = in.readLine()) != null) {
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

      if (contentLength > 0) {
        char[] bodyChars = new char[contentLength];
        in.read(bodyChars, 0, contentLength);
        requestBuilder.append(bodyChars);
      }

      return requestBuilder.toString();

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public void sendResponseToClient(String response, Socket clientSocket) {
    try {
      out = new PrintWriter(clientSocket.getOutputStream(), true);
      out.println(response);
      out.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public String sendDataToServer(String serverName, int portNumber, String data) {
    try {
      out = new PrintWriter(clientSocket.getOutputStream(), true);
      out.println(data);
      out.flush();
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

  @Override
  public String receiveDataFromServer(String serverName, int portNumber, String request) {
    if (isForTested) {
      return testResponse;
    }
    try {
      out = new PrintWriter(clientSocket.getOutputStream(), true);
      out.println(request);
      out.flush();
      return readServerResponse();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    } finally {
      closeResources();
    }
  }

  @Override
  public int initializeClientSocket(String serverName, int portNumber) {
    if (isForTested) {
      return 0;
    }
    try {
      this.closeResources();
      clientSocket = new Socket(serverName, portNumber);
      out = new PrintWriter(clientSocket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      String clockLine = in.readLine();
      System.out.println("Clock line: " + clockLine);
      if (clockLine != null && clockLine.startsWith("LamportClock: ")) {
        return Integer.parseInt(clockLine.split(":")[1].trim());
      } else {
        throw new IOException("Error while initializing client socket.");
      }
    } catch (IOException e) {
      System.out.println("Error while initializing client socket: " + e.getMessage());
      e.printStackTrace();
      closeResources();
      return -1;
    }
  }

  private String readServerResponse() throws IOException {
    StringBuilder responseBuilder = new StringBuilder();
    String line;
    int contentLength = 0;
    boolean isHeader = true;
    in.readLine();
    while (isHeader && (line = in.readLine()) != null) {
      if (line.startsWith("Content-Length: ")) {
        contentLength = Integer.parseInt(line.split(":")[1].trim());
      }

      responseBuilder.append(line).append("\r\n");

      if (line.isEmpty()) {
        isHeader = false;
      }
    }

    char[] bodyChars = new char[contentLength];
    in.read(bodyChars, 0, contentLength);
    responseBuilder.append(bodyChars);
    return responseBuilder.toString();

  }

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

  @Override
  public String handleTestRequest(String serverName, int portNumber, String request) {
    this.testResponse = request;
    return receiveDataFromServer(serverName, portNumber, request);
  }

  @Override
  public boolean checkClientSocketIsClosed() {
    return clientSocket == null;
  }
}
