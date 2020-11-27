package org.mltooling.core.utils;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class CryptoUtils {

  // ================ Constants =========================================== //
  private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");

  // ================ Members ============================================= //

  // ================ Constructors & Main ================================= //
  private CryptoUtils() {}

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //
  public static String getEncodedSha1Sum(String key) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA1");
      md.update(key.getBytes(UTF_8_CHARSET));

      // taken from: http://stackoverflow.com/questions/4895523/java-string-to-sha1
      byte[] b = md.digest();
      String result = "";
      for (int i = 0; i < b.length; i++) {
        result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
      }
      return result.toUpperCase();
    } catch (NoSuchAlgorithmException e) {
      // handle error case
    }
    return key;
  }

  public static String md5(String s) {
    try {
      MessageDigest m = MessageDigest.getInstance("MD5");
      m.update(s.getBytes(UTF_8_CHARSET));
      byte[] digest = m.digest();
      BigInteger bigInt = new BigInteger(1, digest);
      return bigInt.toString(16);
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError();
    }
  }

  public static String encode(final byte[] rawData) {
    // Base64.getEncoder().withoutPadding().encodeToString(someByteArray);
    return Base64.getEncoder().encodeToString(rawData).replaceAll("\r\n", "").replaceAll("\n", "");
  }

  public static byte[] decode(final String encodedData) {
    return Base64.getDecoder().decode(encodedData);
  }

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
