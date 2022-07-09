package bibernate.session;


/**
 * This is a main Bibernate API.
 */
public interface Session {

    <T> T find(Class<T> entityType, Object id);
}
