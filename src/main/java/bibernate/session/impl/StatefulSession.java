package bibernate.session.impl;

import bibernate.action.EntityAction;
import bibernate.action.EntityDeleteAction;
import bibernate.action.EntityInsertAction;
import bibernate.action.EntityUpdateAction;
import bibernate.session.Session;
import bibernate.util.EntityKey;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import javax.sql.DataSource;
import java.util.PriorityQueue;
import java.util.Queue;

import static java.util.Comparator.comparing;

/**
 * A stateful implementation of a {@link Session} interface. It {@link PersistenceContext} as a 1st level cache of
 * entities. All read operations are synchronous. All write operations are performed asynchronously using a queue of
 * {@link EntityAction} objects.
 * <p>
 * All the changes are made in the persistence context, and stored to the queue. Actions have the following priority:
 * 1. INSERT
 * 2. UPDATE
 * 3. DELETE
 * Actions in the queue are executed on flush.
 * <p>
 * All entity updates (entity state changes) are tracked via Dirty Checking mechanism. It means that initial entity
 * snapshots are stored in the {@link PersistenceContext} and then are compared with entity state to detect changes.
 */
@Log4j2
@RequiredArgsConstructor
public class StatefulSession implements Session {
    private EntityPersister persister;
    private PersistenceContext persistenceContext = new PersistenceContext();

    private Queue<EntityAction> actionQueue = new PriorityQueue<>(comparing(EntityAction::priority));
    private boolean closed;

    public StatefulSession(DataSource dataSource) {
        persister = new EntityPersister(dataSource, this, persistenceContext);
    }

    @Override
    public <T> void persist(T entity) {
        verifyIsOpen();
        log.info("Persisting entity {}", entity);
        if (persistenceContext.contains(entity)) {
            throw new RuntimeException("Entity already exists");
        }
        persistenceContext.addEntity(entity);
        log.trace("Adding EntityInsertAction for entity {} to the ActionQueue", entity);
        actionQueue.add(new EntityInsertAction(entity, persister));
    }

    @Override
    @SneakyThrows
    public <T> T find(Class<T> entityType, Object id) {
        verifyIsOpen();
        log.info("Finding entity {} by id = {}", entityType.getSimpleName(), id);
        return persister.findById(entityType, id);
    }

    @Override
    public <T> void remove(T entity) {
        verifyIsOpen();
        log.info("Removing entity {}", entity);
        var managedEntity = persistenceContext.getEntity(EntityKey.valueOf(entity));
        if (managedEntity == null) {
            throw new RuntimeException("Cannot remove an entity that are not in the current session");
        }
        actionQueue.add(new EntityDeleteAction(entity, persister));
    }

    @Override
    public void flush() {
        verifyIsOpen();
        log.trace("Session flush");
        dirtyChecking();
        processActionQueue();
    }

    private void dirtyChecking() {
        log.trace("Checking dirty entities");
        var entities = persistenceContext.getDirtyEntities();
        var updateActions = entities.stream()
                .peek(e -> log.trace("Creating EntityUpdateAction for entity {}", e))
                .map(e -> new EntityUpdateAction(e, persister))
                .toList();
        actionQueue.addAll(updateActions);
    }

    private void processActionQueue() {
        log.trace("Flushing ActionQueue");
        while (!actionQueue.isEmpty()) {
            var entityAction = actionQueue.poll();
            entityAction.execute();
        }
    }

    @Override
    public void close() {
        verifyIsOpen();
        log.info("Closing session");
        flush();
        persistenceContext.clear();
        closed = true;
    }

    void verifyIsOpen() {
        if (isClosed()) {
            throw new IllegalStateException("Session is closed");
        }
    }

    public boolean isClosed() {
        return closed;
    }
}
