package bibernate.session.impl;

import bibernate.session.Session;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.sql.DataSource;

@RequiredArgsConstructor
public class SessionImpl implements Session {
    private JdbcEntityDao entityDao;

    public SessionImpl(DataSource dataSource) {
        entityDao = new JdbcEntityDao(dataSource);
    }

    @Override
    @SneakyThrows
    public <T> T find(Class<T> entityType, Object id) {
        return entityDao.findById(entityType, id);
    }

    @Override
    public void close() {
        entityDao.close();
    }

}
