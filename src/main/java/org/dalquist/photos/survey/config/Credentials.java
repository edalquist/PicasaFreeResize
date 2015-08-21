package org.dalquist.photos.survey.config;

import org.dalquist.photos.survey.model.JsonObject;


public class Credentials extends JsonObject {
  private static final long serialVersionUID = 1L;

  private String email;
  private String password;

  public String getEmail() {
    return email;
  }
  public void setEmail(String email) {
    this.email = email;
  }
  public String getPassword() {
    return password;
  }
  public void setPassword(String password) {
    this.password = password;
  }
}