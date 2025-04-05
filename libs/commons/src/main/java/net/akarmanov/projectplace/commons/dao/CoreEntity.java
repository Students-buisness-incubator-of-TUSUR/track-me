package net.akarmanov.projectplace.commons.dao;

import java.io.Serializable;

public interface CoreEntity<ID extends Serializable> extends Serializable {
  ID getId();

  void setId(ID id);
}
