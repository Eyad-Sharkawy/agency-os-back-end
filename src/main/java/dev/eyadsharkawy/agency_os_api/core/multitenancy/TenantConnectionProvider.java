package dev.eyadsharkawy.agency_os_api.core.multitenancy;

import lombok.RequiredArgsConstructor;
import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class TenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private static final String DEFAULT_SCHEMA = "public";
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private final DataSource dataSource;

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        resetToDefaultSchema(connection);
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = getAnyConnection();
        try {
            setSchema(connection, tenantIdentifier);
        } catch (SQLException e) {
            connection.close();
            throw new HibernateException("Could not switch to schema [" + tenantIdentifier + "]", e);
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) {
        try (connection) {
            resetToDefaultSchema(connection);
        } catch (SQLException e) {
            throw new HibernateException("Could not reset schema to default after tenant [" + tenantIdentifier + "]", e);
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
        String schema = validate(tenantIdentifier);
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET SCHEMA '" + schema + "'");
        }
    }

    private void resetToDefaultSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET SCHEMA '" + DEFAULT_SCHEMA + "'");
        }
    }

    private String validate(String tenantIdentifier) {
        if (tenantIdentifier == null || !SAFE_IDENTIFIER.matcher(tenantIdentifier).matches()) {
            throw new IllegalArgumentException("Invalid tenant identifier: " + tenantIdentifier);
        }
        return tenantIdentifier;
    }
}
