package org.mltooling.core.utils;

import java.util.*;


public final class ListUtils {
    // ================ Constants =========================================== //

    // ================ Members ============================================= //

    // ================ Constructors & Main ================================= //
    private ListUtils() {
    }

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //

    public static <T> List<T> initializeList(T... items) {
        List<T> list = new ArrayList<T>();
        if (items.length > 0) {
            list.addAll(Arrays.asList(items));
        }
        return list;
    }

    public static <T> Set<T> initializeSet(T... items) {
        Set<T> list = new HashSet<>();
        if (items.length > 0) {
            list.addAll(Arrays.asList(items));
        }
        return list;
    }

    public static List<String> lowercaseList(Collection<String> stringList) {
        List<String> lowercaseList = new ArrayList<>();
        for (String item : stringList) {
            lowercaseList.add(item.trim().toLowerCase());
        }
        return lowercaseList;
    }

    public static <T, S extends T> List<T> castList(List<S> list, Class<T> classType) {
        return (List<T>) (List<?>) list;
    }

    public static <T> boolean isNullOrEmpty(Collection<T> list) {
        if (list == null || list.isEmpty()) {
            return true;
        }
        return false;
    }

    public static <T> boolean isNullOrEmpty(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return true;
        }
        return false;
    }

    public static <T> boolean isNullOrEmpty(T[] array) {
        if (array == null || array.length <= 0) {
            return true;
        }
        return false;
    }

    public static <T> List<T> shorten(List<T> list, int maxSize) {
        if (isNullOrEmpty(list)) {
            return list;
        }

        int k = list.size();
        if (k > maxSize) {
            list.subList(maxSize, k).clear();
        }
        return list;
    }

    public static <T> String getAsString(Collection<T> values, boolean fieldsInQuotes) {
        return getAsString(values, fieldsInQuotes, true);
    }

    public static <T> String getAsString(Collection<T> values, boolean fieldsInQuotes, boolean enclosedWithBrackets) {
        String result = (enclosedWithBrackets) ? "[" : "";
        for (T value : values) {
            if (fieldsInQuotes) {
                result += "\"" + value + "\", ";
            } else {
                result += "" + value + ", ";
            }
        }
        result = StringUtils.removeLastComma(result.trim());
        if (enclosedWithBrackets) {
            result += "]";
        }
        return result;
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValue(Map<K, V> map, boolean ascending) {
        List<Map.Entry<K, V>> list =
                new LinkedList<>(map.entrySet());

        if (ascending) {
            Collections.sort(list, new Comparator<Map.Entry<K, V>>() {

                @Override
                public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                    return (o1.getValue()).compareTo(o2.getValue());
                }
            });
        } else {
            Collections.sort(list, new Comparator<Map.Entry<K, V>>() {

                @Override
                public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                    return (o2.getValue()).compareTo(o1.getValue());
                }
            });
        }

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
