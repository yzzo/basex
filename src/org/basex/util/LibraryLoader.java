package org.basex.util;

import org.basex.BaseX;

/**
 * Utility class to load shared libraries.
 *
 * @author Workgroup DBIS, University of Konstanz 2009, ISC License
 * @author Alexander Holupirek
 */
public final class LibraryLoader {
  /** Name of spotlight extractor library. */
  public static final String SPOTEXLIBNAME = "deepfs_spotex";
  /** Name of joint storage library. */
  public static final String JSDBFSLIBNAME = "deepfs_jsdbfs";

  /** Spotlight library presence flag. */
  private static boolean spotexLoaded;
  /** Joint storage library presence flag. */
  private static boolean jsdbfsLoaded;

  /**
   * Load a native library from java.library.path.
   * @param libName name of the library to be loaded.
   */
  private static void loadLibrary(final String libName) {
    try {
      System.loadLibrary(libName);
      BaseX.debug("Loading library ... OK (" + libName + ").");

      if(libName.equals(SPOTEXLIBNAME)) { spotexLoaded = true; return; }
      if(libName.equals(JSDBFSLIBNAME)) { jsdbfsLoaded = true; return; }

    } catch(UnsatisfiedLinkError e) {
      BaseX.errln("Loading library failed (" + libName + ")." + e);
      BaseX.errln("-Djava.library.path is : '"
          + System.getProperty("java.library.path") + "'");
    }
  }

  /**
   * Load native library if not already present.
   * @param libName name of the library to be loaded.
   */
  public static void load(final String libName) {
    if(libName.equals(SPOTEXLIBNAME) && spotexLoaded) return;
    if(libName.equals(JSDBFSLIBNAME) && jsdbfsLoaded) return;

    loadLibrary(libName);
  }

  /** Default constructor disabled. */
  protected LibraryLoader() {
    throw new UnsupportedOperationException();
  }
}