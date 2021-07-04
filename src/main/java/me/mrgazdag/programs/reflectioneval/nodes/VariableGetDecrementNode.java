package me.mrgazdag.programs.reflectioneval.nodes;

import me.mrgazdag.programs.reflectioneval.ExecutionContext;
import me.mrgazdag.programs.reflectioneval.FormatType;
import me.mrgazdag.programs.reflectioneval.OutputResult;
import me.mrgazdag.programs.reflectioneval.Utils;
import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;
import me.mrgazdag.programs.reflectioneval.exception.ParseException;
import me.mrgazdag.programs.reflectioneval.text.StyledText;

import java.awt.*;

public class VariableGetDecrementNode extends Node {
    private final String variableName;

    public VariableGetDecrementNode(ExecutionContext context, String variableName) {
        super(context);
        this.variableName = variableName;
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

        OutputResult varValue = context.getVariables().get(variableName);
        Class<?> c = varValue.clazz;
        Object result;
        if (c == byte.class) result = ((byte) varValue.object) - 1;
        else if (c == Byte.class) result = ((Byte) varValue.object) - 1;
        else if (c == int.class) result = ((int) varValue.object) - 1;
        else if (c == Integer.class) result = ((Integer) varValue.object) - 1;
        else if (c == double.class) result = ((double) varValue.object) - 1;
        else if (c == Double.class) result = ((Double) varValue.object) - 1;
        else if (c == float.class) result = ((float) varValue.object) - 1;
        else if (c == Float.class) result = ((Float) varValue.object) - 1;
        else if (c == char.class) result = ((char) varValue.object) - 1;
        else if (c == Character.class) result = ((Character) varValue.object) - 1;
        else if (c == short.class) result = ((short) varValue.object) - 1;
        else if (c == Short.class) result = ((Short) varValue.object) - 1;
        else if (c == long.class) result = ((long) varValue.object) - 1;
        else if (c == Long.class) result = ((Long) varValue.object) - 1;
        else throw new ExecuteException(new StyledText("Cannot decrement " + c.getSimpleName()));
        OutputResult outputResultOldValue = new OutputResult(context, varValue.object, varValue.clazz, Utils.texts(
                context.getDisplayFormat().text(FormatType.VARNAME, variableName),
                context.getDisplayFormat().text(FormatType.OPERATOR, "--")
        ));
        context.getVariables().put(variableName, new OutputResult(context, result, c, varValue.inputText));
        return outputResultOldValue;
    }
}