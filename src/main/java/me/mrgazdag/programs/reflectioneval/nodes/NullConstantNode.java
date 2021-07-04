package me.mrgazdag.programs.reflectioneval.nodes;

import me.mrgazdag.programs.reflectioneval.ExecutionContext;
import me.mrgazdag.programs.reflectioneval.FormatType;
import me.mrgazdag.programs.reflectioneval.OutputResult;
import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;

public class NullConstantNode extends Node {
    public NullConstantNode(ExecutionContext context) {
        super(context);
    }

    @Override
    public OutputResult execute() throws ExecuteException {
        return new OutputResult(context, null, Object.class, context.getDisplayFormat().text(FormatType.KEYWORD, "null"));
    }
}