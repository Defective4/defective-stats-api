package net.defekt.stats.api.http;

public class RequestPart {
    private final byte[] data;
    private final String filename;

    public RequestPart(byte[] data, String filename) {
        super();
        this.data = data;
        this.filename = filename;
    }

    public byte[] getData() {
        return data;
    }

    public String getFilename() {
        return filename;
    }
}
