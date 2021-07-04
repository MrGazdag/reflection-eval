package me.mrgazdag.programs.reflectioneval.nodes;

import me.mrgazdag.programs.reflectioneval.ExecutionContext;
import me.mrgazdag.programs.reflectioneval.FormatType;
import me.mrgazdag.programs.reflectioneval.OutputResult;
import me.mrgazdag.programs.reflectioneval.Utils;
import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;
import me.mrgazdag.programs.reflectioneval.exception.ParseException;
import me.mrgazdag.programs.reflectioneval.text.StyledText;

public class VariableDeclareNode extends Node {
    private final String variableName;
    private final Class<?> variableType;
    private Node assignment;

    public VariableDeclareNode(ExecutionContext context, String variableName, Class<?> variableType) {
        super(context);
        this.variableName = variableName;
        this.variableType = variableType;
        this.assignment = null;
    }

    public void setAssignment(Node assignment) {
        this.assignment = assignment;
    }

    @Override
    public Class<?> getReturnType() {
        return Void.class; //not usable elsewhere
    }

    @Override
    public OutputResult execute() throws ExecuteException {
        if (context.getVariables().containsKey(variableName))
            throw new ParseException(new StyledText("Variable " + variableName + " is already declared"));
        OutputResult varValue;
        if (assignment != null) {
            OutputResult assignmentResult = assignment.execute();
            if (!variableType.isAssignableFrom(assignmentResult.clazz)) {
                throw new ExecuteException(exceptionToMessage(new ClassCastException(assignmentResult.clazz.getName() + " cannot be cast to " + variableType.getName())));
            }
            varValue = new OutputResult(context, assignmentResult.object, variableType, Utils.texts(
                    context.getDisplayFormat().text(FormatType.CLASS, Utils.className(context, variableType)),
                    context.getDisplayFormat().text(FormatType.SPACE, " "),
                    context.getDisplayFormat().text(FormatType.VARNAME, variableName),
                    context.getDisplayFormat().text(FormatType.SPACE, " "),
                    context.getDisplayFormat().text(FormatType.OPERATOR, "="),
                    context.getDisplayFormat().text(FormatType.SPACE, " "),
                    assignmentResult.inputText
            ));
        } else varValue = new OutputResult(context, null, variableType, Utils.texts(
                context.getDisplayFormat().text(FormatType.CLASS, Utils.className(context, variableType)),
                context.getDisplayFormat().text(FormatType.SPACE, " "),
                context.getDisplayFormat().text(FormatType.VARNAME, variableName)
        ));
        context.getVariables().put(variableName, varValue);
        return varValue;
    }
}