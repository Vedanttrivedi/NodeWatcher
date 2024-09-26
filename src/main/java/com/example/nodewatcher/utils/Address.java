package com.example.nodewatcher.utils;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;

public class Address
{
  public final static String PINGCHECK = "com.example.nodewatcher.PingChecker";

  public final static String DUMPDB = "com.example.nodewatcher.dumpDB";

  public final static String PLUGINDATASENDER = "com.example.nodewatcher.pluginDataSender";

  public final static String PUSHSOCKET = "tcp://localhost:4555";

  public final static String PULLSOCKET = "tcp://localhost:4556";

  public final static int MEMORYINTERVAL=20000;

  public final static int CPUNTERVAL=25000;

  public final static String ENCRYPTION_ALGORITHM="AES";



  private static SecretKey generateSecretKey()
  {
    try
    {

      KeyGenerator keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);

      keyGenerator.init(128);

      return keyGenerator.generateKey();

    }
    catch (Exception exception)
    {
      System.out.println("OOOps "+exception.getMessage());
      return null;
    }

  }
  public final static String secretKeyToString()
  {
    var secretKey = generateSecretKey();

    assert secretKey != null;

    return Base64.getEncoder().encodeToString(secretKey.getEncoded());

  }
}
