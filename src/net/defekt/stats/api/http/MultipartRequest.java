package net.defekt.stats.api.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class MultipartRequest {

    private final URL url;
    private final Map<String, RequestPart> parts = new HashMap<>();

    public MultipartRequest(URL url) {
        this.url = url;
    }

    public void addPart(String name, RequestPart part) {
        parts.put(name, part);
    }

    public void removePart(String name) {
        parts.remove(name);
    }

    public void send() throws IOException {

        String boundary = Long.toString(System.currentTimeMillis());

        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("User-Agent", "Java");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        OutputStream os = connection.getOutputStream();
        PrintWriter pw = new PrintWriter(os);
        for (Entry<String, RequestPart> entry : parts.entrySet()) {
            RequestPart part = entry.getValue();
            pw.println("--" + boundary);
            pw.println("Content-Disposition: form-data; name=\"" + entry.getKey() + "\""
                    + (part.getFilename() == null ? "" : "; filename=\"" + part.getFilename() + "\""));
            pw.println();
            pw.flush();
            os.write(part.getData());
            os.flush();
            pw.println();
        }

        pw.println("--" + boundary + "--");
        pw.flush();
        os.flush();

        connection.getInputStream().close();
    }
}
