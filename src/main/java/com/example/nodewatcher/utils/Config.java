package com.example.nodewatcher.utils;

import java.util.regex.Pattern;

public class Config
{

  public final static int HTTP_PORT = 4500;
  public final static int DATA_SAVER_INSTANCES = 5;
  public final static int DB_POOL_SIZE = 5;

  public static boolean validIp(String ip)
  {
    var regex = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$";

    var pattern = Pattern.compile(regex);

    var matcher = pattern.matcher(ip);

    return matcher.matches();

  }
}

