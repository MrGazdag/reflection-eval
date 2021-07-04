package me.mrgazdag.programs.reflectioneval;

import me.mrgazdag.programs.reflectioneval.exception.ParseException;
import me.mrgazdag.programs.reflectioneval.nodes.*;
import me.mrgazdag.programs.reflectioneval.text.StyledText;

import javax.lang.model.SourceVersion;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class ReflectionEval {
    private static ParseException EXPECTED_EXPRESSION(StringReader sr) {return new ParseException(new StyledText("Expected start of expression"), sr.getCursor());}
    private static ParseException EXPECTED_VALUE(StringReader sr) {return new ParseException(new StyledText("Expected value"), sr.getCursor());}
    private static ParseException INVALID_SYNTAX_EXPECTED(StringReader sr, Object str) {return new ParseException(new StyledText("Invalid syntax, expected " + str), sr.getCursor());}
    private static ParseException INVALID_SYNTAX_EXPECTED_GOT(StringReader sr, Object str, Object str2) {return new ParseException(new StyledText("Invalid syntax, expected " + str + ", got " + str2), sr.getCursor());}
    //private static final Map<String,ExecutionContext> executionContextMap = new HashMap<>();
    private static ParseException INVALID_SYNTAX(StringReader sr, Object msg) {return new ParseException(new StyledText(""+msg), sr.getCursor());}

    /*
    private static ExecutionContext CONSOLE;

    public static void clearPlayer(String name) {
        executionContextMap.remove(name).reset();
    }
    public static ExecutionContext getExecutionContext(CommandSourceStack source) {
        if (source.source instanceof MinecraftServer) {
            if (CONSOLE == null) {
                CONSOLE = new ExecutionContext(source.source);
            }
            return CONSOLE;
        }
        return getExecutionContext(source.getBukkitSender().getName(), source.source);
    }
    private static ExecutionContext getExecutionContext(String name, CommandSource listener) {
        return executionContextMap.computeIfAbsent(name, (key) -> new ExecutionContext(listener));
    }

     */

    private BiFunction<ExecutionContext, SuggestionCollection, ImmutableSuggestionCollection> tabComplete;
    private int tabOffset;
    private final StringReader sr;
    private final ExecutionContext context;

    private final RootNode rootNode;

    public ReflectionEval(ExecutionContext context, StringReader sr) {
        this.tabComplete = this::tabCompleteVariablesAndImports;
        this.sr = sr;
        this.rootNode = new RootNode(context);
        this.context = context;
        this.tabOffset = sr.getCursor();
    }

    public ImmutableSuggestionCollection tabComplete(ExecutionContext context, SuggestionCollection builder) {
        return tabComplete.apply(context, builder.createOffset(sr.getCursor()));
    }

    public void parse() throws ParseException {
        while (sr.canRead()) {
            sr.skipWhitespace();
            if (!sr.canRead()) break;
            Node n;
            try {
                n = parseNode();
            } catch (CastException e) {
                throw EXPECTED_EXPRESSION(sr);
            }
            if (n != null) rootNode.addNode(n);

            sr.skipWhitespace();
            if (sr.canRead()) {
                if (sr.peek() != ';') throw INVALID_SYNTAX_EXPECTED_GOT(sr,";", sr.peek());
                else sr.skip(); //;
            }
        }
        //throw new SimpleCommandExceptionType(new ChatMessage("Failed to parse code"))(sr);
    }
    private Node parseNode() throws ParseException {
        sr.skipWhitespace();
        if (!sr.canRead()) throw EXPECTED_EXPRESSION(sr);
        int beforeParseCursorPosition = sr.getCursor();
        if (sr.peek() == ';') {
            //empty method, just ignore
            sr.skip();
            return null;
        } else if (sr.peek() == '(') {
            //singleton delegate node: (might be possible to just straight up pass it up the stack)
            //also, casting

            sr.skip(); //(
            sr.skipWhitespace();

            Class<?> castingClass = null;
            Node n = null;
            try {
                n = parseNode();
            } catch (CastException e) {
                castingClass = e.clazz;
            }
            sr.skipWhitespace();
            if (!sr.canRead()) throw INVALID_SYNTAX_EXPECTED(sr, ")");
            else if (sr.peek() != ')') throw INVALID_SYNTAX_EXPECTED_GOT(sr, ")", sr.peek());
            sr.skip(); //)
            sr.skipWhitespace();
            if (castingClass != null) {
                Node castTarget = parseNode();
                return new CastNode(context, castingClass, castTarget);
            } else {
                return parseObjectStuff(new SingletonNode(context,n));
            }
        } else if (peekString(sr, "new")) {
            //constructor
            for (int i = 0; i < "new".length(); i++) sr.skip();

            //separate 'new' and the classname
            int afterNew = sr.getCursor();
            sr.skipWhitespace();
            if (sr.getCursor() == afterNew) throw INVALID_SYNTAX_EXPECTED_GOT(sr, "whitespace", sr.peek());

            tabOffset = sr.getCursor();
            tabComplete = this::tabCompleteVariablesAndImports;
            StringBuilder varname = new StringBuilder();
            //mode: type or varname
            boolean expressionStarted = false;
            while (sr.canRead()) {
                char read = sr.peek();
                if (!expressionStarted) {
                    expressionStarted = true;
                    if (!Character.isJavaIdentifierStart(read)) {
                        throw INVALID_SYNTAX_EXPECTED_GOT(sr, "variable name", read);
                    }
                } else if (!Character.isJavaIdentifierPart(read)) {
                    //throw INVALID_SYNTAX_EXPECTED_GOT(sr, "variable name", read);
                    break;
                }
                varname.append(read);
                sr.skip();
            }
            sr.skipWhitespace();
            if (!context.getImports().containsKey(varname.toString())) {
                throw EXPECTED_EXPRESSION(sr);
            }
            Class<?> importClass = context.getImports().get(varname.toString());
            sr.skipWhitespace();
            if (sr.peek() != '(') {
                throw INVALID_SYNTAX_EXPECTED_GOT(sr,"(", sr.peek());
            }
            sr.skip(); //(
            sr.skipWhitespace();
            ConstructorNode node = new ConstructorNode(context, importClass);

            tabOffset = sr.getCursor();
            tabComplete = (context, builder) -> tabCompleteConstructorParametersStart(context,builder,node.getReturnType());

            if (!sr.canRead()) throw INVALID_SYNTAX_EXPECTED(sr,")");
            if (sr.peek() == ')') {
                //instance method
                sr.skip(); //)
                sr.skipWhitespace();
                return parseObjectStuff(node);
            }
            do {
                sr.skipWhitespace();
                Node paramNode = parseNode();
                node.addParameter(paramNode);
                sr.skipWhitespace();
                if (!sr.canRead()) throw INVALID_SYNTAX_EXPECTED(sr,")");
                if (sr.peek() != ',') {
                    break;
                }
                sr.skip(); //,
            } while (true);
            if (sr.peek() == ')') {
                //instance method
                sr.skip(); //)
                sr.skipWhitespace();
                return parseObjectStuff(node);
            } else throw INVALID_SYNTAX_EXPECTED_GOT(sr,")", sr.read());
        } else if (peekString(sr, "null")) {
            //null constant
            for (int i = 0; i < "null".length(); i++) sr.skip();
            return new NullConstantNode(context);
        } else if (peekString(sr, "false")) {
            //false constant
            for (int i = 0; i < "false".length(); i++) sr.skip();
            return new BooleanConstantNode(context, false);
        } else if (peekString(sr, "true")) {
            //true constant
            for (int i = 0; i < "true".length(); i++) sr.skip();
            return new BooleanConstantNode(context, true);
        } else if (peekString(sr, "++")) {
            //increment and get
            for (int i = 0; i < "++".length(); i++) sr.skip();
            int cursor = sr.getCursor();
            Node varNode = parseNode();
            if (!(varNode instanceof VariableGetNode)) {
                sr.setCursor(cursor);
                throw INVALID_SYNTAX_EXPECTED(sr,"variable");
            }
            return new VariableIncrementGetNode(context, ((VariableGetNode) varNode).getVariableName());
        } else if (peekString(sr, "--")) {
            //increment and get
            for (int i = 0; i < "--".length(); i++) sr.skip();
            int cursor = sr.getCursor();
            Node varNode = parseNode();
            if (!(varNode instanceof VariableGetNode)) {
                sr.setCursor(cursor);
                throw INVALID_SYNTAX_EXPECTED(sr,"variable");
            }
            return new VariableDecrementGetNode(context, ((VariableGetNode) varNode).getVariableName());
        } else if (Character.isDigit(sr.peek()) || sr.peek() == '+' || sr.peek() == '-' || sr.peek() == '.') {
            //int or double constant
            boolean isDouble = false;
            boolean signGot = false;
            Character typeChar = null;
            StringBuilder sb = new StringBuilder();
            while (sr.canRead()) {
                char read = sr.peek();
                if (typeChar != null) {
                    //sr.setCursor(sr.getCursor()-1);
                    //throw INVALID_SYNTAX_EXPECTED_GOT(sr, "end of expression", read);
                    break;
                } else if (read == '-' || read == '+') {
                    if (signGot) break;
                    else {
                        signGot = true;
                        if (read == '-') sb.append(read);
                        sr.skip();
                        continue;
                    }
                } else if (read == '.') {
                    if (isDouble) {
                        sr.setCursor(sr.getCursor()-1);
                        throw INVALID_SYNTAX_EXPECTED(sr,"number or end of expression");
                    }
                    isDouble = true;
                } else if (read == '_') {
                    sr.skip();
                    continue; //ignore
                } else if (sb.length() > 0 && (read == 'd' || read == 'D')) {
                    typeChar = 'd';
                    sr.skip();
                    continue;
                } else if (sb.length() > 0 && (read == 'f' || read == 'F')) {
                    typeChar = 'f';
                    sr.skip();
                    continue;
                } else if (sb.length() > 0 && (read == 'l' || read == 'L')) {
                    typeChar = 'L';
                    sr.skip();
                    continue;
                } else if (!Character.isDigit(read)) {
                    /*
                    sr.setCursor(sr.getCursor()-1);
                    throw INVALID_SYNTAX_EXPECTED_GOT(sr,"number", read);
                    */
                    break;
                }
                sb.append(read);
                sr.skip();
                signGot = true;
            }
            if (typeChar == null) {
                if (isDouble) {
                    try {
                        return parseObjectStuff(new DoubleConstantNode(context, Double.parseDouble(sb.toString())));
                    } catch (NumberFormatException e) {
                        sr.setCursor(beforeParseCursorPosition);
                        throw INVALID_SYNTAX_EXPECTED_GOT(sr, "double", sb.toString());
                    }
                } else {
                    try {
                        return parseObjectStuff(new IntegerConstantNode(context, Integer.parseInt(sb.toString())));
                    } catch (NumberFormatException e) {
                        sr.setCursor(beforeParseCursorPosition);
                        throw INVALID_SYNTAX_EXPECTED_GOT(sr, "integer", sb.toString());
                    }
                }
            } else if (typeChar == 'd') {
                try {
                    return parseObjectStuff(new DoubleConstantNode(context, Double.parseDouble(sb.toString())));
                } catch (NumberFormatException e) {
                    sr.setCursor(beforeParseCursorPosition);
                    throw INVALID_SYNTAX_EXPECTED_GOT(sr, "double", sb.toString());
                }
            } else if (typeChar == 'f') {
                try {
                    return parseObjectStuff(new FloatConstantNode(context, Float.parseFloat(sb.toString())));
                } catch (NumberFormatException e) {
                    sr.setCursor(beforeParseCursorPosition);
                    throw INVALID_SYNTAX_EXPECTED_GOT(sr, "float", sb.toString());
                }
            } else {
                try {
                    return parseObjectStuff(new LongConstantNode(context, Long.parseLong(sb.toString())));
                } catch (NumberFormatException e) {
                    sr.setCursor(beforeParseCursorPosition);
                    throw INVALID_SYNTAX_EXPECTED_GOT(sr, "long", sb.toString());
                }
            }
        } else if (sr.peek() == '"') {
            sr.skip();
            //string constant
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (!sr.canRead()) throw INVALID_SYNTAX_EXPECTED(sr, "string");
                char read = sr.read();
                if (read == '\\') {
                    if (!sr.canRead()) {
                        throw INVALID_SYNTAX_EXPECTED(sr, "escape sequence");
                    } else if (sr.peek() == '"') {
                        sb.append("\"");
                        sr.skip();
                    } else sb.append(sr.read());
                } else if (read == '"') {
                    break;
                } else sb.append(read);
            }
            return parseObjectStuff(new StringConstantNode(context, sb.toString()));
        } else {
            //expression
            tabOffset = sr.getCursor();
            tabComplete = this::tabCompleteVariablesAndImports;
            StringBuilder varname = new StringBuilder();
            //mode: type or varname
            boolean expressionStarted = false;
            while (sr.canRead()) {
                char read = sr.peek();
                if (!expressionStarted) {
                    expressionStarted = true;
                    if (!Character.isJavaIdentifierStart(read)) {
                        throw INVALID_SYNTAX_EXPECTED_GOT(sr, "variable name", read);
                    }
                } else if (!Character.isJavaIdentifierPart(read)) {
                    //throw INVALID_SYNTAX_EXPECTED_GOT(sr, "variable name", read);
                    break;
                }
                varname.append(read);
                sr.skip();
            }
            sr.skipWhitespace();
            if (context.getVariables().containsKey(varname.toString())) {
                OutputResult variableContents = context.getVariables().get(varname.toString());

                //possible node here is a variable increment node, variable decrement node, variable get node, variable set node or instance method node
                sr.skipWhitespace();
                if (!sr.canRead()) {
                    //#####
                    //END NODE
                    return parseObjectStuff(new VariableGetNode(context, varname.toString()));
                } else if (sr.peek() == '=') {
                    sr.skip(); //=
                    sr.skipWhitespace();
                    if (!sr.canRead()) {
                        throw EXPECTED_EXPRESSION(sr);
                    }
                    if (!variableContents.clazz.isPrimitive() && peekString(sr, "null")) {
                        sr.skip(); //n
                        sr.skip(); //u
                        sr.skip(); //l
                        sr.skip(); //l
                        //#####
                        //END NODE
                        VariableSetNode node = new VariableSetNode(context, varname.toString());
                        rootNode.addNode(node);
                        return parseObjectStuff(node);
                    }
                    Node valueNode = parseNode();
                    VariableSetNode node = new VariableSetNode(context, varname.toString());
                    node.setValue(valueNode);
                    rootNode.addNode(node);
                    return parseObjectStuff(node);
                } else if (sr.peek() == '.') {
                    if (variableContents.clazz.isPrimitive()) throw INVALID_SYNTAX(sr,"Primitive classes don't have methods/fields");
                    sr.skipWhitespace();
                    return parseObjectStuff(new VariableGetNode(context, varname.toString()));
                } else if (peekString(sr,"++")) {
                    sr.skip(); //+
                    sr.skip(); //+
                    //#####
                    //END NODE
                    return parseObjectStuff(new VariableGetIncrementNode(context, varname.toString()));
                } else if (peekString(sr,"--")) {
                    sr.skip(); //-
                    sr.skip(); //-
                    //#####
                    //END NODE
                    return parseObjectStuff(new VariableGetDecrementNode(context, varname.toString()));
                } else {
                    //#####
                    //END NODE
                    return parseObjectStuff(new VariableGetNode(context, varname.toString()));
                }
            } else if (context.getImports().containsKey(varname.toString())) {
                Class<?> importClass = context.getImports().get(varname.toString());
                //possible node here is a variable declare node, or static method
                sr.skipWhitespace();
                if (!sr.canRead()) {
                    throw INVALID_SYNTAX_EXPECTED(sr, ".");
                } else if (sr.peek() == ')') {
                    throw new CastException(importClass);
                } else if (sr.peek() == '.') {
                    sr.skip(); //.
                    StringBuilder methodName = new StringBuilder();
                    tabOffset = sr.getCursor();
                    tabComplete = (context, builder) -> tabCompleteStaticMethodsFieldsAndEnums(context,builder,importClass);
                    boolean started = false;
                    if (!sr.canRead()) throw INVALID_SYNTAX_EXPECTED(sr,"method or field name");
                    while (sr.canRead()) {
                        char read = sr.peek();
                        if (!started) {
                            started = true;
                            if (!Character.isJavaIdentifierStart(read)) {
                                throw INVALID_SYNTAX_EXPECTED_GOT(sr, "method or field name", read);
                            }
                        } else if (!Character.isJavaIdentifierPart(read)) {
                            //throw INVALID_SYNTAX_EXPECTED_GOT(sr, "variable name", read);
                            break;
                        }
                        methodName.append(read);
                        sr.skip();
                    }
                    sr.skipWhitespace();
                    if (!sr.canRead()) {
                        //must be field or enum or error
                        Enum<?> enumConstant = tryGetEnumConstant(importClass, methodName.toString());
                        if (enumConstant != null) return parseObjectStuff(new EnumConstantGetNode(context, enumConstant, importClass));
                        return parseObjectStuff(new StaticFieldGetNode(context, methodName.toString(), importClass));
                    } else if (sr.peek() == '=') {
                        sr.skip(); //=
                        sr.skipWhitespace();
                        if (!sr.canRead()) throw EXPECTED_EXPRESSION(sr);
                        Node value = parseNode();
                        StaticFieldSetNode field = new StaticFieldSetNode(context,methodName.toString(), importClass);
                        field.setValue(value);
                        return field;
                    } else if (sr.peek() == '(') {
                        //static methods
                        sr.skip(); //(
                        sr.skipWhitespace();
                        StaticMethodNode node = new StaticMethodNode(context, methodName.toString(), importClass);

                        tabOffset = sr.getCursor();
                        tabComplete = (context, builder) -> tabCompleteMethodParametersStart(context,builder,node.getReturnType(), methodName.toString());

                        if (!sr.canRead()) throw INVALID_SYNTAX_EXPECTED(sr,")");
                        if (sr.peek() == ')') {
                            //instance method
                            sr.skip(); //)
                            sr.skipWhitespace();
                            return parseObjectStuff(node);
                        }
                        do {
                            sr.skipWhitespace();
                            Node paramNode = parseNode();
                            node.addParameter(paramNode);
                            sr.skipWhitespace();
                            if (!sr.canRead()) throw INVALID_SYNTAX_EXPECTED(sr,")");
                            if (sr.peek() != ',') {
                                break;
                            }
                            sr.skip(); //,
                        } while (true);
                        if (sr.peek() == ')') {
                            //instance method
                            sr.skip(); //)
                            sr.skipWhitespace();
                            return parseObjectStuff(node);
                        } else throw INVALID_SYNTAX_EXPECTED_GOT(sr,")", sr.read());
                    } else {
                        //must be field or enum or error
                        Enum<?> enumConstant = tryGetEnumConstant(importClass, methodName.toString());
                        if (enumConstant != null) return parseObjectStuff(new EnumConstantGetNode(context, enumConstant, importClass));
                        return parseObjectStuff(new StaticFieldGetNode(context, methodName.toString(), importClass));
                    }
                } else {
                    //variable declaration
                    StringBuilder variableName = new StringBuilder();
                    tabOffset = sr.getCursor();
                    tabComplete = (context, builder) -> tabCompleteCamelCase(context,builder,Utils.className(context, importClass));
                    boolean started = false;
                    if (!sr.canRead()) throw INVALID_SYNTAX_EXPECTED(sr,"variable name");
                    while (sr.canRead()) {
                        char read = sr.peek();
                        if (!started) {
                            started = true;
                            if (!Character.isJavaIdentifierStart(read)) {
                                throw INVALID_SYNTAX_EXPECTED_GOT(sr, "variable name", read);
                            }
                        } else if (!Character.isJavaIdentifierPart(read)) {
                            //throw INVALID_SYNTAX_EXPECTED_GOT(sr, "variable name", read);
                            break;
                        }
                        variableName.append(read);
                        sr.skip();
                    }
                    sr.skipWhitespace();
                    if (!SourceVersion.isName(variableName.toString())) {
                        throw INVALID_SYNTAX(sr, variableName + " is not a valid Java identifier");
                    }
                    if (!sr.canRead()) {
                        //declaration without assignment
                        return new VariableDeclareNode(context, variableName.toString(), importClass);
                    } else if (sr.peek() == '=') {
                        //declaration with assignment
                        sr.skipWhitespace();
                        if (!sr.canRead()) throw EXPECTED_EXPRESSION(sr);
                        Node node = parseNode();
                        VariableDeclareNode varDec = new VariableDeclareNode(context, variableName.toString(), importClass);
                        varDec.setAssignment(node);
                        return varDec;
                    }

                }

            }
        }
        throw EXPECTED_EXPRESSION(sr);
        //throw new SimpleCommandExceptionType(new ChatMessage("Failed to parse code"))(sr);
    }
    public Enum<?> tryGetEnumConstant(Class<?> clazz, String enumConstant) {
        if (!clazz.isEnum()) return null;
        for (Object constant : clazz.getEnumConstants()) {
            Enum<?> e = (Enum<?>) constant;
            if (e.name().equals(enumConstant)) return e;
        }
        return null;
    }
    private static class CastException extends RuntimeException {
        private final Class<?> clazz;

        public CastException(Class<?> clazz) {
            this.clazz = clazz;
        }
    }
    private Node parseObjectStuff(Node on) throws ParseException {
        //return new VariableGetNode(context,varname.toString());
        if (!sr.canRead()) return on;

        do {
            Node inner = on;
            sr.skipWhitespace();
            if (!sr.canRead()) return inner;
            if (sr.peek() == '.' && !on.getReturnType().isPrimitive()) {
                sr.skip(); //.
                sr.skipWhitespace();

                StringBuilder methodName = new StringBuilder();
                tabOffset = sr.getCursor();
                tabComplete = (context, builder) -> tabCompleteInstanceMethodsAndFields(context,builder,inner.getReturnType());
                boolean started = false;
                if (!sr.canRead()) throw INVALID_SYNTAX_EXPECTED(sr,"method or field name");
                while (sr.canRead()) {
                    char read = sr.peek();
                    if (!started) {
                        started = true;
                        if (!Character.isJavaIdentifierStart(read)) {
                            throw INVALID_SYNTAX_EXPECTED_GOT(sr, "method or field name", read);
                        }
                    } else if (!Character.isJavaIdentifierPart(read)) {
                        //throw INVALID_SYNTAX_EXPECTED_GOT(sr, "variable name", read);
                        break;
                    }
                    methodName.append(read);
                    sr.skip();
                }
                sr.skipWhitespace();
                if (!sr.canRead()) {
                    //instance field get
                    on = new InstanceFieldGetNode(context, inner, methodName.toString(), inner.getReturnType());
                } else if (sr.peek() == '(') {
                    sr.skip(); //(
                    sr.skipWhitespace();
                    InstanceMethodNode node = new InstanceMethodNode(context, inner, methodName.toString(), inner.getReturnType());

                    tabOffset = sr.getCursor();
                    tabComplete = (context, builder) -> tabCompleteMethodParametersStart(context,builder,node.getReturnType(), methodName.toString());

                    if (!sr.canRead()) throw INVALID_SYNTAX_EXPECTED(sr,")");
                    if (sr.peek() == ')') {
                        //instance method
                        sr.skip(); //)
                        sr.skipWhitespace();
                        on = node;
                        continue;
                    }
                    do {
                        sr.skipWhitespace();
                        Node paramNode = parseNode();
                        node.addParameter(paramNode);
                        sr.skipWhitespace();
                        if (!sr.canRead()) throw INVALID_SYNTAX_EXPECTED(sr,")");
                        if (sr.peek() != ',') {
                            break;
                        }
                        sr.skip(); //,
                    } while (true);
                    if (sr.peek() == ')') {
                        //instance method
                        sr.skip(); //)
                        sr.skipWhitespace();
                        on = node;
                    } else throw INVALID_SYNTAX_EXPECTED_GOT(sr,")", sr.read());
                } else if (sr.peek() == '=') {
                    //instance field set
                    sr.skip(); //=
                    sr.skipWhitespace();
                    if (!sr.canRead()) throw EXPECTED_EXPRESSION(sr);
                    Node value = parseNode();
                    InstanceFieldSetNode field = new InstanceFieldSetNode(context,on,methodName.toString(),on.getReturnType());
                    field.setValue(value);
                    on = field;
                } else {
                    //instance field get
                    on = new InstanceFieldGetNode(context, inner, methodName.toString(), inner.getReturnType());
                }
            } else if (peekString(sr, "==")) {
                sr.skip(); //=
                sr.skip(); //=
                sr.skipWhitespace();
                if (!sr.canRead()) throw EXPECTED_EXPRESSION(sr);
                Node otherNode = parseObjectStuff(parseNode());
                return new OperatorEqualsNode(context,on,otherNode);
            } else return on;
        } while (sr.canRead());
        return on;
    }

    private ImmutableSuggestionCollection tabCompleteDot(ExecutionContext context, SuggestionCollection builder) {
        builder = builder.createOffset(tabOffset);
        builder.suggest(".");
        return builder.build();
    }

    private ImmutableSuggestionCollection tabCompleteInstanceMethodsAndFields(ExecutionContext context, SuggestionCollection builder, Class<?> clazz) {
        builder = builder.createOffset(tabOffset);
        String input = builder.getRemaining();
        for (Method m : collectMethods(clazz)) {
            int mod = m.getModifiers();
            if (m.getName().startsWith(input)) {
                if (context.isPubliconly()) {
                    if (Modifier.isPublic(mod)) builder.suggest(m.getName() + "(" + (m.getParameterCount() == 0 ? ")" : ""));
                } else builder.suggest(m.getName() + "(" + (m.getParameterCount() == 0 ? ")" : ""));
            }
        }
        for (Field f : collectFields(clazz)) {
            int mod = f.getModifiers();
            if (f.getName().startsWith(input)) {
                if (context.isPubliconly()) {
                    if (Modifier.isPublic(mod)) builder.suggest(f.getName());
                } else builder.suggest(f.getName());
            }
        }
        return builder.build();
    }
    private ImmutableSuggestionCollection tabCompleteCamelCase(ExecutionContext context, SuggestionCollection builder, String toCamelCase) {
        builder = builder.createOffset(tabOffset);
        if (toCamelCase.length() <= 1) builder.suggest(toCamelCase.toLowerCase(Locale.ROOT));
        else builder.suggest(Character.toLowerCase(toCamelCase.charAt(0)) + toCamelCase.substring(1));
        return builder.build();
    }
    private ImmutableSuggestionCollection tabCompleteMethodParametersStart(ExecutionContext context, SuggestionCollection builder, Class<?> clazz, String methodName) {
        builder = builder.createOffset(tabOffset);
        String input = builder.getRemaining();
        if (input.length() == 0) for (Method m : collectMethods(clazz)) {
            if (m.getParameterCount() == 0 && m.getName().equals(methodName)) {
                builder.suggest(")");
                break;
            }
        }
        for (String s : context.getVariables().keySet()) {
            if (s.startsWith(input)) builder.suggest(s);
        }
        for (String s : context.getImports().keySet()) {
            if (s.startsWith(input)) builder.suggest(s);
        }
        return builder.build();
    }
    private ImmutableSuggestionCollection tabCompleteConstructorParametersStart(ExecutionContext context, SuggestionCollection builder, Class<?> clazz) {
        builder = builder.createOffset(tabOffset);
        String input = builder.getRemaining();
        if (input.length() == 0) {
            try {
                clazz.getConstructor();
                builder.suggest(")");
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        for (String s : context.getVariables().keySet()) {
            if (s.startsWith(input)) builder.suggest(s);
        }
        for (String s : context.getImports().keySet()) {
            if (s.startsWith(input)) builder.suggest(s);
        }
        return builder.build();
    }
    private static Set<Method> collectMethods(Class<?> c) {
        return collectMethods(c, new HashSet<>());
    }
    private static Set<Method> collectMethods(Class<?> c, Set<Method> methods) {
        methods.addAll(Arrays.asList(c.getDeclaredMethods()));
        for (Class<?> anInterface : c.getInterfaces()) {
            collectMethods(anInterface, methods);
        }
        if (c.getSuperclass() == null || c == Object.class) return methods;
        return collectMethods(c.getSuperclass(), methods);
    }

    private static Set<Field> collectFields(Class<?> c) {
        return collectFields(c, new HashSet<>());
    }
    private static Set<Field> collectFields(Class<?> c, Set<Field> fields) {
        fields.addAll(Arrays.asList(c.getDeclaredFields()));
        for (Class<?> anInterface : c.getInterfaces()) {
            collectFields(anInterface, fields);
        }
        if (c.getSuperclass() == null || c == Object.class) return fields;
        return collectFields(c.getSuperclass(), fields);
    }

    private static Set<Enum<?>> collectEnums(Class<?> c) {
        Set<Enum<?>> enums = new HashSet<>();
        if (!c.isEnum()) return enums;
        for (Object enumConstant : c.getEnumConstants()) {
            enums.add((Enum<?>) enumConstant);
        }
        return enums;
    }

    private ImmutableSuggestionCollection tabCompleteStaticMethodsFieldsAndEnums(ExecutionContext context, SuggestionCollection builder, Class<?> clazz) {
        builder = builder.createOffset(tabOffset);
        String input = builder.getRemaining();
        for (Method m : collectMethods(clazz)) {
            int mod = m.getModifiers();
            if (m.getName().startsWith(input)) {
                if (context.isPubliconly()) {
                    if (Modifier.isStatic(mod) && Modifier.isPublic(mod)) builder.suggest(m.getName() + "(" + (m.getParameterCount() == 0 ? ")" : ""));
                } else builder.suggest(m.getName() + "(" + (m.getParameterCount() == 0 ? ")" : ""));
            }
        }
        for (Field f : collectFields(clazz)) {
            int mod = f.getModifiers();
            if (f.getName().startsWith(input)) {
                if (context.isPubliconly()) {
                    if (Modifier.isStatic(mod) && Modifier.isPublic(mod)) builder.suggest(f.getName());
                } else builder.suggest(f.getName());
            }
        }
        for (Enum<?> e : collectEnums(clazz)) {
            if (e.name().startsWith(input)) builder.suggest(e.name());
        }
        return builder.build();
    }

    private ImmutableSuggestionCollection tabCompleteVariablesAndImports(ExecutionContext context, SuggestionCollection builder) {
        builder = builder.createOffset(tabOffset);
        String input = builder.getRemaining();
        if (context.getVariables().containsKey(input) || context.getImports().containsKey(input)) builder.suggest(input + ".");
        for (String s : context.getVariables().keySet()) {
            if (s.startsWith(input)) builder.suggest(s);
        }
        for (String s : context.getImports().keySet()) {
            if (s.startsWith(input)) builder.suggest(s);
        }
        return builder.build();
    }

    public RootNode getRootNode() {
        return rootNode;
    }

    private static boolean peekString(StringReader sr, String toPeek) {
        if (!sr.canRead(toPeek.length())) return false;
        char[] chars = toPeek.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (sr.peek(i) != chars[i]) return false;
        }
        return true;
    }

    public ExecutionContext getContext() {
        return context;
    }

}
