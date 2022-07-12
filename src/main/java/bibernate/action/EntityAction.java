package bibernate.action;

/**
 * Represents a write SQL operation. Implementation of this interface are used to perform insert, update and remove
 * operations asynchronously.
 */
public interface EntityAction {
    void execute();

    int priority();
}
