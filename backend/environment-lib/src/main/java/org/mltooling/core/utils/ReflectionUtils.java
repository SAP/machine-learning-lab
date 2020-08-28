package org.mltooling.core.utils;

import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;


public class ReflectionUtils {
    // ================ Constants =========================================== //

    // ================ Members ============================================= //

    // ================ Constructors & Main ================================= //

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //
    @SuppressWarnings("unchecked")
    public static <T> T invokeSuperMethod(Object instance, String methodName, Object... params) throws Exception {
        return invokeMethod(instance.getClass().getSuperclass(), instance, methodName, params);
    }

    public static <T> T invokeMethod(Object instance, String methodName, Object... params) throws Exception {
        return invokeMethod(instance.getClass(), instance, methodName, params);
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeMethod(Class clazz, Object instance, String methodName, Object... params) throws Exception {

        Method[] allMethods = clazz.getDeclaredMethods();

        if (allMethods != null && allMethods.length > 0) {

            Class[] paramClasses = Arrays.stream(params).map(p -> p != null ? p.getClass() : null).toArray(Class[]::new);

            for (Method method : allMethods) {
                String currentMethodName = method.getName();
                if (!currentMethodName.equals(methodName)) {
                    continue;
                }
                Type[] pTypes = method.getParameterTypes();
                if (pTypes.length == paramClasses.length) {
                    boolean goodMethod = true;
                    int i = 0;
                    for (Type pType : pTypes) {
                        if (!ClassUtils.isAssignable(paramClasses[i++], (Class<?>) pType)) {
                            goodMethod = false;
                            break;
                        }
                    }
                    if (goodMethod) {
                        method.setAccessible(true);
                        return (T) method.invoke(instance, params);
                    }
                }
            }

            throw new Exception("There are no methods found with name " + methodName + " and params " +
                                        Arrays.toString(paramClasses));
        }

        throw new Exception("There are no methods found with name " + methodName);
    }

    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
