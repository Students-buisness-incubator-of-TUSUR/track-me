package net.akarmanov.projectplace.mapping;

import net.akarmanov.projectplace.domain.User;
import net.akarmanov.projectplace.rest.api.dto.UserDTO;
import net.akarmanov.projectplace.rest.api.dto.UserUpdateDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {UserPhotoMapper.class})
public interface UserMapper {


  UserDTO mapUserToDto(User user);

  @Mapping(target = "streams", ignore = true)
  @Mapping(target = "middleName", ignore = true)
  @Mapping(target = "lastName", ignore = true)
  @Mapping(target = "firstName", ignore = true)
  @Mapping(target = "userTeamCards", ignore = true)
  @Mapping(target = "role", ignore = true)
  @Mapping(target = "photo", ignore = true)
  @Mapping(target = "password", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "enabled", ignore = true)
  void updateFromDto(UserUpdateDTO userDTO, @MappingTarget User user);
}
