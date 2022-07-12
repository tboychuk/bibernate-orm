package bibernate.session.impl;

import bibernate.collection.LazyList;
import bibernate.util.EntityKey;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static bibernate.util.EntityUtil.*;
import static bibernate.util.SqlUtil.*;

/**
 * {@link EntityPersister} is a reflection-based CRUD API for entities that is implemented using JDBC API.
 **/
@Log4j2
@RequiredArgsConstructor
public class EntityPersister {
    private final DataSource dataSource;
    private final StatefulSession session;
    private final PersistenceContext persistenceContext;

    @SneakyThrows
    public <T> T insert(T entity) {
        log.trace("Inserting entity {}", entity);
        var entityType = entity.getClass();
        try (var connection = dataSource.getConnection()) {
            var tableName = resolveTableName(entityType);
            log.trace("Resolved table name -> '{}'", tableName);
            var columns = commaSeparatedInsertableColumns(entityType);
            var params = commaSeparatedInsertableParams(entityType);
            var insertQuery = String.format(INSERT_INTO_TABLE_VALUES_TEMPLATE, tableName, columns, params);
            log.trace("Preparing insert statement: {}", insertQuery);
            try (var insertStatement = connection.prepareStatement(insertQuery)) {
                fillInsertStatementParams(insertStatement, entity);
                log.debug("SQL: {}", insertStatement);
                insertStatement.executeUpdate();
            }
        }
        return entity;
    }

    @SneakyThrows
    public <T> T findById(Class<T> entityType, Object id) {
        log.trace("Selecting entity {} by id = {}", entityType.getSimpleName(), id);
        var key = EntityKey.of(entityType, id);
        var cachedEntity = persistenceContext.getEntity(key);
        if (cachedEntity != null) {
            log.trace("Returning cached entity from the context {}", cachedEntity);
            return entityType.cast(cachedEntity);
        }
        log.trace("No cached entity found... Loading entity from the DB");
        var idField = getIdField(entityType);
        return findOneBy(entityType, idField, id);
    }

    @SneakyThrows
    public <T> List<T> findAllBy(Class<T> entityType, Field field, Object columnValue) {
        log.trace("Selecting from table by column value");
        var list = new ArrayList<T>();
        try (var connection = dataSource.getConnection()) {
            var tableName = resolveTableName(entityType);
            log.trace("Resolved table name -> {}", tableName);
            var columnName = resolveColumnName(field);
            log.trace("Resolved column name -> {}", columnName);
            var selectSql = String.format(SELECT_FROM_TABLE_BY_COLUMN_QUERY_TEMPLATE, tableName, columnName);
            log.trace("Preparing select statement: {}", selectSql);
            try (var selectStatement = connection.prepareStatement(selectSql)) {
                selectStatement.setObject(1, columnValue);
                log.debug("SQL: {}", selectStatement);
                var resultSet = selectStatement.executeQuery();
                while (resultSet.next()) {
                    var entity = createEntityFrom(entityType, resultSet);
                    list.add(entity);
                }
            }
        }
        return list;
    }

    @SneakyThrows
    public <T> T findOneBy(Class<T> entityType, Field field, Object columnValue) {
        var result = findAllBy(entityType, field, columnValue);
        if (result.size() != 1) {
            throw new IllegalStateException("The result must contain exactly one row");
        }
        return result.get(0);
    }

    @SneakyThrows
    private <T> T createEntityFrom(Class<T> entityType, ResultSet resultSet) {
        log.trace("Creating entity {} from the result set", entityType.getSimpleName());
        var constructor = entityType.getConstructor();
        var entity = constructor.newInstance();
        log.trace("Processing entity fields");
        for (var field : entityType.getDeclaredFields()) {
            field.setAccessible(true);
            if (isSimpleColumnField(field)) {
                log.trace("Processing simple field {}", field.getName());
                var columnName = resolveColumnName(field);
                log.trace("Resolved column name '{}'", columnName);
                var columnValue = resultSet.getObject(columnName);
                log.trace("Fetched column value '{}' from the result set", columnValue);
                log.trace("Setting value '{}' to the entity field {}", columnValue, field.getName());
                field.set(entity, columnValue);
            } else if (isEntityField(field)) {
                log.trace("Processing entity field {}", field.getName());
                var relatedEntityType = field.getType();
                var joinColumnName = resolveColumnName(field);
                log.trace("Resolved joining column name '{}'", joinColumnName);
                var joinColumnValue = resultSet.getObject(joinColumnName);
                log.trace("Fetched joining column value '{}' from the result set", joinColumnValue);
                var relatedEntity = findById(relatedEntityType, joinColumnValue);
                log.trace("Setting related entity {} to the field {}", relatedEntity, field.getName());
                field.set(entity, relatedEntity);
            } else if (isEntityCollectionField(field)) {// setting lazy list for toMany relation
                log.trace("Processing entity collection field {}", field.getName());
                var relatedEntityType = getEntityCollectionElementType(field);
                log.trace("Resolved related entity collection element type -> {}", relatedEntityType.getSimpleName());
                var entityFieldInRelatedEntity = getRelatedEntityField(entityType, relatedEntityType);
                log.trace("Resolved entity field on the opposite side -> {}", entityFieldInRelatedEntity.getName());
                var entityId = getId(entity);
                log.trace("Creating a list that will lazily fetch {} elements for {} with id = {}",
                        relatedEntityType.getSimpleName(), entityType.getSimpleName(), entityId);
                var list = createLazyList(relatedEntityType, entityFieldInRelatedEntity, entityId);
                log.trace("Setting lazy list of {} to the entity field {}", relatedEntityType.getSimpleName(), field.getName());
                field.set(entity, list);
            }
        }
        return persistenceContext.manageEntity(entity);
    }

    private <T> LazyList<T> createLazyList(Class<T> relatedEntityType, Field entityFieldInRelatedEntity, Object entityId) {
        Supplier<List<T>> listSupplier = () -> {
            session.verifyIsOpen();
            return findAllBy(relatedEntityType, entityFieldInRelatedEntity, entityId);
        };
        return new LazyList<>(listSupplier);
    }

    @SneakyThrows
    public <T> T update(T entity) {
        log.trace("Updating entity {}", entity);
        var entityType = entity.getClass();
        try (var connection = dataSource.getConnection()) {
            var tableName = resolveTableName(entityType);
            log.trace("Resolved table name -> {}", tableName);
            var updatableColumns = commaSeparatedUpdatableColumnSetters(entityType);
            var idColumn = resolveIdColumnName(entityType) + " = ?";
            var updateQuery = String.format(UPDATE_TABLE_SET_VALUES_BY_COLUMN_TEMPLATE, tableName, updatableColumns, idColumn);
            log.trace("Preparing update statement: {}", updateQuery);
            try (var updateStatement = connection.prepareStatement(updateQuery)) {
                fillUpdateStatementParams(updateStatement, entity);
                var idParamIndex = getUpdatableFields(entityType).length + 1;
                updateStatement.setObject(idParamIndex, getId(entity));
                log.debug("SQL: " + updateStatement);
                updateStatement.executeUpdate();
            }
        }
        return entity;
    }

    @SneakyThrows
    public <T> T delete(T entity) {
        log.trace("Deleting entity {}", entity);
        var entityType = entity.getClass();
        try (var connection = dataSource.getConnection()) {
            var tableName = resolveTableName(entityType);
            log.trace("Resolved table name -> {}", tableName);
            var idColumnName = resolveIdColumnName(entityType);
            var deleteQuery = String.format(DELETE_FROM_TABLE_BY_COLUMN, tableName, idColumnName);
            log.trace("Preparing delete statement: {}", deleteQuery);
            try (var deleteStatement = connection.prepareStatement(deleteQuery)) {
                Object id = getId(entity);
                deleteStatement.setObject(1, id);
                log.debug("SQL: " + deleteStatement);
                deleteStatement.executeUpdate();
            }
        }
        return entity;
    }

    @SneakyThrows
    private <T> void fillInsertStatementParams(PreparedStatement insertStatement, T entity) {
        var insertableFields = getInsertableFields(entity.getClass());
        setParamsFromFields(insertStatement, entity, insertableFields);
    }

    @SneakyThrows
    private <T> void fillUpdateStatementParams(PreparedStatement updateStatement, T entity) {
        var updatableFields = getUpdatableFields(entity.getClass());
        setParamsFromFields(updateStatement, entity, updatableFields);
    }

    @SneakyThrows
    private void setParamsFromFields(PreparedStatement statement, Object entity, Field[] fields) {
        for (int i = 0; i < fields.length; i++) {
            var field = fields[i];
            field.setAccessible(true);
            var columnValue = field.get(entity);
            statement.setObject(i + 1, columnValue);
        }
    }
}
