package net.trackme.sso.dao.repository;

import net.trackme.sso.dao.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

  Optional<RoleEntity> findByCode(String code);
}
