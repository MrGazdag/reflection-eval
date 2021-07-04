package me.mrgazdag.programs.reflectioneval;

import me.mrgazdag.programs.reflectioneval.exception.ExecuteException;
import me.mrgazdag.programs.reflectioneval.exception.ParseException;
import me.mrgazdag.programs.reflectioneval.text.Style;
import me.mrgazdag.programs.reflectioneval.text.StyledText;
import org.json.JSONObject;

import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ExecutionContext {
    private final Map<String,OutputResult> variables;
    private final Map<String,Class<?>> imports;
    private final DisplayFormat displayFormat;
    private final CommandTarget list;
    private final Map<Class<?>, Function<?,StyledText>> styledTextProducers;
    private BiFunction<Object,Class<?>,StyledText> defaultStyledTextProducer;
    private boolean publiconly;

    public ExecutionContext(CommandTarget commandSource) {
        this.variables = new HashMap<>();
        this.list = commandSource;
        this.imports = new HashMap<>();
        this.displayFormat = new DisplayFormat();
        this.styledTextProducers = new HashMap<>();
        reset();
    }

    public <T> void addStyledTextProducer(Class<T> clazz, Function<T, StyledText> func) {
        styledTextProducers.put(clazz, func);
    }

    public void removeStyledTextProducer(Class<?> clazz) {
        styledTextProducers.remove(clazz);
    }

    @SuppressWarnings("unchecked")
    public <T> Function<T, StyledText> getStyledTextProducer(Class<T> clazz) {
        Function<?,StyledText> func = styledTextProducers.get(clazz);
        return (Function<T, StyledText>) func;
    }

    @SuppressWarnings("unchecked")
    public StyledText prettyPrint(Object obj, Class<?> type) {
        for (Map.Entry<Class<?>, Function<?, StyledText>> entry : styledTextProducers.entrySet()) {
            if (entry.getKey() == type || entry.getKey().isAssignableFrom(type)) {
                return ((Function<Object,StyledText>)entry.getValue()).apply(obj);
            }
        }
        return defaultStyledTextProducer.apply(obj, type);
    }

    public void resetStyledTextProducers() {
        this.defaultStyledTextProducer = (obj, type) -> {
            if (obj == null) {
                return new StyledText(new Style().color("#AAAAAA").italic(true), "null");
            } else if (obj.getClass().isArray()) {
                StyledText base = text(FormatType.BRACKETS, "[");
                Object[] array = (Object[]) obj;
                boolean first = true;
                for (Object o : array) {
                    if (first) {
                        first = false;
                    } else {
                        base.append(text(FormatType.COMMA, ", "));
                    }
                    base.append(prettyPrint(o, type.getComponentType()));
                }
                base.append(text(FormatType.BRACKETS, "]"));
                return base;
            } else {
                return new StyledText(new Style().color("#AAAAAA"), obj.toString());
            }
        };
        addStyledTextProducer(Void.class, (o) -> new StyledText(new Style().color("#555555").italic(true), "void"));
        addStyledTextProducer(void.class, (o) -> new StyledText(new Style().color("#555555").italic(true), "void"));
        addStyledTextProducer(CharSequence.class, (o) -> text(FormatType.STRING, "\"" + escapeJavaString(o) + "\""));
        addStyledTextProducer(Number.class, (o) -> text(FormatType.NUMBER, o.toString()));
        addStyledTextProducer(Color.class, (o) -> {
            String hs = getHexString(o);
            return text(FormatType.OBJECT, "Color[")
                    .append(text(FormatType.INSTANCE_METHOD, "red"))
                    .append(text(FormatType.OPERATOR, "="))
                    .append(text(FormatType.NUMBER, o.getRed() + ""))
                    .append(text(FormatType.SPACE, " "))
                    .append(new StyledText(new Style().color(new Color(o.getRed(), 0, 0)), "█"))
                    .append(text(FormatType.COMMA, ","))
                    .append(text(FormatType.SPACE, " "))

                    .append(text(FormatType.INSTANCE_METHOD, "green"))
                    .append(text(FormatType.OPERATOR, "="))
                    .append(text(FormatType.NUMBER, o.getGreen() + ""))
                    .append(text(FormatType.SPACE, " "))
                    .append(new StyledText(new Style().color(new Color(0, o.getGreen(), 0)), "█"))
                    .append(text(FormatType.COMMA, ","))
                    .append(text(FormatType.SPACE, " "))

                    .append(text(FormatType.INSTANCE_METHOD, "blue"))
                    .append(text(FormatType.OPERATOR, "="))
                    .append(text(FormatType.NUMBER, o.getBlue() + ""))
                    .append(text(FormatType.SPACE, " "))
                    .append(new StyledText(new Style().color(new Color(0, 0, o.getBlue())), "█"))
                    .append(text(FormatType.OBJECT, "]"))
                    .append(text(FormatType.SPACE, " "))
                    .append(text(FormatType.PARENTHESES, "("))
                    .append(text(FormatType.STRING, hs))
                    .append(text(FormatType.PARENTHESES, ")"))
                    .append(text(FormatType.SPACE, " "))
                    .append(new StyledText(new Style().color(o), "█"))
                    ;
        });
    }

    public void setDefaultStyledTextProducer(BiFunction<Object, Class<?>, StyledText> defaultStyledTextProducer) {
        this.defaultStyledTextProducer = defaultStyledTextProducer;
    }

    public boolean unimportClass(String name) {
        boolean result = imports.containsKey(name);
        imports.remove(name);
        return result;
    }
    public boolean importClass(Class<?> clazz) {
        boolean result = !imports.containsKey(clazz.getSimpleName());
        if (result) imports.put(clazz.getSimpleName(), clazz);
        return result;
    }

    public StyledText text(FormatType type, String text) {
        return displayFormat.text(type, text);
    }

    public void clearVariables() {
        variables.clear();
    }

    public void resetImports() {
        imports.clear();
        //java.lang stuff
        imports.put("String", String.class);
        imports.put("int", int.class);
        imports.put("double", double.class);
        imports.put("float", float.class);
        imports.put("boolean", boolean.class);
        imports.put("short", short.class);
        imports.put("long", long.class);
        imports.put("byte", byte.class);
        //basic coreclient stuff
        /*
        imports.put("CoreClient", CoreClient.class);
        imports.put("ResourcePack", ResourcePack.class);
        imports.put("EntityPlayer", EntityPlayer.class);
        imports.put("Location", Location.class);
        imports.put("Block", Block.class);
        imports.put("ItemStack", ItemStack.class);
        imports.put("ItemStackBuilder", ItemStackBuilder.class);
        */
    }

    public void reset() {
        clearVariables();
        resetImports();
        resetStyledTextProducers();
        publiconly = true;
    }

    public void setPubliconly(boolean publiconly) {
        this.publiconly = publiconly;
    }

    public boolean isPubliconly() {
        return publiconly;
    }

    public CommandTarget getList() {
        return list;
    }

    public Map<String, OutputResult> getVariables() {
        return variables;
    }

    public Map<String, Class<?>> getImports() {
        return imports;
    }

    public DisplayFormat getDisplayFormat() {
        return displayFormat;
    }

    public JSONObject save(String name) {
        JSONObject data = new JSONObject();
        JSONObject imports = new JSONObject();
        for (Map.Entry<String, Class<?>> entry : this.imports.entrySet()) {
            imports.put(entry.getKey(), entry.getValue().getName());
        }
        data.put("imports", imports);
        JSONObject variables = new JSONObject();
        for (Map.Entry<String, OutputResult> entry : this.variables.entrySet()) {
            JSONObject varData = new JSONObject();
            OutputResult value = entry.getValue();
            varData.put("type", value.clazz.getName());
            varData.put("value", value.inputText.getRawText());
            varData.put("text", value.inputText);
            /*
            if (value instanceof NodeResult) {

            }
            else {
                varData.put("value", JSONObject.NULL);
            }
            */
            variables.put(entry.getKey(), varData);
        }
        data.put("variables", variables);

        return data;
    }

    public void load(JSONObject data) throws ParseException, ExecuteException, ClassNotFoundException {
        variables.clear();
        imports.clear();
        JSONObject imports = data.getJSONObject("imports");
        for (String key : imports.keySet()) {
            String type = imports.getString(key);
            Class<?> typeClass;
            switch (type) {
                case "int":
                    typeClass = int.class;
                    break;
                case "double":
                    typeClass = double.class;
                    break;
                case "float":
                    typeClass = float.class;
                    break;
                case "byte":
                    typeClass = byte.class;
                    break;
                case "char":
                    typeClass = char.class;
                    break;
                case "boolean":
                    typeClass = boolean.class;
                    break;
                case "long":
                    typeClass = long.class;
                    break;
                case "short":
                    typeClass = short.class;
                    break;
                default:
                    typeClass = Class.forName(type);
                    break;
            }
            this.imports.put(key, typeClass);
        }

        JSONObject variables = data.getJSONObject("variables");
        for (String key : variables.keySet()) {
            OutputResult varContents;
            JSONObject varData = variables.getJSONObject(key);
            String type = varData.getString("type");
            Class<?> typeClass;
            switch (type) {
                case "int":
                    typeClass = int.class;
                    break;
                case "double":
                    typeClass = double.class;
                    break;
                case "float":
                    typeClass = float.class;
                    break;
                case "byte":
                    typeClass = byte.class;
                    break;
                case "char":
                    typeClass = char.class;
                    break;
                case "boolean":
                    typeClass = boolean.class;
                    break;
                case "long":
                    typeClass = long.class;
                    break;
                case "short":
                    typeClass = short.class;
                    break;
                default:
                    typeClass = Class.forName(type);
                    break;
            }
            //this.variables.put(key, new OutputResult(this,null,typeClass));
            String value = varData.isNull("value") ? null : varData.getString("value");
            Object valueObj;
            if (value != null) {
                ReflectionEval runner = new ReflectionEval(this, new StringReader(value));
                runner.parse();
                valueObj = runner.getRootNode().execute().object;
            } else {
                valueObj = null;
            }
            varContents = new OutputResult(this,valueObj,typeClass, new StyledText(varData.getJSONObject("text")));
            this.variables.put(key, varContents);
        }
    }

    private static String getHexString(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();
        return a != 255 ? String.format("#%02X%02X%02X%02X", r, g, b, a) : String.format("#%02X%02X%02X", r, g, b);
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