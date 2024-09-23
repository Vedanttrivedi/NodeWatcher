package com.example.nodewatcher.tester;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class PassWordHashing
{
  public static String hashPassword(String password)
  {
    try
    {

      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      byte[] hashedBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));

      return Base64.getEncoder().encodeToString(hashedBytes);

    }
    catch (NoSuchAlgorithmException e)
    {

      throw new RuntimeException("Error while hashing password", e);

    }
  }

  public static void main(String[] args)
  {
    String password = "mySecurePassword123";

    String hashedPassword = hashPassword(password);

    System.out.println("Hashed Password: " + hashedPassword);
  }
}

