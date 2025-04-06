package net.akarmanov.projectplace.sso.dao.repository;

import net.akarmanov.projectplace.sso.dao.entity.SystemOauth2ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemOauth2ClientRepository extends JpaRepository<SystemOauth2ClientEntity, Long> {
  Optional<SystemOauth2ClientEntity> findByClientId(String clientId);
}
