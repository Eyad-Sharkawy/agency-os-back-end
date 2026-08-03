package dev.eyadsharkawy.agency_os_api.core.multitenancy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private static final String DEFAULT_SCHEMA = "public";
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private final DataSource dataSource;

    @Override
    public Connection getAnyConnection() throws SQLException {
        log.debug("Fetching raw connection from datasource");
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        log.debug("Releasing raw connection");
        resetToDefaultSchema(connection);
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        log.debug("Acquiring connection for tenant: [{}]", tenantIdentifier);

        Connection connection = getAnyConnection();
        try {
            setSchema(connection, tenantIdentifier);
        } catch (SQLException e) {
            log.error("Failed to switch connection to schema for tenant: [{}]", tenantIdentifier, e);
            connection.close();
            throw new HibernateException("Could not switch to schema [" + tenantIdentifier + "]", e);
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) {
        log.debug("Releasing connection for tenant: [{}]", tenantIdentifier);
        try {
            resetToDefaultSchema(connection);
        } catch (SQLException e) {
            log.error("Failed to reset schema to default after tenant: [{}]", tenantIdentifier, e);
            throw new HibernateException("Could not reset schema to default after tenant [" + tenantIdentifier + "]", e);
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                log.warn("Failed to gracefully close connection for tenant: [{}]", tenantIdentifier, e);
            }
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(@NonNull Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(@NonNull Class<T> unwrapType) {
        return null;
    }

    private void setSchema(Connection connection, String tenantIdentifier) throws SQLException {
        executeSchemaChange(connection, validate(tenantIdentifier));
    }

    private void resetToDefaultSchema(Connection connection) throws SQLException {
        executeSchemaChange(connection, DEFAULT_SCHEMA);
    }

    private String validate(String tenantIdentifier) {
        if (tenantIdentifier == null || !SAFE_IDENTIFIER.matcher(tenantIdentifier).matches()) {
            log.warn("Security violation or malformed tenant identifier detected: [{}]", tenantIdentifier);
            throw new IllegalArgumentException("Invalid tenant identifier: " + tenantIdentifier);
        }
        return tenantIdentifier;
    }

    private void executeSchemaChange(Connection connection, String schema) throws SQLException {
        log.trace("Executing SQL: SET SCHEMA '{}'", schema);
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET SCHEMA '" + schema + "'");
        }
    }
}
