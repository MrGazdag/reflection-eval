package me.mrgazdag.programs.reflectioneval.nodes;

import me.mrgazdag.programs.reflectioneval.ExecutionContext;
import me.mrgazdag.programs.reflectioneval.FormatType;
import me.mrgazdag.programs.reflectioneval.OutputResult;
import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;

public class LongConstantNode extends Node {
    private final long value;

    public LongConstantNode(ExecutionContext context, long value) {
        super(context);
        this.value = value;
    }

    @Override
    public Class<?> getReturnType() {
        return long.class;
    }

    @Override
    public OutputResult execute() throws ExecuteException {
        return new OutputResult(context, value, long.class, context.getDisplayFormat().text(FormatType.NUMBER, value + "L"));
    }
}