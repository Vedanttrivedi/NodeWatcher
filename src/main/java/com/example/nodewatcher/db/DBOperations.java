package com.example.nodewatcher.db;

import com.example.nodewatcher.models.Credential;

public interface DBOperations
{
  void save(Credential credential);

  void getCredential();

  void getCredential(int id);

  void deleteCredential(int id);

  void updateCredential(int id,Credential credential);

}
