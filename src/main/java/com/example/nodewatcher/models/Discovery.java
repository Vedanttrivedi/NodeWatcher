package com.example.nodewatcher.models;

import io.vertx.core.json.JsonObject;

public class Discovery
{
    private int id;

    private String name;

    private int credentialId;//references credentials(id)

  private String time;

  public Discovery(int id, String name, int credentialId, String ip, boolean isProvisioned, String created_at) {
    this.id = id;
    this.name = name;
    this.credentialId = credentialId;
    this.ip = ip;
    this.isProvisioned = isProvisioned;
    this.created_at = created_at;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  private  String ip;
  public JsonObject toJson(String status)
  {
    var jsonObject = new JsonObject();
    jsonObject.put("id",id);
    jsonObject.put("name",name);
    jsonObject.put("cid",credentialId);
    jsonObject.put("created_at",created_at);
    jsonObject.put("isProvisioned",isProvisioned);
    jsonObject.put("ip",ip);
    jsonObject.put("status",status);
    return jsonObject;
  }

  private boolean isProvisioned;


  public String getCreated_at()
  {
    return created_at;
  }

  public void setCreated_at(String created_at)
  {
    this.created_at = created_at;
  }

  private String created_at;


  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getCredentialId() {
    return credentialId;
  }

  public void setCredentialId(int credentialId) {
    this.credentialId = credentialId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "Discovery{" +
      "id=" + id +
      ", name='" + name + '\'' +
      ", credentialId=" + credentialId +
      ", time='" + time + '\'' +
      ", ip='" + ip + '\'' +
      ", isProvisioned=" + isProvisioned +
      ", created_at='" + created_at + '\'' +
      '}';
  }
}
