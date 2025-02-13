package net.akarmanov.projectplace.rest.api.dto;

public enum UserRoleDto {
  ADMIN,
  TRACKER,
  SUPER_ADMIN;

  @Override
  public String toString() {
    return name().toUpperCase();
  }
}
