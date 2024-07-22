package demo.http.server.java;

public class ByteArrayWrapper {

    private byte[] internal;
    private boolean completed = false;

    public ByteArrayWrapper(byte[] internal) {
        this.internal = internal;
    }

    public byte[] getInternal() {
        return internal;
    }

    public void setInternal(byte[] internal) {
        this.internal = internal;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void markCompleted() {
        completed = true;
    }
}
