package bibernate.session.impl;

import bibernate.util.EntityKey;
import lombok.extern.log4j.Log4j2;

import java.util.*;

import static bibernate.util.EntityUtil.entityToSnapshot;

/**
 * A context of entities that acts as a 1st level cache. It stores entity objects as well, and their initial snapshots.
 */
@Log4j2
public class PersistenceContext {
    private final Map<EntityKey<?>, Object> entitiesByKey = new HashMap<>();
    private final Map<EntityKey<?>, Object[]> entitiesSnapshotByKey = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T manageEntity(T entity) {
        log.trace("Checking entity {}", entity);
        var key = EntityKey.valueOf(entity);
        var cachedEntity = entitiesByKey.get(key);
        if (cachedEntity != null) {
            log.trace("Entity is already in the context. Returning cached object {}", cachedEntity);
            return (T) cachedEntity;
        } else {
            log.trace("Adding new entity {} to the context", entity);
            return addEntity(entity);
        }
    }

    public <T> T getEntity(EntityKey<T> key) {
        log.trace("Getting entity from the context by key {}", key);
        Object entity = entitiesByKey.get(key);
        return key.entityType().cast(entity);
    }

    public <T> T addEntity(T entity) {
        log.trace("Adding entity {} to the PersistenceContext", entity);
        var key = EntityKey.valueOf(entity);
        entitiesByKey.put(key, entity);
        entitiesSnapshotByKey.put(key, entityToSnapshot(entity));
        return entity;
    }

    public <T> boolean contains(T entity) {
        log.trace("Checking if entity {} exists in the context", entity);
        var key = EntityKey.valueOf(entity);
        return entitiesByKey.containsKey(key);
    }

    public List<?> getDirtyEntities() {
        log.trace("Looking for dirty entities (the ones that have changed)");
        var list = new ArrayList<>();
        for (var entityEntry : entitiesByKey.entrySet()) {
            var currentEntity = entityEntry.getValue();
            var currentEntitySnapshot = entityToSnapshot(currentEntity);
            var initialSnapshot = entitiesSnapshotByKey.get(entityEntry.getKey());
            log.trace("Comparing snapshots: {} <=> {}", initialSnapshot, currentEntitySnapshot);
            if (!Arrays.equals(currentEntitySnapshot, initialSnapshot)) {
                log.trace("Found dirty entity {}", currentEntity);
                log.trace("Initial snapshot {}", initialSnapshot);
                list.add(currentEntity);
            }
        }
        return list;
    }

    public void clear() {
        log.trace("Clearing persistence context");
        entitiesByKey.clear();
        entitiesSnapshotByKey.clear();
    }
}
