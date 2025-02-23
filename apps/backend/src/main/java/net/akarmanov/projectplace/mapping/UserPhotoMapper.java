package net.akarmanov.projectplace.mapping;

import net.akarmanov.projectplace.domain.UserPhoto;
import net.akarmanov.projectplace.services.user.UserPhotoDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring")
public interface UserPhotoMapper {

  UserPhotoDto toModel(UserPhoto userPhoto);

  @Mapping(target = "photo", ignore = true)
  @Mapping(target = "user",
           ignore = true)
  UserPhoto toEntity(UserPhotoDto userPhotoDto);
}
