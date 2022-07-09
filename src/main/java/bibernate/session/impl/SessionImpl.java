package bibernate.session.impl;

import bibernate.session.Session;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.ResultSet;

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

    private <T> T createEntityFromResultSet(Class<T> entityType, ResultSet resultSet) {
        // todo: 1. create entity instance
        // todo: 2. for each field -> find a corresponding column value in the result set
        // todo: 3. set field value
        // todo: 4. return entity
    }
}
