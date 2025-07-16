package net.trackme.commons.acl;

import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;

import javax.sql.DataSource;

public class PostgresJdbcMutableAclService extends JdbcMutableAclService {
    public PostgresJdbcMutableAclService(DataSource dataSource,
                                         LookupStrategy lookupStrategy,
                                         AclCache aclCache,
                                         String classIdentityQuery,
                                         String sidIdentityQuery) {
        super(dataSource, lookupStrategy, aclCache);
        super.setClassIdentityQuery(classIdentityQuery);
        super.setSidIdentityQuery(sidIdentityQuery);
        super.setAclClassIdSupported(true);
    }
}
