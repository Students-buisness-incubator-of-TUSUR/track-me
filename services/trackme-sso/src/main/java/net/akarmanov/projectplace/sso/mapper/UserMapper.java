package net.akarmanov.projectplace.sso.mapper;

import net.akarmanov.projectplace.sso.dao.entity.RoleEntity;
import net.akarmanov.projectplace.sso.dao.entity.UserEntity;
import net.akarmanov.projectplace.sso.dto.UserDto;
import net.akarmanov.projectplace.sso.dto.UserUpdateDto;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {
  @Named("roleToCodeList")
  static List<String> roleToCodeList(Set<RoleEntity> roles) {
    return roles == null ? List.of() : roles.stream()
        .map(UserMapper::roleToCode)
        .toList();
  }

  @Named("roleToCode")
  static String roleToCode(RoleEntity role) {
    return role.getCode();
  }

  @Mapping(target = "enabled", source = "active")
  @Mapping(target = "roles", source = "roles", qualifiedByName = "roleToCodeList")
  UserDto userEntityToUserDto(UserEntity userEntity);

  @Mapping(target = "version", ignore = true)
  @Mapping(target = "username", ignore = true)
  @Mapping(target = "roles", ignore = true)
  @Mapping(target = "passwordHash", ignore = true)
  @Mapping(target = "lastUpdatedBy", ignore = true)
  @Mapping(target = "lastUpdateDate", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "creationDate", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "active", ignore = true)
  @Mapping(target = "accountNonLocked", ignore = true)
  void updateUserEntityFromUserDto(UserUpdateDto userDto, @MappingTarget UserEntity userEntity);
}