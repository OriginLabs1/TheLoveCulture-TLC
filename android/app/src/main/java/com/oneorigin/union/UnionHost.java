package com.oneorigin.union;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * The only bridge from the web surface to Android.
 *
 * It exposes an encrypted secret store, a share sheet, safe external links,
 * theme colour updates and writes into Downloads. It exposes no contacts,
 * no arbitrary intents and no raw filesystem.
 *
 * Secrets are encrypted with an AES-256-GCM key held by the Android Keystore,
 * so a copied SharedPreferences file is useless on another device.
 */
public class UnionHost {

    private static final String KEY_ALIAS = "union.secrets.v1";
    private static final String PREFS = "union.secure";
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String VERSION = "0.0.1";

    private final Activity activity;
    private final SharedPreferences prefs;
    private SecretKey key;

    UnionHost(Activity activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            this.key = loadOrCreateKey();
        } catch (Exception error) {
            this.key = null;
        }
    }

    private static SecretKey loadOrCreateKey() throws Exception {
        KeyStore store = KeyStore.getInstance(KEYSTORE);
        store.load(null);
        if (store.containsAlias(KEY_ALIAS)) {
            return (SecretKey) store.getKey(KEY_ALIAS, null);
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        generator.init(
            new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        );
        return generator.generateKey();
    }

    @JavascriptInterface
    public boolean secretSet(String name, String value) {
        if (key == null || name == null || value == null) return false;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] packed = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, packed, 0, iv.length);
            System.arraycopy(encrypted, 0, packed, iv.length, encrypted.length);
            prefs.edit().putString(name, Base64.encodeToString(packed, Base64.NO_WRAP)).apply();
            return true;
        } catch (Exception error) {
            return false;
        }
    }

    @JavascriptInterface
    public String secretGet(String name) {
        if (key == null || name == null) return null;
        String stored = prefs.getString(name, null);
        if (stored == null) return null;
        try {
            byte[] packed = Base64.decode(stored, Base64.NO_WRAP);
            if (packed.length < 13) return null;
            byte[] iv = new byte[12];
            System.arraycopy(packed, 0, iv, 0, 12);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] plain = cipher.doFinal(packed, 12, packed.length - 12);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception error) {
            return null;
        }
    }

    @JavascriptInterface
    public boolean secretDelete(String name) {
        if (name == null) return false;
        prefs.edit().remove(name).apply();
        return true;
    }

    @JavascriptInterface
    public String info() {
        return "{\"platform\":\"android-webview\",\"native\":true,"
            + "\"secureStore\":" + (key != null) + ","
            + "\"version\":\"" + VERSION + "\","
            + "\"sdk\":" + Build.VERSION.SDK_INT + "}";
    }

    @JavascriptInterface
    public void share(String text) {
        final String value = text == null ? "" : text;
        activity.runOnUiThread(() -> {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, value);
            Intent chooser = Intent.createChooser(send, "Share from Union");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                activity.startActivity(chooser);
            } catch (Exception error) {
                copy(value);
                Toast.makeText(activity, "Copied instead.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @JavascriptInterface
    public void copy(String text) {
        final String value = text == null ? "" : text;
        activity.runOnUiThread(() -> {
            ClipboardManager manager =
                (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (manager != null) {
                manager.setPrimaryClip(ClipData.newPlainText("Union", value));
            }
        });
    }

    @JavascriptInterface
    public boolean openExternal(String url) {
        if (url == null) return false;
        try {
            Uri uri = Uri.parse(url);
            if (!"https".equals(uri.getScheme())) return false;
            if (uri.getHost() == null) return false;
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            return true;
        } catch (Exception error) {
            return false;
        }
    }

    @JavascriptInterface
    public void setTheme(String color) {
        int parsed;
        try {
            parsed = Color.parseColor(color);
        } catch (Exception error) {
            return;
        }
        final int value = parsed;
        activity.runOnUiThread(() -> {
            activity.getWindow().setStatusBarColor(value);
            activity.getWindow().setNavigationBarColor(value);
        });
    }

    @JavascriptInterface
    public void exit() {
        activity.runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.finishAndRemoveTask();
            } else {
                activity.finish();
            }
        });
    }

    /**
     * Write a backup, proof copy or package into Downloads/Union and return a
     * human-readable location. No storage permission is required: MediaStore
     * on Android 10+, app-scoped external storage on Android 9.
     */
    @JavascriptInterface
    public String saveFile(String name, String mime, String base64) {
        byte[] bytes;
        try {
            bytes = Base64.decode(base64 == null ? "" : base64, Base64.DEFAULT);
        } catch (Exception error) {
            throw new IllegalArgumentException("The file is not valid base64");
        }
        String safe = safeName(name);
        String type = safeMime(mime);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = activity.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, safe);
            values.put(MediaStore.MediaColumns.MIME_TYPE, type);
            values.put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/Union"
            );
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IllegalStateException("Could not create a Downloads entry");
            try {
                OutputStream out = resolver.openOutputStream(uri);
                if (out == null) throw new IllegalStateException("Could not open the file");
                try {
                    out.write(bytes);
                } finally {
                    out.close();
                }
                values.clear();
                values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                resolver.update(uri, values, null, null);
            } catch (Exception error) {
                resolver.delete(uri, null, null);
                throw new IllegalStateException("Could not write the file: " + error.getMessage());
            }
            return "Downloads/Union/" + safe;
        }

        File dir = new File(activity.getExternalFilesDir(null), "Union");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Could not create the download directory");
        }
        File file = new File(dir, safe);
        try {
            FileOutputStream out = new FileOutputStream(file);
            try {
                out.write(bytes);
            } finally {
                out.close();
            }
        } catch (Exception error) {
            throw new IllegalStateException("Could not write the file: " + error.getMessage());
        }
        return file.getAbsolutePath();
    }

    private static String safeName(String name) {
        String value = name == null ? "union-file" : name.trim();
        value = value.replaceAll("[^A-Za-z0-9._ -]", "_");
        if (value.isEmpty()) value = "union-file";
        if (value.length() > 120) value = value.substring(value.length() - 120);
        return value;
    }

    private static String safeMime(String mime) {
        if (mime == null || mime.isEmpty()) return "application/octet-stream";
        String value = mime.toLowerCase(Locale.US);
        if (!value.matches("[a-z0-9.+-]+/[a-z0-9.+-]+")) return "application/octet-stream";
        return value;
    }
}
