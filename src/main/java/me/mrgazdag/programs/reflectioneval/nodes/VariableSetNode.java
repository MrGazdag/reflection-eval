package me.mrgazdag.programs.reflectioneval.nodes;

import me.mrgazdag.programs.reflectioneval.ExecutionContext;
import me.mrgazdag.programs.reflectioneval.FormatType;
import me.mrgazdag.programs.reflectioneval.OutputResult;
import me.mrgazdag.programs.reflectioneval.Utils;
import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;
import me.mrgazdag.programs.reflectioneval.exception.ParseException;
import me.mrgazdag.programs.reflectioneval.text.StyledText;

public class VariableSetNode extends Node {
    private final String variableName;
    private Node value;

    public VariableSetNode(ExecutionContext context, String variableName) {
        super(context);
        this.variableName = variableName;
        this.value = null;
    }

    @Override
    public Class<?> getReturnType() {
        if (!context.getVariables().containsKey(variableName))
            throw new ParseException(new StyledText("Unknown variable " + variableName));
        return context.getVariables().get(variableName).clazz;
    }

    public void setValue(Node value) {
        this.value = value;
    }

    @Override
    public OutputResult execute() throws ExecuteException {
        if (!context.getVariables().containsKey(variableName))
            throw new ParseException(new StyledText("Unknown variable " + variableName));

        OutputResult result = value == null ? null : value.execute();
        OutputResult oldValue = context.getVariables().get(variableName);

        if (result != null && !oldValue.clazz.isAssignableFrom(result.clazz)) {
            throw new ExecuteException(exceptionToMessage(new ClassCastException(result.clazz.getName() + " cannot be cast to " + oldValue.clazz.getName())));
        }

        //keep old type
        OutputResult newValue = new OutputResult(context, result == null ? null : result.object, oldValue.clazz, Utils.texts(
                context.getDisplayFormat().text(FormatType.VARNAME, variableName),
                context.getDisplayFormat().text(FormatType.SPACE, " "),
                context.getDisplayFormat().text(FormatType.OPERATOR, "="),
                context.getDisplayFormat().text(FormatType.SPACE, " "),
                (result == null ? context.getDisplayFormat().text(FormatType.KEYWORD, "null") : result.inputText)
        ));
        context.getVariables().put(variableName, newValue);
        return newValue;
    }
}