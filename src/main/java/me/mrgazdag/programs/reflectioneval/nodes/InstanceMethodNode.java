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
import java.util.function.Supplier;

public class InstanceMethodNode extends Node {
    private final List<Node> parameters;
    private final Supplier<OutputResult> toInvoke;
    private final Class<?> instanceClass;
    private final String methodName;
    private Method method;

    public InstanceMethodNode(ExecutionContext context, Supplier<OutputResult> toInvoke, String methodName, Class<?> instanceClass) {
        super(context);
        this.parameters = new ArrayList<>();
        this.methodName = methodName;
        this.toInvoke = toInvoke;
        this.instanceClass = instanceClass;
    }

    private Method acquireMethod() {
        if (method != null) return method;

        Class<?>[] resultTypes = new Class[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            OutputResult result = parameters.get(i).execute();
            resultTypes[i] = result.clazz;
        }

        Method method = Utils.findMethod(instanceClass, methodName, resultTypes);
        if (method == null)
            throw new ParseException(exceptionToMessage(new NoSuchMethodException(Utils.method(context, methodName, resultTypes))));

        if (!Modifier.isPublic(method.getModifiers())) {
            if (context.isPubliconly()) {
                //   - if its not public, and the publiconly setting is disabled, throw an IllegalAccessException
                throw new ExecuteException(exceptionToMessage(new IllegalAccessException("Method '" + methodName + "' is not public")));
            }
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

    public void addParameter(Node node) {
        parameters.add(node);
        this.method = null;
    }

    @Override
    public OutputResult execute() throws ExecuteException {
        //order of operations:
        // - get instance object
        //   - if it throws an exception, throw that
        //   - if it is null, throw a NullPointerException
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


        OutputResult instance;
        try {
            instance = toInvoke.get();
        } catch (Throwable e) {
            throw new ExecuteException(new StyledText("Could not get instance: ").append(exceptionToMessage(e)));
        }

        if (instance.object == null)
            throw new ExecuteException(exceptionToMessage(new NullPointerException("null")));

        acquireMethod();
        if (Modifier.isStatic(method.getModifiers())) {
            //   - if its static, warn user
            //throw new CommandRuntimeException(exceptionToMessage(new IllegalAccessException("Method '" + methodName + "' is static")));
            Utils.warning(context, "Method is static");
            instance = null;
        }

        StyledText[] components = new StyledText[parameters.size()];
        Object[] results = new Object[parameters.size()];
        Class<?>[] resultTypes = new Class[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            OutputResult result = parameters.get(i).execute();
            results[i] = result.object;
            components[i] = result.inputText;
            resultTypes[i] = result.clazz;
        }

        if (!Modifier.isPublic(method.getModifiers()) && !context.isPubliconly()) {
            method.setAccessible(true);
            Utils.warning(context, "Method " + Utils.className(context, instanceClass) + "." + Utils.method(context, methodName, resultTypes) + " is " + Utils.accessLevel(method));
        }


        Class<?> resultType = method.getReturnType();
        try {
            Object result;
            try {
                result = method.invoke(instance == null ? null : instance.object, results);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
            if (!Modifier.isPublic(method.getModifiers()) && !context.isPubliconly()) {
                method.setAccessible(false);
            }
            StyledText text = Utils.texts(
                    instance == null ? context.getDisplayFormat().text(FormatType.CLASS, Utils.className(context, instanceClass)) : instance.inputText,
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
            return new OutputResult(context, result, resultType, text);
        } catch (Throwable e) {
            throw new ExecuteException(new StyledText("Exception while invoking method: ").append(exceptionToMessage(e)));
        }
    }
}