package com.example.nodewatcher.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class Config
{

  public final static TimeUnit TIME_UNIT = TimeUnit.SECONDS;

  public final static int HTTP_PORT = 4500;

  private static final String SECRET_KEY ="Hellow";

  public static SecretKey generateKey()
  {
    try
    {
      KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");

      keyGenerator.init(128);

      return keyGenerator.generateKey();

    }
    catch (Exception exception)
    {
      return  null;
    }
  }

  public static String encrypt(String plainText)
  {
    try
    {
      var cipher = Cipher.getInstance("AES");

      SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");

      cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

      var encryptedBytes = cipher.doFinal(plainText.getBytes());

      return Base64.getEncoder().encodeToString(encryptedBytes);
    }
    catch (Exception exception)
    {
      return plainText;
    }
  }

  public static String decrypt(String encryptedText)
  {
    try
    {
      var cipher = Cipher.getInstance("AES");

      var secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");

      cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

      var decodedBytes = Base64.getDecoder().decode(encryptedText); // Decode from Base64

      var decryptedBytes = cipher.doFinal(decodedBytes);

      return new String(decryptedBytes); // Convert decrypted bytes to string
    }
    catch (Exception exception)
    {
      return encryptedText;
    }
  }

}
