package bibernate.session;


/**
 * This is a main Bibernate API.
 */
public interface Session {
    <T> void persist(T entity);

    <T> T find(Class<T> entityType, Object id);

    <T> void remove(T person);

    void flush();

    void close();
}
