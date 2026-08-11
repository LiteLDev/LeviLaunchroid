package org.levimc.pojavcontrols;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AtomicFile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ControlRepository {
    private static final String PREFS = "pojav_controls";
    private static final String ACTIVE = "active_profile";
    private static final String DEFAULT = "default";
    private static final int MAX_PROFILE_BYTES = 4 * 1024 * 1024;

    private final Context context;
    private final File directory;
    private final SharedPreferences preferences;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    ControlRepository(Context context) {
        this.context = context.getApplicationContext();
        directory = new File(this.context.getFilesDir(), "pojav_controls");
        preferences = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        ensureDefault();
    }

    String activeName() {
        String value = sanitizeName(preferences.getString(ACTIVE, DEFAULT));
        return value.isEmpty() ? DEFAULT : value;
    }

    void setActive(String name) {
        String safe = sanitizeName(name);
        if (!safe.isEmpty()) preferences.edit().putString(ACTIVE, safe).apply();
    }

    List<String> listProfiles() {
        ensureDefault();
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        ArrayList<String> result = new ArrayList<>();
        if (files != null) {
            for (File file : files) result.add(file.getName().substring(0, file.getName().length() - 5));
        }
        Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    CustomControls loadActive() {
        return load(activeName());
    }

    CustomControls load(String name) {
        ensureDefault();
        File file = profileFile(name);
        if (!file.isFile()) file = profileFile(DEFAULT);
        try (InputStream input = new FileInputStream(file)) {
            return readProfile(input);
        } catch (Exception ignored) {
            CustomControls controls = CustomControls.createDefault();
            save(DEFAULT, controls);
            return controls;
        }
    }

    void save(String name, CustomControls controls) {
        String safe = sanitizeName(name);
        if (safe.isEmpty()) throw new IllegalArgumentException("Invalid profile name");
        controls.normalize();
        ensureDirectory();
        byte[] data = gson.toJson(controls).getBytes(StandardCharsets.UTF_8);
        AtomicFile target = new AtomicFile(profileFile(safe));
        FileOutputStream output = null;
        try {
            output = target.startWrite();
            output.write(data);
            output.flush();
            target.finishWrite(output);
        } catch (IOException exception) {
            if (output != null) target.failWrite(output);
            throw new IllegalStateException(exception);
        }
    }

    String importProfile(String requestedName, InputStream input) throws IOException {
        CustomControls controls = readProfile(input);
        String base = sanitizeName(requestedName);
        if (base.isEmpty()) base = "imported";
        String available = base;
        int suffix = 2;
        while (profileFile(available).exists()) available = base + "-" + suffix++;
        save(available, controls);
        return available;
    }

    void exportProfile(String name, OutputStream output) throws IOException {
        CustomControls controls = load(name);
        byte[] data = gson.toJson(controls).getBytes(StandardCharsets.UTF_8);
        output.write(data);
        output.flush();
    }

    boolean delete(String name) {
        String safe = sanitizeName(name);
        if (safe.isEmpty() || DEFAULT.equals(safe)) return false;
        boolean deleted = profileFile(safe).delete();
        if (safe.equals(activeName())) setActive(DEFAULT);
        return deleted;
    }

    String duplicate(String sourceName, String requestedName) {
        String safe = sanitizeName(requestedName);
        if (safe.isEmpty()) throw new IllegalArgumentException("Invalid profile name");
        save(safe, load(sourceName));
        return safe;
    }

    private CustomControls readProfile(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > MAX_PROFILE_BYTES) throw new IOException("Invalid profile size");
            output.write(buffer, 0, read);
        }
        byte[] data = output.toByteArray();
        if (data.length == 0 || data.length > MAX_PROFILE_BYTES) throw new IOException("Invalid profile size");
        try {
            CustomControls controls = gson.fromJson(new String(data, StandardCharsets.UTF_8), CustomControls.class);
            if (controls == null) throw new IOException("Invalid profile");
            controls.normalize();
            return controls;
        } catch (JsonParseException exception) {
            throw new IOException("Invalid profile", exception);
        }
    }

    private void ensureDefault() {
        ensureDirectory();
        if (!profileFile(DEFAULT).isFile()) save(DEFAULT, CustomControls.createDefault());
    }

    private void ensureDirectory() {
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IllegalStateException("Unable to create controls directory");
    }

    private File profileFile(String name) {
        return new File(directory, sanitizeName(name) + ".json");
    }

    static String sanitizeName(String value) {
        if (value == null) return "";
        String safe = value.trim().replaceAll("[^A-Za-z0-9._ -]", "_");
        while (safe.startsWith(".")) safe = safe.substring(1);
        return safe.length() > 80 ? safe.substring(0, 80) : safe;
    }
}
