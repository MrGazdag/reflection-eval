package me.mrgazdag.programs.reflectioneval.nodes;

import me.mrgazdag.programs.reflectioneval.ExecutionContext;
import me.mrgazdag.programs.reflectioneval.OutputResult;
import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;

public class SingletonNode extends Node {
    private final Node node;

    public SingletonNode(ExecutionContext context, Node node) {
        super(context);
        this.node = node;
    }

    @Override
    public Class<?> getReturnType() {
        return node.getReturnType();
    }

    @Override
    public OutputResult execute() throws ExecuteException {
        return resolveNode().execute();
    }

    @Override
    public Node resolveNode() {
        Node result = node;
        //noinspection IdempotentLoopBody
        while (result instanceof SingletonNode) {
            result = ((SingletonNode) node).node;
        }
        return result;
    }
}