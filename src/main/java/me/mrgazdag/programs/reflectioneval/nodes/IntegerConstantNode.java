package me.mrgazdag.programs.reflectioneval.nodes;

import me.mrgazdag.programs.reflectioneval.ExecutionContext;
import me.mrgazdag.programs.reflectioneval.FormatType;
import me.mrgazdag.programs.reflectioneval.OutputResult;
import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;

public class IntegerConstantNode extends Node {
    private final int value;

    public IntegerConstantNode(ExecutionContext context, int value) {
        super(context);
        this.value = value;
    }

    @Override
    public Class<?> getReturnType() {
        return int.class;
    }

    @Override
    public OutputResult execute() throws ExecuteException {
        return new OutputResult(context, value, int.class, context.getDisplayFormat().text(FormatType.NUMBER, value + ""));
    }
}