package net.trackme.backend.services.acl;

import net.trackme.backend.configuration.AclAppProperties;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;

import javax.sql.DataSource;

public class PostgresJdbcMutableAclService extends JdbcMutableAclService {
  public PostgresJdbcMutableAclService(DataSource dataSource, LookupStrategy lookupStrategy, AclCache aclCache,
                                       AclAppProperties properties) {
    super(dataSource, lookupStrategy, aclCache);
    super.setClassIdentityQuery(properties.getClassIdentityQuery());
    super.setSidIdentityQuery(properties.getSidIdentityQuery());
    super.setAclClassIdSupported(true);
  }
}
