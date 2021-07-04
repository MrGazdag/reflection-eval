package me.mrgazdag.programs.reflectioneval.nodes;

import me.mrgazdag.programs.reflectioneval.ExecutionContext;
import me.mrgazdag.programs.reflectioneval.FormatType;
import me.mrgazdag.programs.reflectioneval.OutputResult;
import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;
import me.mrgazdag.programs.reflectioneval.exception.ParseException;
import me.mrgazdag.programs.reflectioneval.text.StyledText;

public class VariableGetNode extends Node {
    private final String variableName;

    public VariableGetNode(ExecutionContext context, String variableName) {
        super(context);
        this.variableName = variableName;
    }

    public String getVariableName() {
        return variableName;
    }

    @Override
    public Class<?> getReturnType() {
        if (!context.getVariables().containsKey(variableName))
            throw new ParseException(new StyledText("Unknown variable " + variableName));
        return context.getVariables().get(variableName).clazz;
    }

    @Override
    public OutputResult execute() throws ExecuteException {
        if (!context.getVariables().containsKey(variableName))
            throw new ParseException(new StyledText("Unknown variable " + variableName));
        OutputResult varContents = context.getVariables().get(variableName);
        return new OutputResult(context, varContents.object, varContents.clazz, context.getDisplayFormat().text(FormatType.VARNAME, variableName));
    }
}