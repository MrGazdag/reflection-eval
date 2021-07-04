package me.mrgazdag.programs.reflectioneval.nodes;

import me.mrgazdag.programs.reflectioneval.ExecutionContext;
import me.mrgazdag.programs.reflectioneval.FormatType;
import me.mrgazdag.programs.reflectioneval.OutputResult;
import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;

public class FloatConstantNode extends Node {
    private final float value;

    public FloatConstantNode(ExecutionContext context, float value) {
        super(context);
        this.value = value;
    }

    @Override
    public Class<?> getReturnType() {
        return float.class;
    }

    @Override
    public OutputResult execute() throws ExecuteException {
        return new OutputResult(context, value, float.class, context.getDisplayFormat().text(FormatType.NUMBER, value + "f"));
    }
}