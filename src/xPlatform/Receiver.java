package xPlatform;

import java.net.Socket;

/**
 * Created by Vladok on 21.09.2016.
 */
public class Receiver {
    private Socket socket;
    private String name;
    private boolean disconnected;
    public Receiver(Socket socket, String name) {
        this.socket = socket;
        this.name = name;
        disconnected=false;
    }
    public Socket getSocket() {
        return socket;
    }
    public String getName() {
        return name;
    }
    public void setDisconnected(boolean disconnected) {
        this.disconnected = disconnected;
    }
    public boolean isDisconnected() {
        return disconnected;
    }
}
