package net.trackme.backend.commons.dao;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@MappedSuperclass
public abstract class VersionedBusinessEntity<ID extends Serializable> extends BusinessEntity<ID> {
  @Version
  @Column(name = "object_version_number", nullable = false)
  private Long version;
}
