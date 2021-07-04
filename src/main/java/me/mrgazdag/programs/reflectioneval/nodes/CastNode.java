package me.mrgazdag.programs.reflectioneval.nodes;

import me.mrgazdag.programs.reflectioneval.ExecutionContext;
import me.mrgazdag.programs.reflectioneval.FormatType;
import me.mrgazdag.programs.reflectioneval.OutputResult;
import me.mrgazdag.programs.reflectioneval.Utils;
import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;

public class CastNode extends Node {
    private final Class<?> destinationClass;
    private final Node value;

    public CastNode(ExecutionContext context, Class<?> destinationClass, Node value) {
        super(context);
        this.destinationClass = destinationClass;
        this.value = value;
    }

    @Override
    public Class<?> getReturnType() {
        return destinationClass;
    }

    @Override
    public OutputResult execute() throws ExecuteException {
        OutputResult old = value.execute();
        String castName = Utils.className(context, destinationClass);
        if (!destinationClass.isAssignableFrom(old.clazz)) {
            try {
                destinationClass.cast(old.object);
            } catch (ClassCastException e) {
                throw new ExecuteException(exceptionToMessage(new ClassCastException(old.clazz.getName() + " cannot be cast to " + destinationClass.getName())));
            }
        }
        return new OutputResult(context, old.object, destinationClass, Utils.texts(
                context.getDisplayFormat().text(FormatType.PARENTHESES, "(("),
                context.getDisplayFormat().text(FormatType.CLASS, castName),
                context.getDisplayFormat().text(FormatType.PARENTHESES, ")"),
                context.getDisplayFormat().text(FormatType.SPACE, " "),
                old.inputText,
                context.getDisplayFormat().text(FormatType.PARENTHESES, ")")
        ));
    }
}