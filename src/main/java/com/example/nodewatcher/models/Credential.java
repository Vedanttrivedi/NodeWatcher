package com.example.nodewatcher.models;

import io.vertx.core.json.JsonObject;

public record Credential(String name,String username,String password,String timestamp,int protocol)
{

}
