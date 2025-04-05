package net.akarmanov.projectplace.commons.dao;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@MappedSuperclass
public abstract class VersionedEntity<ID extends Serializable> implements CoreEntity<ID> {
  @Version
  @Column(name = "object_version_number", nullable = false)
  private Long version;
}
