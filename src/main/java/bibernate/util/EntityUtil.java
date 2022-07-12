package bibernate.util;

import bibernate.annotation.*;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

@Log4j2
public final class EntityUtil {
    private EntityUtil() {
    }

    public static String resolveColumnName(Field field) {
        return Optional.ofNullable(field.getAnnotation(Column.class))
                .map(Column::value)
                .orElse(field.getName());
    }

    public static String resolveIdColumnName(Class<?> entityType) {
        var field = getIdField(entityType);
        return resolveColumnName(field);
    }

    public static <T> String resolveTableName(Class<T> entityType) {
        return Optional.ofNullable(entityType.getAnnotation(Table.class))
                .map(Table::value)
                .orElse(entityType.getSimpleName());
    }

    public static <T> Field getIdField(Class<T> entityType) {
        return Arrays.stream(entityType.getDeclaredFields())
                .filter(EntityUtil::isIdField)
                .findAny()
                .orElseThrow(() -> new RuntimeException("Cannot find a field marked with @Id in class " + entityType.getSimpleName()));
    }

    @SneakyThrows
    public static Object getId(Object entity) {
        var entityType = entity.getClass();
        var idField = getIdField(entityType);
        idField.setAccessible(true);
        return idField.get(entity);
    }

    public static boolean isIdField(Field field) {
        return field.isAnnotationPresent(Id.class);
    }

    public static boolean isColumnField(Field field) {
        return !isEntityCollectionField(field);
    }

    public static boolean isSimpleColumnField(Field field) {
        return !isEntityField(field) && !isEntityCollectionField(field);
    }

    public static boolean isEntityField(Field field) {
        return field.isAnnotationPresent(ManyToOne.class);
    }

    public static boolean isEntityCollectionField(Field field) {
        return field.isAnnotationPresent(OneToMany.class);
    }

    public static <T> Field getRelatedEntityField(Class<T> fromEntity, Class<?> toEntity) {
        return Arrays.stream(toEntity.getDeclaredFields())
                .filter(f -> f.getType().equals(fromEntity))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Cannon find related field between in " + toEntity + " for " + fromEntity));
    }

    public static Field[] getFields(Class<?> entityType, Predicate<Field> fieldPredicate) {
        return Arrays.stream(entityType.getDeclaredFields())
                .filter(fieldPredicate)
                .toArray(Field[]::new);
    }

    public static Field[] getColumnFields(Class<?> entityType) {
        return getFields(entityType, EntityUtil::isColumnField);
    }

    public static Field[] getUpdatableFields(Class<?> entityType) {
        Predicate<Field> updatableFieldPredicate = f -> isColumnField(f) && !isIdField(f);
        return getFields(entityType, updatableFieldPredicate);
    }

    public static Field[] getInsertableFields(Class<?> entityType) {
        return getFields(entityType, EntityUtil::isColumnField);
    }

    public static Class<?> getEntityCollectionElementType(Field field) {
        var parameterizedType = (ParameterizedType) field.getGenericType();
        var typeArguments = parameterizedType.getActualTypeArguments();
        var actualTypeArgument = typeArguments[0];
        var relatedEntityType = (Class<?>) actualTypeArgument;
        return relatedEntityType;
    }


    @SneakyThrows
    public static Object[] entityToSnapshot(Object entity) {
        log.trace("Creating a snapshot for entity {}", entity);
        var columnValues = new ArrayList<>();
        for (var f : entity.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            if (isSimpleColumnField(f)) {
                var columnValue = f.get(entity);
                columnValues.add(columnValue);
            } else if (isEntityField(f)) {
                var relatedEntity = f.get(entity);
                var relatedEntityId = getId(relatedEntity);
                columnValues.add(relatedEntityId);
            }
        }
        var snapshot = columnValues.toArray();
        log.trace("Created a snapshot {}", Arrays.toString(snapshot));
        return snapshot;
    }
}
