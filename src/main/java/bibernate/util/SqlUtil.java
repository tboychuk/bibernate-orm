package bibernate.util;

import java.util.Arrays;
import java.util.stream.Collectors;

import static bibernate.util.EntityUtil.getInsertableFields;
import static bibernate.util.EntityUtil.getUpdatableFields;

public class SqlUtil { 
    public static final String INSERT_INTO_TABLE_VALUES_TEMPLATE = "INSERT INTO %s(%s) VALUES(%s);";
    public static final String SELECT_FROM_TABLE_BY_COLUMN_QUERY_TEMPLATE = "SELECT * FROM %s WHERE %s = ?;";
    public static final String UPDATE_TABLE_SET_VALUES_BY_COLUMN_TEMPLATE = "UPDATE %s SET %s WHERE %s;";
    public static final String DELETE_FROM_TABLE_BY_COLUMN = "DELETE FROM %s WHERE %s = ?;";

    public static String commaSeparatedInsertableColumns(Class<?> entityType) {
        var insertableFields = getInsertableFields(entityType);
        return Arrays.stream(insertableFields)
                .map(EntityUtil::resolveColumnName)
                .collect(Collectors.joining(", "));
    }

    public static String commaSeparatedInsertableParams(Class<?> entityType) {
        var insertableFields = getInsertableFields(entityType);
        return Arrays.stream(insertableFields)
                .map(f -> "?")
                .collect(Collectors.joining(","));
    }

    public static String commaSeparatedUpdatableColumnSetters(Class<?> entityType) {
        var updatableFields = getUpdatableFields(entityType);
        return Arrays.stream(updatableFields)
                .map(EntityUtil::resolveColumnName)
                .map(column -> column + " = ?")
                .collect(Collectors.joining(", "));
    }

}
