package com.example.nodewatcher.models;

import io.vertx.core.json.JsonObject;

public record Credential(String name,String username,String password,String timestamp,int protocol)
{
  public static Credential fromJson(JsonObject jsonObject)
  {
    var name = jsonObject.getString("name");

    var username = jsonObject.getString("username");

    var password = jsonObject.getString("password");

    var timestamp = jsonObject.getString("timestamp");

    var protocol = jsonObject.getInteger("protocol");

    protocol=1;//SSH for now

    //protocol 1 = SSH
    //protocol 2 = Windows
    //protocol 3 = Cloud

    return new Credential(name,username,password,timestamp,protocol);

  }
}
