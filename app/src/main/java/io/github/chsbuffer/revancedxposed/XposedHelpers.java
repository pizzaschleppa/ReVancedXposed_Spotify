package io.github.chsbuffer.revancedxposed;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clean, modern reflection helper providing drop-in compatibility with XposedHelpers methods
 * without dependency on legacy de.robv.android.xposed runtime classes.
 */
@SuppressWarnings("unused")
public final class XposedHelpers {
    private static final ConcurrentHashMap<String, Field> fieldCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Method> methodCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Constructor<?>> constructorCache = new ConcurrentHashMap<>();

    private XposedHelpers() {}

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(new ClassNotFoundException("Class " + className + " not found", e));
        }
    }

    public static Class<?> findClassIfExists(String className, ClassLoader classLoader) {
        try {
            return findClass(className, classLoader);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Field findField(Class<?> clazz, String fieldName) {
        String key = clazz.getName() + "#" + fieldName;
        Field cached = fieldCache.get(key);
        if (cached != null) return cached;

        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                Field f = current.getDeclaredField(fieldName);
                f.setAccessible(true);
                fieldCache.put(key, f);
                return f;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldError(clazz.getName() + "#" + fieldName);
    }

    public static Field findFieldIfExists(Class<?> clazz, String fieldName) {
        try {
            return findField(clazz, fieldName);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Field findFirstFieldByExactType(Class<?> clazz, Class<?> type) {
        String key = clazz.getName() + "#type=" + type.getName();
        Field cached = fieldCache.get(key);
        if (cached != null) return cached;

        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                if (f.getType().equals(type)) {
                    f.setAccessible(true);
                    fieldCache.put(key, f);
                    return f;
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchFieldError("Field of type " + type.getName() + " in " + clazz.getName());
    }

    public static Object getObjectField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).get(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setObjectField(Object obj, String fieldName, Object value) {
        try {
            findField(obj.getClass(), fieldName).set(obj, value);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static int getIntField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).getInt(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setIntField(Object obj, String fieldName, int value) {
        try {
            findField(obj.getClass(), fieldName).setInt(obj, value);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static long getLongField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).getLong(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setLongField(Object obj, String fieldName, long value) {
        try {
            findField(obj.getClass(), fieldName).setLong(obj, value);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static boolean getBooleanField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).getBoolean(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setBooleanField(Object obj, String fieldName, boolean value) {
        try {
            findField(obj.getClass(), fieldName).setBoolean(obj, value);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static float getFloatField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).getFloat(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setFloatField(Object obj, String fieldName, float value) {
        try {
            findField(obj.getClass(), fieldName).setFloat(obj, value);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static Object getStaticObjectField(Class<?> clazz, String fieldName) {
        try {
            return findField(clazz, fieldName).get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) {
        try {
            findField(clazz, fieldName).set(null, value);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static Method findMethodExact(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        String key = clazz.getName() + "#" + methodName + Arrays.toString(parameterTypes);
        Method cached = methodCache.get(key);
        if (cached != null) return cached;

        Class<?> current = clazz;
        while (current != null) {
            try {
                Method m = current.getDeclaredMethod(methodName, parameterTypes);
                m.setAccessible(true);
                methodCache.put(key, m);
                return m;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodError(clazz.getName() + "#" + methodName);
    }

    public static Method findMethodExact(String className, ClassLoader classLoader, String methodName, Object... parameterTypes) {
        Class<?> clazz = findClass(className, classLoader);
        Class<?>[] paramClasses = getParameterClasses(clazz.getClassLoader(), parameterTypes);
        return findMethodExact(clazz, methodName, paramClasses);
    }

    public static Method findMethodExactIfExists(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            return findMethodExact(clazz, methodName, parameterTypes);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Method findMethodExactIfExists(Class<?> clazz, String methodName, String... parameterTypeNames) {
        try {
            Class<?>[] paramClasses = new Class<?>[parameterTypeNames.length];
            for (int i = 0; i < parameterTypeNames.length; i++) {
                paramClasses[i] = findClass(parameterTypeNames[i], clazz.getClassLoader());
            }
            return findMethodExact(clazz, methodName, paramClasses);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Constructor<?> findConstructorExact(Class<?> clazz, Class<?>... parameterTypes) {
        String key = clazz.getName() + "#<init>" + Arrays.toString(parameterTypes);
        Constructor<?> cached = constructorCache.get(key);
        if (cached != null) return cached;

        try {
            Constructor<?> c = clazz.getDeclaredConstructor(parameterTypes);
            c.setAccessible(true);
            constructorCache.put(key, c);
            return c;
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
    }

    public static Constructor<?> findConstructorExactIfExists(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            return findConstructorExact(clazz, parameterTypes);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Constructor<?> findConstructorExactIfExists(Class<?> clazz, String... parameterTypeNames) {
        try {
            Class<?>[] paramClasses = new Class<?>[parameterTypeNames.length];
            for (int i = 0; i < parameterTypeNames.length; i++) {
                paramClasses[i] = findClass(parameterTypeNames[i], clazz.getClassLoader());
            }
            return findConstructorExact(clazz, paramClasses);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object callMethod(Object obj, String methodName, Object... args) {
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
        }
        Method m = findMethodBestMatch(obj.getClass(), methodName, paramTypes);
        try {
            return m.invoke(obj, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object callMethod(Object obj, String methodName, Class<?>[] parameterTypes, Object... args) {
        Method m = findMethodExact(obj.getClass(), methodName, parameterTypes);
        try {
            return m.invoke(obj, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
        }
        Method m = findMethodBestMatch(clazz, methodName, paramTypes);
        try {
            return m.invoke(null, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object... args) {
        Method m = findMethodExact(clazz, methodName, parameterTypes);
        try {
            return m.invoke(null, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object newInstance(Class<?> clazz, Object... args) {
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
        }
        Constructor<?> c = findConstructorBestMatch(clazz, paramTypes);
        try {
            return c.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object newInstance(Class<?> clazz, Class<?>[] parameterTypes, Object... args) {
        Constructor<?> c = findConstructorExact(clazz, parameterTypes);
        try {
            return c.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        Class<?> current = clazz;
        while (current != null) {
            for (Method m : current.getDeclaredMethods()) {
                if (m.getName().equals(methodName) && isAssignable(parameterTypes, m.getParameterTypes())) {
                    m.setAccessible(true);
                    return m;
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodError("No matching method " + methodName + " on " + clazz.getName());
    }

    private static Constructor<?> findConstructorBestMatch(Class<?> clazz, Class<?>... parameterTypes) {
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            if (isAssignable(parameterTypes, c.getParameterTypes())) {
                c.setAccessible(true);
                return c;
            }
        }
        throw new NoSuchMethodError("No matching constructor on " + clazz.getName());
    }

    private static boolean isAssignable(Class<?>[] from, Class<?>[] to) {
        if (from.length != to.length) return false;
        for (int i = 0; i < from.length; i++) {
            if (from[i] == null) continue;
            if (to[i].isPrimitive()) {
                if (!wrapPrimitive(to[i]).isAssignableFrom(from[i])) return false;
            } else if (!to[i].isAssignableFrom(from[i])) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> wrapPrimitive(Class<?> primitive) {
        if (primitive == int.class) return Integer.class;
        if (primitive == long.class) return Long.class;
        if (primitive == boolean.class) return Boolean.class;
        if (primitive == byte.class) return Byte.class;
        if (primitive == char.class) return Character.class;
        if (primitive == short.class) return Short.class;
        if (primitive == float.class) return Float.class;
        if (primitive == double.class) return Double.class;
        return primitive;
    }

    public static Class<?>[] getParameterClasses(ClassLoader classLoader, Object[] parameterTypes) {
        Class<?>[] paramClasses = new Class<?>[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i] instanceof Class<?>) {
                paramClasses[i] = (Class<?>) parameterTypes[i];
            } else if (parameterTypes[i] instanceof String) {
                paramClasses[i] = findClass((String) parameterTypes[i], classLoader);
            } else {
                throw new IllegalArgumentException("Parameter type must be Class or String");
            }
        }
        return paramClasses;
    }

    public static io.github.libxposed.api.XposedInterface.HookHandle findAndHookMethod(
            Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        if (parameterTypesAndCallback.length == 0) {
            throw new IllegalArgumentException("No callback provided");
        }
        Object callbackObj = parameterTypesAndCallback[parameterTypesAndCallback.length - 1];
        Class<?>[] paramClasses = new Class<?>[parameterTypesAndCallback.length - 1];
        for (int i = 0; i < paramClasses.length; i++) {
            Object p = parameterTypesAndCallback[i];
            if (p instanceof Class<?>) {
                paramClasses[i] = (Class<?>) p;
            } else if (p instanceof String) {
                paramClasses[i] = findClass((String) p, clazz.getClassLoader());
            } else {
                throw new IllegalArgumentException("Parameter type must be Class or String");
            }
        }
        Method m = findMethodExact(clazz, methodName, paramClasses);
        if (callbackObj instanceof XC_MethodHook) {
            return HelperKt.hookMethod((java.lang.reflect.Executable) m, (XC_MethodHook) callbackObj);
        }
        throw new IllegalArgumentException("Callback must be XC_MethodHook");
    }

    public static io.github.libxposed.api.XposedInterface.HookHandle findAndHookMethod(
            String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        Class<?> clazz = findClass(className, classLoader);
        return findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
    }

    public static io.github.libxposed.api.XposedInterface.HookHandle findAndHookConstructor(
            Class<?> clazz, Object... parameterTypesAndCallback) {
        if (parameterTypesAndCallback.length == 0) {
            throw new IllegalArgumentException("No callback provided");
        }
        Object callbackObj = parameterTypesAndCallback[parameterTypesAndCallback.length - 1];
        Class<?>[] paramClasses = new Class<?>[parameterTypesAndCallback.length - 1];
        for (int i = 0; i < paramClasses.length; i++) {
            Object p = parameterTypesAndCallback[i];
            if (p instanceof Class<?>) {
                paramClasses[i] = (Class<?>) p;
            } else if (p instanceof String) {
                paramClasses[i] = findClass((String) p, clazz.getClassLoader());
            } else {
                throw new IllegalArgumentException("Parameter type must be Class or String");
            }
        }
        Constructor<?> c = findConstructorExact(clazz, paramClasses);
        if (callbackObj instanceof XC_MethodHook) {
            return HelperKt.hookMethod((java.lang.reflect.Executable) c, (XC_MethodHook) callbackObj);
        }
        throw new IllegalArgumentException("Callback must be XC_MethodHook");
    }

    public static io.github.libxposed.api.XposedInterface.HookHandle findAndHookConstructor(
            String className, ClassLoader classLoader, Object... parameterTypesAndCallback) {
        Class<?> clazz = findClass(className, classLoader);
        return findAndHookConstructor(clazz, parameterTypesAndCallback);
    }
}