package me.mrgazdag.programs.reflectioneval.nodes;

import me.mrgazdag.programs.reflectioneval.ExecutionContext;
import me.mrgazdag.programs.reflectioneval.OutputResult;
import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;
import me.mrgazdag.programs.reflectioneval.exception.ParseException;
import me.mrgazdag.programs.reflectioneval.text.Style;
import me.mrgazdag.programs.reflectioneval.text.StyledText;

import java.awt.*;
import java.util.function.Supplier;

public abstract class Node implements Supplier<OutputResult> {
    protected ExecutionContext context;

    public Node(ExecutionContext context) {
        this.context = context;
    }

    public abstract OutputResult execute() throws ExecuteException;

    public Node resolveNode() {
        return this;
    }

    @Override
    public OutputResult get() {
        return execute();
    }

    public Class<?> getReturnType() {
        return Object.class;
    }

    public static StyledText exceptionToMessage(Throwable e) {
        if (e instanceof ParseException pe) return pe.getStyledMessage();
        else if (e instanceof ExecuteException ee) return ee.getStyledMessage();
        /*
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        //print the exception into the byte array output stream
        e.printStackTrace(ps);
        ps.close();
        */
        return new StyledText(new Style(), e.getClass().getName() + ": " + e.getLocalizedMessage());
    }

}