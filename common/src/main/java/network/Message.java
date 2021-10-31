package network;

import watcher.Operation;

import java.util.Arrays;

public class Message {

    private Operation operation;
    private long position;
    private byte[] file;

    public Message() {
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public byte[] getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }

    @Override
    public String toString() {
        return "Message{" +
                "operation=" + operation +
                ", position=" + position +
                ", file=" + Arrays.toString(file) +
                '}';
    }
}
