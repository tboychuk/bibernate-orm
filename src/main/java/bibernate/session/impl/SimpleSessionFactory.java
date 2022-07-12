package bibernate.session.impl;

import bibernate.session.Session;
import bibernate.session.SessionFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import javax.sql.DataSource;

@Log4j2
@RequiredArgsConstructor
public class SimpleSessionFactory implements SessionFactory {
    private final DataSource dataSource;

    @Override
    public Session openSession() {
        log.info("Opening new StatefulSession");
        return new StatefulSession(dataSource);
    }
}
