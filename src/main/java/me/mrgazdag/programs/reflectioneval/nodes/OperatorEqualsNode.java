package me.mrgazdag.programs.reflectioneval.nodes;

import me.mrgazdag.programs.reflectioneval.ExecutionContext;
import me.mrgazdag.programs.reflectioneval.FormatType;
import me.mrgazdag.programs.reflectioneval.OutputResult;
import me.mrgazdag.programs.reflectioneval.Utils;
import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;
import me.mrgazdag.programs.reflectioneval.text.StyledText;

public class OperatorEqualsNode extends Node {
    private final Node param1;
    private final Node param2;

    public OperatorEqualsNode(ExecutionContext context, Node param1, Node param2) {
        super(context);
        this.param1 = param1;
        this.param2 = param2;
    }

    @Override
    public Class<?> getReturnType() {
        return boolean.class;
    }

    @Override
    public OutputResult execute() throws ExecuteException {
        OutputResult value1 = param1.execute();
        OutputResult value2 = param2.execute();
        if (!value1.clazz.isAssignableFrom(value2.clazz)) {
            throw new ExecuteException(new StyledText("Operator '==' cannot be applied to '" + Utils.className(context, value1.clazz) + "' and '" + Utils.className(context, value2.clazz) + "'"));
        }

        return new OutputResult(context, value1.object == value2.object, boolean.class, Utils.texts(
                value1.inputText,
                context.getDisplayFormat().text(FormatType.SPACE, " "),
                context.getDisplayFormat().text(FormatType.OPERATOR, "=="),
                context.getDisplayFormat().text(FormatType.SPACE, " "),
                value2.inputText
        ));
    }
}