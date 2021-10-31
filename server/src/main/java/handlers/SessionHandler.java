package handlers;

import files.UserFilesList;
import io.netty.channel.Channel;
import server.Server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SessionHandler {

    private final Channel channel;
    private final String username;
    private final Path userDirectory;
    private final Path userRecycleBinDirectory;
    private final UserFilesList userFiles;

    public SessionHandler(Channel channel, String username) {
        this.channel = channel;
        this.username = username;
        this.userDirectory = Path.of(Server.getWorkDirectory() + "\\" + username);
        this.userRecycleBinDirectory = Path.of(Server.getWorkDirectory() + "\\$recycled\\" + username);
        initUserDirs();
        this.userFiles = new UserFilesList(userDirectory);
    }

    private void initUserDirs() {
        try {
            if (!Files.exists(userDirectory)) Files.createDirectories(userDirectory);
            if (!Files.exists(userRecycleBinDirectory)) Files.createDirectories(userRecycleBinDirectory);
        } catch (IOException e) {
            System.out.println("Problem occurred while trying to init user's directories: " + e.getCause() + " | " + e.getMessage());
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public String getUsername() {
        return username;
    }

    public Path getUserDirectory() {
        return userDirectory;
    }

    public Path getUserRecycleBinDirectory() {
        return userRecycleBinDirectory;
    }

    public UserFilesList getUserFiles() {
        return userFiles;
    }
}
