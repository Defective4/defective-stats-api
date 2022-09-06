package net.defekt.stats.api;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import net.defekt.stats.api.http.MultipartRequest;
import net.defekt.stats.api.http.RequestPart;

public class StatsCollector {

    private final URL url;
    private final String token = Long.toString(System.currentTimeMillis() / 1000);
    private final String userID;

    private final Map<String, String> values = new HashMap<String, String>();
    private final Map<String, Long> numberValues = new HashMap<String, Long>();

    private final long startDate = System.currentTimeMillis();
    private final List<StatsUpdateCallback> callbacks = new ArrayList<>();

    public void addCallback(StatsUpdateCallback callback) {
        callbacks.add(callback);
    }

    public static String generateUserID() {
        Random rand = new Random();
        String id = "User";
        while (id.length() < 16)
            id += Integer.toString(rand.nextInt(10));
        return id;

    }

    public static String generateUserID(long seed) {
        Random rand = new Random(seed);
        String id = "User";
        while (id.length() < 16)
            id += Integer.toString(rand.nextInt(10));
        return id;
    }

    private final Timer autoUpdateTimer = new Timer("updateTimer", true);

    public StatsCollector(URL url, String userID) {
        this.url = url;
        this.userID = userID;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                update(false);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }));

        autoUpdateTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                try {
                    update(true);
                } catch (Exception ex2) {
                    ex2.printStackTrace();
                }
            }
        }, 1000 * 60 * 15, 1000 * 60 * 15);
    }

    private boolean paused = false;

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public void putStat(String name, Object value) {
        values.put(name, value.toString());
    }

    public String getStat(String name) {
        String stat = values.getOrDefault(name, "");
        values.put(name, stat);
        return stat;
    }

    public void putNumber(String name, long value) {
        numberValues.put(name, value);
    }

    public long getNumber(String name) {
        long val = numberValues.getOrDefault(name, (long) 0);
        numberValues.put(name, val);
        return val;
    }

    public void update(boolean continuous) throws IOException {
        if (paused) return;
        for (StatsUpdateCallback callback : callbacks) {
            boolean result = callback.updating(this);
            if (!result) return;
        }
        MultipartRequest req = new MultipartRequest(url);
        req.addPart("userID", new RequestPart(userID.getBytes(), null));
        req.addPart("token", new RequestPart(token.getBytes(), null));

        JsonObject obj = new JsonObject();
        JsonObject client = new JsonObject();

        client.addProperty("token", token);
        client.addProperty("userID", userID);
        client.addProperty("useTime", (System.currentTimeMillis() - startDate) / 1000);
        client.addProperty("continuous", continuous);

        obj.add("clientInfo", client);

        for (Entry<String, String> entry : values.entrySet()) {
            obj.addProperty(entry.getKey(), entry.getValue());
        }

        for (Entry<String, Long> entry : numberValues.entrySet()) {
            obj.addProperty(entry.getKey(), entry.getValue());
        }

        req.addPart("data",
                new RequestPart(new GsonBuilder().setPrettyPrinting().create().toJson(obj).getBytes(), "data.json"));

        req.send();
    }

    public boolean isPaused() {
        return paused;
    }
}
