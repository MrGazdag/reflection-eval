package me.mrgazdag.programs.reflectioneval.nodes;

import me.mrgazdag.programs.reflectioneval.ExecutionContext;
import me.mrgazdag.programs.reflectioneval.OutputResult;
import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;

import java.util.ArrayList;
import java.util.List;

public class RootNode extends Node {
    private final List<Node> nodes;

    public RootNode(ExecutionContext context) {
        super(context);
        this.nodes = new ArrayList<>();
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void addNode(Node node) {
        if (node != this) nodes.add(node);
    }

    @Override
    public OutputResult execute() throws ExecuteException {
        OutputResult last = null;
        for (Node node : nodes) {
            last = node.execute();
        }
        return last;
    }
}