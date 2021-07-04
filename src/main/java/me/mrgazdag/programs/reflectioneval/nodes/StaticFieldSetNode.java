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

public class StaticFieldSetNode extends Node {
    private final Class<?> instanceClass;
    private final String fieldName;
    private Field field;
    private Node value;

    public StaticFieldSetNode(ExecutionContext context, String fieldName, Class<?> instanceClass) {
        super(context);
        this.fieldName = fieldName;
        this.instanceClass = instanceClass;
        acquireField();
    }

    public void setValue(Node value) {
        this.value = value;
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


        acquireField();
        if (!Modifier.isPublic(field.getModifiers()) && !context.isPubliconly()) {
            field.setAccessible(true);
            Utils.warning(context, "Field " + Utils.className(context, instanceClass) + "." + fieldName + " is " + Utils.accessLevel(field));
        }
        if (!Modifier.isStatic(field.getModifiers())) {
            //   - if its static, throw error
            throw new ParseException(exceptionToMessage(new IllegalAccessException("Field '" + fieldName + "' is static")));
        }

        Class<?> resultType = field.getType();
        OutputResult valueObject = value.execute();
        if (!resultType.isAssignableFrom(valueObject.clazz)) {
            throw new ExecuteException(exceptionToMessage(new ClassCastException(valueObject.clazz.getName() + " cannot be cast to " + resultType.getName())));
        }
        try {
            field.set(null, valueObject.object);
            if (!Modifier.isPublic(field.getModifiers()) && !context.isPubliconly()) {
                field.setAccessible(false);
            }
            return new OutputResult(context, valueObject.object, resultType, Utils.texts(
                    context.getDisplayFormat().text(FormatType.CLASS, Utils.className(context, instanceClass)),
                    context.getDisplayFormat().text(FormatType.METHOD_DOT, "."),
                    context.getDisplayFormat().text(FormatType.STATIC_METHOD, fieldName),
                    context.getDisplayFormat().text(FormatType.SPACE, " "),
                    context.getDisplayFormat().text(FormatType.OPERATOR, "="),
                    context.getDisplayFormat().text(FormatType.SPACE, " "),
                    valueObject.inputText
            ));
        } catch (Exception e) {
            throw new ExecuteException(new StyledText("Exception while setting field: ").append(exceptionToMessage(e)));
        }
    }
}