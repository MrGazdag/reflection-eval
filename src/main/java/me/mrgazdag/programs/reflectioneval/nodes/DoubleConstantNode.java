package me.mrgazdag.programs.reflectioneval.nodes;

import me.mrgazdag.programs.reflectioneval.ExecutionContext;
import me.mrgazdag.programs.reflectioneval.FormatType;
import me.mrgazdag.programs.reflectioneval.OutputResult;
import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;

public class DoubleConstantNode extends Node {
    private final double value;

    public DoubleConstantNode(ExecutionContext context, double value) {
        super(context);
        this.value = value;
    }

    @Override
    public Class<?> getReturnType() {
        return double.class;
    }

    @Override
    public OutputResult execute() throws ExecuteException {
        return new OutputResult(context, value, double.class, context.getDisplayFormat().text(FormatType.NUMBER, value + "d"));
    }
}