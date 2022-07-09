package bibernate.session;

import bibernate.util.EntityUtil;

public record EntityKey<T>(Class<T> entityType, Object id) {
    public static <T> EntityKey<?> of(Class<T> entityType, Object id) {
        return new EntityKey<>(entityType, id);
    }

    public static <T> EntityKey<T> valueOf(T entity) {
        var id = EntityUtil.getId(entity);
        var entityType = entity.getClass();
        return new EntityKey(entityType, id);
    }
}
