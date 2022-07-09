package bibernate.session.impl;

import bibernate.annotation.Column;
import bibernate.session.Session;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.Optional;

@RequiredArgsConstructor
public class SessionImpl implements Session {
    private final DataSource dataSource;
    
    @Override
    @SneakyThrows
    public <T> T find(Class<T> entityType, Object id) {
        try (var connection = dataSource.getConnection()) {
            try (var selectStatement = connection.createStatement()) {
                var resultSet = selectStatement.executeQuery("select * from persons where id = " + id);
                resultSet.next();
                // todo: create entity from the result set
                return createEntityFromResultSet(entityType, resultSet);
            }
        }
    }

    @SneakyThrows
    private <T> T createEntityFromResultSet(Class<T> entityType, ResultSet resultSet) {
        var constructor = entityType.getConstructor();
        var entity = constructor.newInstance();
        for (var field : entityType.getDeclaredFields()) {
            var columnName = Optional.ofNullable(field.getAnnotation(Column.class))
                    .map(Column::value)
                    .orElse(field.getName());
            var columnValue = resultSet.getObject(columnName);
            field.setAccessible(true);
            field.set(entity, columnValue);
        }
        return entity;
    }
}
