package bibernate.session.impl;

import bibernate.session.EntityKey;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static bibernate.util.EntityUtil.*;

/**
 * Reflection based JDCB client for entities.
 **/
@RequiredArgsConstructor
public class JdbcEntityDao {
    private final String SELECT_FROM_TABLE_BY_COLUMN = "select * from %s where %s = ?;";
    private final DataSource dataSource;
    private Map<EntityKey<?>, Object> entityCache = new HashMap<>();
    private boolean open = true;

    @SneakyThrows
    public <T> T findById(Class<T> entityType, Object id) {
        veritySessionIsOpen();
        var cachedEntity = entityCache.get(EntityKey.of(entityType, id));
        if (cachedEntity != null) {
            return entityType.cast(cachedEntity);
        }
        var idField = getIdField(entityType);
        return findOneBy(entityType, idField, id);
    }

    @SneakyThrows
    public <T> List<T> findAllBy(Class<T> entityType, Field field, Object columnValue) {
        veritySessionIsOpen();
        var list = new ArrayList<T>();
        try (var connection = dataSource.getConnection()) {
            var tableName = resolveTableName(entityType);
            var columnName = resolveColumnName(field);
            var selectSql = String.format(SELECT_FROM_TABLE_BY_COLUMN, tableName, columnName);
            try (var selectStatement = connection.prepareStatement(selectSql)) {
                selectStatement.setObject(1, columnValue);
                System.out.println("SQL: " + selectStatement);
                var resultSet = selectStatement.executeQuery();
                while (resultSet.next()) {
                    var entity = createEntityFromResultSet(entityType, resultSet);
                    list.add(entity);
                }
            }
        }
        return list;
    }

    @SneakyThrows
    public <T> T findOneBy(Class<T> entityType, Field field, Object columnValue) {
        veritySessionIsOpen();
        var result = findAllBy(entityType, field, columnValue);
        if (result.size() != 1) {
            throw new IllegalStateException("The result must contain exactly one row");
        }
        return result.get(0);
    }

    @SneakyThrows
    private <T> T createEntityFromResultSet(Class<T> entityType, ResultSet resultSet) {
        var constructor = entityType.getConstructor();
        var entity = constructor.newInstance();
        for (var field : entityType.getDeclaredFields()) {
            var columnName = resolveColumnName(field);
            var columnValue = resultSet.getObject(columnName);
            field.setAccessible(true);
            field.set(entity, columnValue);
        }

        return cache(entity);
    }

    private <T> T cache(T entity) {
        var entityKey = EntityKey.valueOf(entity);
        var cachedEntity = entityCache.get(entityKey);
        if (cachedEntity != null) {
            return (T) cachedEntity;
        } else {
            entityCache.put(entityKey, entity);
            return entity;
        }
    }

    public void veritySessionIsOpen() {
        if (!isOpen()) {
            throw new RuntimeException("Session has been already closed.");
        }
    }
    
    public boolean isOpen() {
        return open;
    }

    public void close() {
        this.entityCache.clear();
        this.open = false;
    }
}
