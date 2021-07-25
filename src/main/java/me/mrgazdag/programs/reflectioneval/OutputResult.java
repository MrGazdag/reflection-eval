package me.mrgazdag.programs.reflectioneval;

import me.mrgazdag.programs.reflectioneval.text.StyledText;

public class OutputResult {
    public final ExecutionContext context;
    public final Object object;
    public final Class<?> clazz;
    public final StyledText inputText;

    public OutputResult(ExecutionContext context, Object object, Class<?> clazz, StyledText inputText) {
        this.context = context;
        this.object = object;
        this.clazz = clazz;
        this.inputText = inputText;
    }

    public void printInputTextTo(CommandTarget output) {
        output.sendMessage(inputText);
    }

    public void printResultTo(CommandTarget output) {
        output.sendMessage(context.prettyPrint(object, clazz));
    }
}