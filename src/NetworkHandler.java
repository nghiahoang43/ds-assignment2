import java.net.Socket;

public interface NetworkHandler {

    // ------- Server-side methods -------

    /**
     * Initializes the server socket and starts listening on the given port.
     *
     * @param portNumber Port number to listen on.
     */
    void initializeServer(int portNumber);

    /**
     * Accepts an incoming client connection.
     *
     * @return The accepted client Socket.
     */
    Socket acceptIncomingClient();

    /**
     * Waits for data from a client.
     *
     * @param clientSocket The socket through which the client is connected.
     * @return Data received from the client.
     */
    String waitForDataFromClient(Socket clientSocket);

    /**
     * Sends a response back to the client.
     *
     * @param response     The response string to be sent.
     * @param clientSocket The socket through which the client is connected.
     */
    void sendResponseToClient(String response, Socket clientSocket);

    // ------- Client-side methods -------

    /**
     * Sends data to a specified server.
     *
     * @param serverName  The name of the server to which data will be sent.
     * @param portNumber  The port number on which the server is listening.
     * @param data        The data to be sent.
     * @return Response from the server.
     */
    String sendDataToServer(String serverName, int portNumber, String data);

    /**
     * Receives data from a specified server.
     *
     * @param serverName  The name of the server from which data will be received.
     * @param portNumber  The port number on which the server is listening.
     * @param request     Request to be sent to the server.
     * @return Data received from the server.
     */
    String receiveDataFromServer(String serverName, int portNumber, String request);

    /**
     * Closes any open resources.
     */
    void closeResources();
}
