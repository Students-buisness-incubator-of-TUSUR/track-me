package net.akarmanov.projectplace.sso.dao.repository;

import net.akarmanov.projectplace.sso.dao.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

  RoleEntity findByCode(String code);
}
