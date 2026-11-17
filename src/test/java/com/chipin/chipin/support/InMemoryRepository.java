package com.chipin.chipin.support;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Test helper: a stand-in for a Spring Data repository that keeps rows in a map
 * instead of PostgreSQL. It lets service tests run in milliseconds with no database.
 *
 * It understands save, saveAll, findById, findAll, delete, deleteAll, and query
 * methods named like findByGroupId, findByGroupIdAndUserId, findByItemIdIn and
 * findByAssetIdOrderByIdDesc, which covers everything the services call.
 */
public final class InMemoryRepository {

    private InMemoryRepository() {}

    @SuppressWarnings("unchecked")
    public static <R> R create(Class<R> repositoryType) {
        Map<Long, Object> rows = new LinkedHashMap<>();
        long[] nextId = {1};

        return (R) Proxy.newProxyInstance(repositoryType.getClassLoader(), new Class<?>[]{repositoryType},
                (proxy, method, args) -> {
                    String name = method.getName();
                    switch (name) {
                        case "save":
                            return store(rows, nextId, args[0]);
                        case "saveAll": {
                            List<Object> saved = new ArrayList<>();
                            for (Object entity : (Iterable<Object>) args[0]) {
                                saved.add(store(rows, nextId, entity));
                            }
                            return saved;
                        }
                        case "findById":
                            return Optional.ofNullable(rows.get((Long) args[0]));
                        case "findAll":
                            return new ArrayList<>(rows.values());
                        case "delete":
                            rows.values().remove(args[0]);
                            return null;
                        case "deleteAll":
                            if (args == null) {
                                rows.clear();
                            } else {
                                for (Object entity : (Iterable<Object>) args[0]) {
                                    rows.values().remove(entity);
                                }
                            }
                            return null;
                        case "count":
                            return (long) rows.size();
                        case "toString":
                            return "InMemoryRepository(" + repositoryType.getSimpleName() + ")";
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        default:
                            if (name.startsWith("findBy")) {
                                return query(rows, method, args);
                            }
                            throw new UnsupportedOperationException("InMemoryRepository does not support " + name);
                    }
                });
    }

    // Gives a new entity the next id, like the database would.
    private static Object store(Map<Long, Object> rows, long[] nextId, Object entity) throws Exception {
        Long id = (Long) entity.getClass().getMethod("getId").invoke(entity);
        if (id == null) {
            id = nextId[0]++;
            entity.getClass().getMethod("setId", Long.class).invoke(entity, id);
        }
        rows.put(id, entity);
        return entity;
    }

    // Handles names like findByAssetIdAndUserIdOrderByIdDesc and findByItemIdIn.
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object query(Map<Long, Object> rows, Method method, Object[] args) throws Exception {
        String rest = method.getName().substring("findBy".length());
        String orderBy = null;
        int orderAt = rest.indexOf("OrderBy");
        if (orderAt >= 0) {
            orderBy = rest.substring(orderAt + "OrderBy".length());
            rest = rest.substring(0, orderAt);
        }
        String[] conditions = rest.split("And");

        List<Object> matches = new ArrayList<>();
        for (Object row : rows.values()) {
            boolean ok = true;
            for (int i = 0; i < conditions.length && ok; i++) {
                String property = conditions[i];
                if (property.endsWith("In")) {
                    Object value = read(row, property.substring(0, property.length() - 2));
                    ok = ((Collection<?>) args[i]).contains(value);
                } else {
                    ok = Objects.equals(read(row, property), args[i]);
                }
            }
            if (ok) {
                matches.add(row);
            }
        }

        if (orderBy != null) {
            boolean descending = orderBy.endsWith("Desc");
            String property = orderBy.replaceAll("(Asc|Desc)$", "");
            matches.sort((a, b) -> {
                try {
                    int result = ((Comparable) read(a, property)).compareTo(read(b, property));
                    return descending ? -result : result;
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        }

        if (method.getReturnType() == Optional.class) {
            return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
        }
        return matches;
    }

    // Calls getXxx() or isXxx() for a property name such as "GroupId".
    private static Object read(Object row, String property) throws Exception {
        try {
            return row.getClass().getMethod("get" + property).invoke(row);
        } catch (NoSuchMethodException e) {
            return row.getClass().getMethod("is" + property).invoke(row);
        }
    }
}
