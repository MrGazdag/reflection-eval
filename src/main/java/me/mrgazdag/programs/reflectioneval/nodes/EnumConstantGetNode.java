package me.mrgazdag.programs.reflectioneval.nodes;

import me.mrgazdag.programs.reflectioneval.ExecutionContext;
import me.mrgazdag.programs.reflectioneval.FormatType;
import me.mrgazdag.programs.reflectioneval.OutputResult;
import me.mrgazdag.programs.reflectioneval.Utils;
import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;

public class EnumConstantGetNode extends Node {
    private final Class<?> enumClass;
    private final Enum<?> enumConstant;

    public EnumConstantGetNode(ExecutionContext context, Enum<?> enumConstant, Class<?> enumClass) {
        super(context);
        this.enumClass = enumClass;
        this.enumConstant = enumConstant;
    }

    @Override
    public Class<?> getReturnType() {
        return enumClass;
    }

    @Override
    public OutputResult execute() throws ExecuteException {
        return new OutputResult(context, enumConstant, enumClass, Utils.texts(
                context.getDisplayFormat().text(FormatType.CLASS, Utils.className(context, enumClass)),
                context.getDisplayFormat().text(FormatType.METHOD_DOT, "."),
                context.getDisplayFormat().text(FormatType.ENUM_CONSTANT, enumConstant.name())
        ));
    }
}