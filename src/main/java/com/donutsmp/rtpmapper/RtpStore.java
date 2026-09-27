package com.donutsmp.rtpmapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RtpStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final List<RtpPoint> points = new ArrayList<>();
    private int session = 1;

    public synchronized void add(double x, double y, double z, String dimension) {
        points.add(new RtpPoint(x, y, z, dimension, System.currentTimeMillis(), session));
    }

    public synchronized List<RtpPoint> all() { return Collections.unmodifiableList(new ArrayList<>(points)); }
    public synchronized List<RtpPoint> sessionPoints() {
        List<RtpPoint> out = new ArrayList<>();
        for (RtpPoint p : points) if (p.session() == session) out.add(p);
        return out;
    }
    public synchronized int size() { return points.size(); }
    public synchronized int sessionSize() { return sessionPoints().size(); }
    public synchronized int session() { return session; }
    public synchronized void newSession() { session++; }
    public synchronized void clear() { points.clear(); }

    public synchronized void save(MinecraftClient client) {
        try {
            Path path = dataPath(client);
            Files.createDirectories(path.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("session", session);
            JsonArray array = new JsonArray();
            for (RtpPoint p : points) {
                JsonObject o = new JsonObject();
                o.addProperty("x", p.x()); o.addProperty("y", p.y()); o.addProperty("z", p.z());
                o.addProperty("dimension", p.dimension()); o.addProperty("timestamp", p.timestamp());
                o.addProperty("session", p.session()); array.add(o);
            }
            Files.writeString(path, GSON.toJson(root), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) { }
    }

    public synchronized void load(MinecraftClient client) {
        try {
            Path path = dataPath(client);
            if (!Files.exists(path)) return;
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            session = root.has("session") ? root.get("session").getAsInt() : 1;
            points.clear();
            if (!root.has("points")) {
                // Older versions used a top-level array; current format is intentionally explicit.
            }
            JsonArray array = root.has("samples") ? root.getAsJsonArray("samples") : root.getAsJsonArray("points");
            if (array == null) return;
            for (var e : array) {
                JsonObject o = e.getAsJsonObject();
                points.add(new RtpPoint(o.get("x").getAsDouble(), o.get("y").getAsDouble(), o.get("z").getAsDouble(),
                        o.has("dimension") ? o.get("dimension").getAsString() : "unknown",
                        o.has("timestamp") ? o.get("timestamp").getAsLong() : 0L,
                        o.has("session") ? o.get("session").getAsInt() : 1));
            }
        } catch (Exception ignored) { }
    }

    private Path dataPath(MinecraftClient client) {
        return client.runDirectory.toPath().resolve("config/donutsmp_rtp_mapper/samples.json");
    }

    public synchronized void exportCsv(MinecraftClient client) throws IOException {
        Path path = client.runDirectory.toPath().resolve("donutsmp_rtp_mapper.csv");
        StringBuilder csv = new StringBuilder("x,y,z,dimension,timestamp,session\n");
        for (RtpPoint p : points) {
            csv.append(p.x()).append(',').append(p.y()).append(',').append(p.z()).append(',')
                    .append('"').append(p.dimension().replace("\"", "\"\"")).append('"').append(',')
                    .append(p.timestamp()).append(',').append(p.session()).append('\n');
        }
        Files.writeString(path, csv.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
