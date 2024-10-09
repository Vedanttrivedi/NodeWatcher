package com.example.nodewatcher.utils;

public class Address
{
  public final static String PINGCHECK = "com.example.nodewatcher.PingChecker";

  public final static String SSHCHECK = "com.example.nodewatcher.SSH";

  public final static String DUMPDB = "com.example.nodewatcher.dumpDB";

  public final static String UPDATE_DISCOVERY = "com.example.nodewatcher.updateDiscovery";

  public final static String PLUGIN_DATA_SENDER = "com.example.nodewatcher.pluginDataSender";

  public final static String UNREACHED_DISCOVERY = "com.example.nodewatcher.unreachedDiscovery";

  public final static String PUSH_SOCKET = "tcp://localhost:4555";

  public final static String PULL_SOCKET = "tcp://localhost:4556";

  public final static int MEMORY_INTERVAL=20000;

  public final static int CPU_INTERVAL=35000;

  public final static int UNREACHBILITY_TIMER=30000;//5 Minutes

}
