package me.mrgazdag.programs.reflectioneval;

public enum FormatType {
    STRING("abcd1234"),
    NUMBER("1.2"),
    VARNAME("variable"),
    KEYWORD("null"),
    VOID("void"),
    OBJECT("java.lang.Object@12345678"),
    CLASS("CoreClient"),
    METHOD_DOT("."),
    PARENTHESES("()"),
    BRACKETS("[]"),
    STATIC_METHOD("method"),
    INSTANCE_METHOD("method"),
    ENUM_CONSTANT("ENUM"),
    OPERATOR("++"),
    COMMA(","),
    SEMICOLON(";"),
    SPACE(" "),
    WARNING("Variable might be null");
    private final String example;

    FormatType(String example) {
        this.example = example;
    }

    public String getExample() {
        return example;
    }
}