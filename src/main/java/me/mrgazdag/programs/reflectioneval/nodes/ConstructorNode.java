package me.mrgazdag.programs.reflectioneval.nodes;

import me.mrgazdag.programs.reflectioneval.ExecutionContext;
import me.mrgazdag.programs.reflectioneval.FormatType;
import me.mrgazdag.programs.reflectioneval.OutputResult;
import me.mrgazdag.programs.reflectioneval.Utils;
import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;
import me.mrgazdag.programs.reflectioneval.exception.ParseException;
import me.mrgazdag.programs.reflectioneval.text.StyledText;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class ConstructorNode extends Node {
    private final List<Node> parameters;
    private final Class<?> staticClass;
    private Constructor<?> constructor;

    public ConstructorNode(ExecutionContext context, Class<?> staticClass) {
        super(context);
        this.parameters = new ArrayList<>();
        this.staticClass = staticClass;
    }

    public void addParameter(Node node) {
        parameters.add(node);
        this.constructor = null;
    }

    private void acquireConstructor() {
        if (constructor != null) return;

        Class<?>[] resultTypes = new Class[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            OutputResult result = parameters.get(i).execute();
            resultTypes[i] = result.clazz;
        }

        Constructor<?> constructor = Utils.findConstructor(staticClass, resultTypes);
        if (constructor == null)
            throw new ParseException(exceptionToMessage(new NoSuchMethodException(Utils.method(context, Utils.constructor(context, staticClass, resultTypes), resultTypes))));


        if (!Modifier.isPublic(constructor.getModifiers())) {
            if (context.isPubliconly()) {
                //   - if its not public, and the publiconly setting is disabled, throw an IllegalAccessException
                throw new ParseException(exceptionToMessage(new IllegalAccessException("Constructor '" + Utils.constructor(context, staticClass, resultTypes) + "' is not public")));
            }
        }
        this.constructor = constructor;
    }

    @Override
    public Class<?> getReturnType() {
        return staticClass;
    }

    @Override
    public OutputResult execute() throws ExecuteException {
        //order of operations:
        // - execute all parameter nodes
        // - collect all of the parameter classes
        // - collect all of the parameter objects
        // - get constructor instance
        //   - if not found, throw a NoSuchMethodException
        //   - if its not public, and the publiconly setting is disabled, throw an IllegalAccessException
        // - get return object
        //   - if an exception happens, display that exception
        // - return the returned object


        acquireConstructor();

        StyledText[] components = new StyledText[parameters.size()];
        Class<?>[] resultTypes = new Class[parameters.size()];
        Object[] results = new Object[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            OutputResult result = parameters.get(i).execute();
            results[i] = result.object;
            components[i] = result.inputText;
            resultTypes[i] = result.clazz;
        }

        if (!Modifier.isPublic(constructor.getModifiers()) && !context.isPubliconly()) {
            constructor.setAccessible(true);
            Utils.warning(context, "Constructor " + Utils.constructor(context, staticClass, resultTypes) + " is " + Utils.accessLevel(constructor));
        }

        try {
            // - get return object
            Object result;
            try {
                result = constructor.newInstance(results);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }

            if (!Modifier.isPublic(constructor.getModifiers()) && !context.isPubliconly()) {
                constructor.setAccessible(false);
            }
            StyledText text = Utils.texts(
                    context.getDisplayFormat().text(FormatType.KEYWORD, "new"),
                    context.getDisplayFormat().text(FormatType.SPACE, " "),
                    context.getDisplayFormat().text(FormatType.CLASS, Utils.className(context, staticClass)),
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
            return new OutputResult(context, result, staticClass, text);
        } catch (Throwable e) {
            //   - if an exception happens, display that exception
            throw new ExecuteException(new StyledText("Exception while invoking constructor:").append(exceptionToMessage(e)));
        }
    }
}