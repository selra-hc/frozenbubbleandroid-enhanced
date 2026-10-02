/*
 *                 [[ Frozen-Bubble ]]
 *
 * Copyright (c) 2000-2003 Guillaume Cottenceau.
 * Java sourcecode - Copyright (c) 2003 Glenn Sanson.
 * Additional source - Copyright (c) 2013 Eric Fortin.
 *
 * This code is distributed under the GNU General Public License
 */

package org.cb.enhancedfrozenbubble;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

/**
 * The single mutable level catalog used by both gameplay and the in-app
 * level editor. It lives in app-private storage and is seeded once from
 * the read-only {@code assets/levels.txt} bundled in the APK; from then on
 * every read/write goes through the writable copy, so built-in and
 * user-created levels are indistinguishable entries in the same list.
 */
public class LevelStore {

  private static final String FILENAME = "levels.txt";

  public static void ensureSeeded(Context ctx) throws IOException {
    File dest = new File(ctx.getFilesDir(), FILENAME);
    if (!dest.exists()) {
      writeBytes(ctx, readBuiltinLevels(ctx));
    }
  }

  public static void resetToDefaults(Context ctx) throws IOException {
    writeBytes(ctx, readBuiltinLevels(ctx));
  }

  /**
   * Returns the original seed content of the level at {@code index}, i.e.
   * what it looked like before any in-app edits, or {@code null} if
   * {@code index} doesn't correspond to a seeded level (e.g. it's a level
   * added in-app after the initial seed, which has no default to revert
   * to). This is index-based: if levels have been reordered since the
   * seed, "default" means whatever the seed had at that same position.
   */
  public static byte[][] getDefaultLevel(Context ctx, int index) throws IOException {
    List<byte[][]> defaults =
        new LevelManager(readBuiltinLevels(ctx), 0).getAllLevels();
    if (index < 0 || index >= defaults.size()) {
      return null;
    }
    return LevelManager.copyLevel(defaults.get(index));
  }

  public static byte[] loadRawBytes(Context ctx) throws IOException {
    ensureSeeded(ctx);
    return readRaw(ctx);
  }

  public static LevelManager load(Context ctx) throws IOException {
    return new LevelManager(loadRawBytes(ctx), 0);
  }

  public static List<byte[][]> loadLevelList(Context ctx) throws IOException {
    return load(ctx).getAllLevels();
  }

  public static List<byte[][]> mutableCopy(Context ctx) throws IOException {
    List<byte[][]> stored = loadLevelList(ctx);
    List<byte[][]> copy = new ArrayList<byte[][]>(stored.size());
    for (byte[][] level : stored) {
      copy.add(LevelManager.copyLevel(level));
    }
    return copy;
  }

  public static void save(Context ctx, List<byte[][]> levels) throws IOException {
    writeBytes(ctx, LevelManager.serializeLevels(levels));
  }

  private static void writeBytes(Context ctx, byte[] data) throws IOException {
    File dir = ctx.getFilesDir();
    File dest = new File(dir, FILENAME);
    File temp = new File(dir, FILENAME + ".tmp");
    FileOutputStream fos = new FileOutputStream(temp);
    try {
      fos.write(data);
      fos.getFD().sync();
    } finally {
      fos.close();
    }
    if (dest.exists() && !dest.delete()) {
      throw new IOException("Could not replace " + FILENAME);
    }
    if (!temp.renameTo(dest)) {
      throw new IOException("Could not rename temp file to " + FILENAME);
    }
  }

  private static byte[] readBuiltinLevels(Context ctx) throws IOException {
    InputStream is = ctx.getAssets().open("levels.txt");
    try {
      return readAll(is);
    } finally {
      is.close();
    }
  }

  private static byte[] readRaw(Context ctx) throws IOException {
    File file = new File(ctx.getFilesDir(), FILENAME);
    if (!file.exists()) {
      return new byte[0];
    }
    FileInputStream fis = new FileInputStream(file);
    try {
      return readAll(fis);
    } finally {
      fis.close();
    }
  }

  private static byte[] readAll(InputStream is) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] chunk = new byte[4096];
    int read;
    while ((read = is.read(chunk)) != -1) {
      buffer.write(chunk, 0, read);
    }
    return buffer.toByteArray();
  }
}
