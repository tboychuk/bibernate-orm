package bibernate.util;

import bibernate.annotation.Column;
import bibernate.annotation.Id;
import bibernate.annotation.Table;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

public class EntityUtil {

    public static String resolveColumnName(Field field) {
        return Optional.ofNullable(field.getAnnotation(Column.class))
                .map(Column::value)
                .orElse(field.getName());
    }

    public static <T> String resolveTableName(Class<T> entityType) {
        return Optional.ofNullable(entityType.getAnnotation(Table.class))
                .map(Table::value)
                .orElse(entityType.getSimpleName());
    }

    public static <T> Field getIdField(Class<T> entityType) {
        return Arrays.stream(entityType.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Id.class))
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
}
