package me.mrgazdag.programs.reflectioneval.nodes;

import me.mrgazdag.programs.reflectioneval.ExecutionContext;
import me.mrgazdag.programs.reflectioneval.FormatType;
import me.mrgazdag.programs.reflectioneval.OutputResult;
import me.mrgazdag.programs.reflectioneval.Utils;
import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;
import me.mrgazdag.programs.reflectioneval.exception.ParseException;
import me.mrgazdag.programs.reflectioneval.text.StyledText;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class StaticMethodNode extends Node {
    private final List<Node> parameters;
    private final Class<?> staticClass;
    private final String methodName;
    private Method method;

    public StaticMethodNode(ExecutionContext context, String methodName, Class<?> staticClass) {
        super(context);
        this.parameters = new ArrayList<>();
        this.methodName = methodName;
        this.staticClass = staticClass;
    }

    public void addParameter(Node node) {
        parameters.add(node);
        this.method = null;
    }

    private Method acquireMethod() {
        if (method != null) return method;

        Class<?>[] resultTypes = new Class[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            OutputResult result = parameters.get(i).execute();
            resultTypes[i] = result.clazz;
        }

        Method method = Utils.findMethod(staticClass, methodName, resultTypes);
        if (method == null)
            throw new ParseException(exceptionToMessage(new NoSuchMethodException(Utils.method(context, methodName, resultTypes))));


        if (!Modifier.isPublic(method.getModifiers())) {
            if (context.isPubliconly()) {
                //   - if its not public, and the publiconly setting is disabled, throw an IllegalAccessException
                throw new ParseException(exceptionToMessage(new IllegalAccessException("Method '" + methodName + "' is not public")));
            }
        }

        if (!Modifier.isStatic(method.getModifiers())) {
            //   - if its not static, throw an IllegalAccessException
            throw new ParseException(exceptionToMessage(new IllegalAccessException("Method '" + methodName + "' is not static")));
        }
        this.method = method;
        return method;
    }

    @Override
    public Class<?> getReturnType() {
        if (this.method == null) {
            Method m = acquireMethod();
            if (m != null) return m.getReturnType();
            else return super.getReturnType();
        } else return this.method.getReturnType();
    }

    @Override
    public OutputResult execute() throws ExecuteException {
        //order of operations:
        // - execute all parameter nodes
        // - collect all of the parameter classes
        // - collect all of the parameter objects
        // - get method instance
        //   - if not found, throw a NoSuchMethodException
        //   - if its not public, and the publiconly setting is disabled, throw an IllegalAccessException
        //   - if its not static, throw an IllegalAccessException
        // - get return class
        // - get return object
        //   - if an exception happens, display that exception
        // - return the returned object


        acquireMethod();

        StyledText[] components = new StyledText[parameters.size()];
        Class<?>[] resultTypes = new Class[parameters.size()];
        Object[] results = new Object[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            OutputResult result = parameters.get(i).execute();
            results[i] = result.object;
            components[i] = result.inputText;
            resultTypes[i] = result.clazz;
        }

        if (!Modifier.isPublic(method.getModifiers()) && !context.isPubliconly()) {
            method.setAccessible(true);
            Utils.warning(context, "Method " + Utils.className(context, staticClass) + "." + Utils.method(context, methodName, resultTypes) + " is " + Utils.accessLevel(method));
        }
        if (!Modifier.isPublic(method.getDeclaringClass().getModifiers()) && !context.isPubliconly()) {
            method.setAccessible(true);
            Utils.warning(context, "Class " + Utils.className(context, method.getDeclaringClass()) + " is " + Utils.accessLevel(method.getDeclaringClass()));
        }

        // - get return class
        Class<?> resultType = method.getReturnType();
        try {
            // - get return object
            Object result;
            try {
                result = method.invoke(null, results);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }

            if (!Modifier.isPublic(method.getModifiers()) && !context.isPubliconly()) {
                method.setAccessible(false);
            }
            StyledText text = Utils.texts(
                    context.getDisplayFormat().text(FormatType.CLASS, Utils.className(context, staticClass)),
                    context.getDisplayFormat().text(FormatType.METHOD_DOT, "."),
                    context.getDisplayFormat().text(FormatType.STATIC_METHOD, method.getName()),
                    context.getDisplayFormat().text(FormatType.PARENTHESES, "(")
            );
            boolean first = true;
            for (StyledText component : components) {
                if (first) {
                    first = false;
                } else {
                    text.append(context.getDisplayFormat().text(FormatType.COMMA, ", "));
                }
                text.append(component);
            }

            text.append(context.getDisplayFormat().text(FormatType.PARENTHESES, ")"));

            // - return the returned object
            return new OutputResult(context, result, resultType, text);
        } catch (Throwable e) {
            //   - if an exception happens, display that exception
            throw new ExecuteException(new StyledText("Exception while invoking method:").append(exceptionToMessage(e)));
        }
    }
}