package com.example.nodewatcher.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class Config {

  public final static TimeUnit TIME_UNIT = TimeUnit.SECONDS;
  public final static int HTTP_PORT = 4500;


}

