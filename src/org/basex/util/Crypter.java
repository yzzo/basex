package org.basex.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 * De- and encryption of passwords.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Andreas Weiler
 */
public final class Crypter {
  /** Bytes for de- and encryption. */
  private static final byte[] SALT = { (byte) 0xc9, (byte) 0xc9, (byte) 0xc9,
      (byte) 0xc9, (byte) 0xc9, (byte) 0xc9, (byte) 0xc9, (byte) 0xc9};
  /** De- and encryption key. */
  private static final String KEY = "BaseX";
  /** Encryption cipher. */
  private Cipher encrypt;
  /** Decryption cipher. */
  private Cipher decrypt;

  /**
   * Standard constructor.
   */
  public Crypter() {
    try {
      final PBEParameterSpec ps = new PBEParameterSpec(SALT, 20);
      final SecretKeyFactory kf =
        SecretKeyFactory.getInstance("PBEWithMD5AndDES");
      final SecretKey k = kf.generateSecret(new PBEKeySpec(KEY.toCharArray()));
      encrypt = Cipher.getInstance("PBEWithMD5AndDES/CBC/PKCS5Padding");
      encrypt.init(Cipher.ENCRYPT_MODE, k, ps);
      decrypt = Cipher.getInstance("PBEWithMD5AndDES/CBC/PKCS5Padding");
      decrypt.init(Cipher.DECRYPT_MODE, k, ps);
    } catch(final Exception ex) {
      throw new SecurityException("Could not initialize CryptoLibrary: "
          + ex.getMessage());
    }
  }

  /**
   * Encrypts a token.
   * @param tok token to be encrypted
   * @return encrypted string.
   */
  public synchronized byte[] encrypt(final byte[] tok) {
    try {
      return encrypt.doFinal(tok);
    } catch(final Exception ex) {
      throw new SecurityException("Could not encrypt: " + ex.getMessage());
    }
  }

  /**
   * Decrypts a string.
   * @param str Description of the Parameter
   * @return decrypted token.
   */
  public synchronized byte[] decrypt(final byte[] str) {
    try {
      return decrypt.doFinal(str);
    } catch(final Exception ex) {
      throw new SecurityException("Could not decrypt: " + ex.getMessage());
    }
  }
}