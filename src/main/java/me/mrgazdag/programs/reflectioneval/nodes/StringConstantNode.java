package me.mrgazdag.programs.reflectioneval.nodes;

import me.mrgazdag.programs.reflectioneval.ExecutionContext;
import me.mrgazdag.programs.reflectioneval.FormatType;
import me.mrgazdag.programs.reflectioneval.OutputResult;
import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;

import java.util.Locale;

public class StringConstantNode extends Node {
    private final String value;

    public StringConstantNode(ExecutionContext context, String value) {
        super(context);
        this.value = value;
    }

    @Override
    public Class<?> getReturnType() {
        return String.class;
    }

    @Override
    public OutputResult execute() throws ExecuteException {
        return new OutputResult(context, value, String.class, context.getDisplayFormat().text(FormatType.STRING, "\"" + escapeJavaString(value) + "\""));
    }

    private static String escapeJavaString(CharSequence str) {
        StringBuilder sb = new StringBuilder();
        if (str != null) {
            int sz = str.length();

            for(int i = 0; i < sz; ++i) {
                char ch = str.charAt(i);
                if (ch > 4095) {
                    sb.append("\\u").append(hex(ch));
                } else if (ch > 255) {
                    sb.append("\\u0").append(hex(ch));
                } else if (ch > 127) {
                    sb.append("\\u00").append(hex(ch));
                } else if (ch < ' ') {
                    switch(ch) {
                        case '\b':
                            sb.append((char)92);
                            sb.append((char)98);
                            break;
                        case '\t':
                            sb.append((char)92);
                            sb.append((char)116);
                            break;
                        case '\n':
                            sb.append((char)92);
                            sb.append((char)110);
                            break;
                        case '\u000b':
                        default:
                            if (ch > 15) {
                                sb.append("\\u00").append(hex(ch));
                            } else {
                                sb.append("\\u000").append(hex(ch));
                            }
                            break;
                        case '\f':
                            sb.append((char)92);
                            sb.append((char)102);
                            break;
                        case '\r':
                            sb.append((char)92);
                            sb.append((char)114);
                    }
                } else {
                    switch (ch) {
                        case '"' -> {
                            sb.append((char) 92);
                            sb.append((char) 34);
                        }
                        case '\'' -> sb.append((char) 39);
                        case '/' -> sb.append((char) 47);
                        case '\\' -> {
                            sb.append((char) 92);
                            sb.append((char) 92);
                        }
                        default -> sb.append(ch);
                    }
                }
            }
        }
        return sb.toString();
    }
    private static String hex(char ch) {
        return Integer.toHexString(ch).toUpperCase(Locale.ENGLISH);
    }

}