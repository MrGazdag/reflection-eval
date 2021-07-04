package me.mrgazdag.programs.reflectioneval.nodes;

import me.mrgazdag.programs.reflectioneval.ExecutionContext;
import me.mrgazdag.programs.reflectioneval.FormatType;
import me.mrgazdag.programs.reflectioneval.OutputResult;
import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;

public class BooleanConstantNode extends Node {
    private final boolean value;

    public BooleanConstantNode(ExecutionContext context, boolean value) {
        super(context);
        this.value = value;
    }

    @Override
    public Class<?> getReturnType() {
        return boolean.class;
    }

    @Override
    public OutputResult execute() throws ExecuteException {
        return new OutputResult(context, value, boolean.class, context.getDisplayFormat().text(FormatType.KEYWORD, value + ""));
    }
}