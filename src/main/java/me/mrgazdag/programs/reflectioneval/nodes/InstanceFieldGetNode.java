package me.mrgazdag.programs.reflectioneval.nodes;

import me.mrgazdag.programs.reflectioneval.ExecutionContext;
import me.mrgazdag.programs.reflectioneval.FormatType;
import me.mrgazdag.programs.reflectioneval.OutputResult;
import me.mrgazdag.programs.reflectioneval.Utils;
import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;
import me.mrgazdag.programs.reflectioneval.exception.ParseException;
import me.mrgazdag.programs.reflectioneval.text.StyledText;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

public class InstanceFieldGetNode extends Node {
    private final Supplier<OutputResult> toInvoke;
    private final Class<?> instanceClass;
    private final String fieldName;
    private Field field;

    public InstanceFieldGetNode(ExecutionContext context, Supplier<OutputResult> toInvoke, String fieldName, Class<?> instanceClass) {
        super(context);
        this.fieldName = fieldName;
        this.toInvoke = toInvoke;
        this.instanceClass = instanceClass;
        acquireField();
    }

    private Field acquireField() {
        if (field != null) return field;

        Field field = Utils.findField(instanceClass, fieldName);
        if (field == null) throw new ParseException(exceptionToMessage(new NoSuchFieldException(fieldName)));

        if (!Modifier.isPublic(field.getModifiers())) {
            if (context.isPubliconly()) {
                //   - if its not public, and the publiconly setting is disabled, throw an IllegalAccessException
                throw new ParseException(exceptionToMessage(new IllegalAccessException("Field '" + fieldName + "' is not public")));
            }
        }

        this.field = field;
        return field;
    }

    @Override
    public Class<?> getReturnType() {
        if (this.field == null) {
            Field f = acquireField();
            if (f != null) return f.getType();
            else return super.getReturnType();
        } else return this.field.getType();
    }

    @Override
    public OutputResult execute() throws ExecuteException {
        //order of operations:
        // - get instance object
        //   - if it throws an exception, throw that
        //   - if it is null, throw a NullPointerException
        // - get field instance
        //   - if not found, throw a NoSuchFieldException
        //   - if its not public, and the publiconly setting is disabled, throw an IllegalAccessException
        //   - if its not static, throw an IllegalAccessException
        // - get field type
        // - get field contents
        //   - if an exception happens, display that exception
        // - return the returned object


        OutputResult instance;
        try {
            instance = toInvoke.get();
        } catch (Throwable e) {
            throw new ExecuteException(new StyledText("Could not get instance: ").append(exceptionToMessage(e)));
        }

        if (instance == null) throw new ExecuteException(exceptionToMessage(new NullPointerException("null")));

        acquireField();
        if (!Modifier.isPublic(field.getModifiers()) && !context.isPubliconly()) {
            field.setAccessible(true);
            Utils.warning(context, "Field " + Utils.className(context, instanceClass) + "." + fieldName + " is " + Utils.accessLevel(field));
        }
        if (Modifier.isStatic(field.getModifiers())) {
            //   - if its static, warn user
            //throw new CommandRuntimeException(exceptionToMessage(new IllegalAccessException("Method '" + methodName + "' is static")));
            Utils.warning(context, "Field is static");
            instance = null;
        }

        Class<?> resultType = field.getType();
        try {
            Object result = field.get(instance == null ? null : instance.object);
            if (!Modifier.isPublic(field.getModifiers()) && !context.isPubliconly()) {
                field.setAccessible(false);
            }
            return new OutputResult(context, result, resultType, Utils.texts(
                    instance == null ? context.getDisplayFormat().text(FormatType.CLASS, Utils.className(context, instanceClass)) : instance.inputText,
                    context.getDisplayFormat().text(FormatType.METHOD_DOT, "."),
                    context.getDisplayFormat().text(FormatType.STATIC_METHOD, fieldName)
            ));
        } catch (Exception e) {
            throw new ExecuteException(new StyledText("Exception while getting field:").append(exceptionToMessage(e)));
        }
    }
}