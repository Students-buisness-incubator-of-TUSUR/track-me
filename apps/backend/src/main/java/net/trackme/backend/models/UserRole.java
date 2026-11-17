package net.trackme.backend.models;

public enum UserRole {
  OVERLORD,
  ADMIN,
  SUPER_ADMIN,
  TRACKER;

  @Override
  public String toString() {
    return name().toUpperCase();
  }

}
