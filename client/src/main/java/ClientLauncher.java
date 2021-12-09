import client.Client;

import java.io.*;

public class ClientLauncher {
    public static void main(String[] args) throws IOException {
        new Client("bananza").start();
    }
}
