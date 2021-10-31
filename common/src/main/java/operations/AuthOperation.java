package operations;

import files.SimpleFile;

import java.util.List;

public class AuthOperation extends Operation {

    private String username;
    private List<SimpleFile> userFilesList;
    private String message;

    public AuthOperation() {
    }

    private AuthOperation(OperationType type, String username) {
        super(type);
        this.username = username;
    }

    private AuthOperation(OperationType type, String username, List<SimpleFile> userSimpleFiles) {
        this(type, username);
        this.userFilesList = userSimpleFiles;
    }

    private AuthOperation(OperationType type, String username, String message) {
        this(type, username);
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<SimpleFile> getUserFilesList() {
        return userFilesList;
    }

    public static AuthOperation createAuthRequest(String username) {
        return new AuthOperation(OperationType.AUTH_REQUEST, username);
    }

    public static AuthOperation createAuthSuccess(String username, List<SimpleFile> userSimpleFiles) {
        return new AuthOperation(OperationType.AUTH_SUCCESS, username, userSimpleFiles);
    }

    public static AuthOperation createAuthFailed(String username, String message) {
        return new AuthOperation(OperationType.AUTH_FAILED, username, message);
    }
}
