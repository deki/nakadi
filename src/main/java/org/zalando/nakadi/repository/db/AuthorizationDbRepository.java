package org.zalando.nakadi.repository.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.zalando.nakadi.annotations.DB;
import org.zalando.nakadi.domain.Permission;
import org.zalando.nakadi.exceptions.runtime.RepositoryProblemException;
import org.zalando.nakadi.plugin.api.authz.AuthorizationService;

import java.util.List;
import java.util.UUID;

@DB
@Repository
public class AuthorizationDbRepository extends AbstractDbRepository {

    @Autowired
    public AuthorizationDbRepository(final JdbcTemplate jdbcTemplate, final ObjectMapper jsonMapper) {
        super(jdbcTemplate, jsonMapper);
    }

    public List<Permission> listAdmins() throws RepositoryProblemException {
        final List<Permission> admins;
        try {
            admins = jdbcTemplate.query("SELECT * FROM zn_data.authorization WHERE az_resource='nakadi'",
                    permissionRowMapper);
        } catch (final DataAccessException e) {
            throw new RepositoryProblemException("Errorr occurred when fetching admininstrators", e);
        }

        return admins;
    }

    public void deletePermission(final Permission permission) {
        try {
            jdbcTemplate.update("DELETE FROM zn_data.authorization " +
                            "WHERE az_resource=? AND az_operation=? AND az_data_type=? AND az_value=?",
                    permission.getResource(), permission.getOperation().toString(),
                    permission.getAuthorizationAttribute().getDataType(),
                    permission.getAuthorizationAttribute().getValue());
        } catch (final DataAccessException e) {
            throw new RepositoryProblemException("Error occurred when deleting permission", e);
        }
    }

    public void createPermission(final Permission permission) {
        try {
            jdbcTemplate.update("INSERT INTO zn_data.authorization VALUES (?, ?, ?::az_operation, ?, ?)",
                    UUID.randomUUID(), permission.getResource(), permission.getOperation().toString(),
                    permission.getAuthorizationAttribute().getDataType(),
                    permission.getAuthorizationAttribute().getValue());
        } catch (final DataAccessException e) {
            throw new RepositoryProblemException("Error occurred when creating permission", e);
        }
    }

    static Permission buildPermission(final String resource, final String operation, final String dataType,
                                      final String value) {
        return new Permission(resource, AuthorizationService.Operation.valueOf(operation.toUpperCase()),
                dataType, value);
    }

    private final RowMapper<Permission> permissionRowMapper = (rs, rowNum)
            -> buildPermission(rs.getString("az_resource"), rs.getString("az_operation"),
            rs.getString("az_data_type"), rs.getString("az_value"));

}
