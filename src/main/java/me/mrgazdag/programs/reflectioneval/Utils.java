package me.mrgazdag.programs.reflectioneval;

import jdk.jshell.execution.Util;
import me.mrgazdag.programs.reflectioneval.text.StyledText;

import java.awt.*;
import java.lang.reflect.*;

public class Utils {
    public static String method(ExecutionContext context, String name, Class<?>[] types) {
        StringBuilder sb = new StringBuilder(name);
        sb.append("(");
        boolean first = true;
        for (Class<?> type : types) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(className(context, type));
        }
        return sb.append(")").toString();
    }
    public static Method findMethod(Class<?> clazz, String name, Class<?>[] resultTypes) {
        try {
            return clazz.getDeclaredMethod(name, resultTypes);
        } catch (NoSuchMethodException ignored) {}
        for (Class<?> anInterface : clazz.getInterfaces()) {
            Method m = findMethod(anInterface, name, resultTypes);
            if (m != null) return m;
        }
        if (clazz.getSuperclass() != null && clazz != Object.class) return findMethod(clazz.getSuperclass(), name, resultTypes);
        return null;
    }

    public static Field findField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException ignored) {}
        for (Class<?> anInterface : clazz.getInterfaces()) {
            Field f = findField(anInterface, name);
            if (f != null) return f;
        }
        if (clazz.getSuperclass() != null && clazz != Object.class) return findField(clazz.getSuperclass(), name);
        return null;
    }

    public static StyledText texts(StyledText base, StyledText...siblings) {
        for (StyledText sibling : siblings) base.append(sibling);
        return base;
    }

    public static String className(ExecutionContext context, Class<?> clazz) {
        return context.getImports().get(clazz.getSimpleName()) == clazz ? clazz.getSimpleName() : clazz.getName();
    }

    public static void warning(ExecutionContext context, String msg) {
        context.getList().sendMessage(context.getDisplayFormat().text(FormatType.WARNING, "⚠ " + msg));
    }

    public static String accessLevel(Member member) {
        int mod = member.getModifiers();
        if (Modifier.isPublic(mod)) return "public";
        else if (Modifier.isPrivate(mod)) return "private";
        else if (Modifier.isProtected(mod)) return "protected";
        else return "package-private";
    }

    public static Constructor<?> findConstructor(Class<?> clazz, Class<?>[] resultTypes) {
        try {
            return clazz.getConstructor(resultTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static String constructor(ExecutionContext context, Class<?> staticClass, Class<?>[] types) {
        StringBuilder sb = new StringBuilder("new " + className(context,staticClass));
        sb.append("(");
        boolean first = true;
        for (Class<?> type : types) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(className(context, type));
        }
        return sb.append(")").toString();
    }
}
