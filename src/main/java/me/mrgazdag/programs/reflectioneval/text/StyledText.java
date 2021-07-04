package me.mrgazdag.programs.reflectioneval.text;

import me.mrgazdag.programs.reflectioneval.DisplayFormat;
import me.mrgazdag.programs.reflectioneval.FormatType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class StyledText {
    private final String text;
    private final FormatType formatType;
    private final Style style;
    private final List<StyledText> children;

    public StyledText(String text) {
        this.formatType = null;
        this.style = new Style();
        this.text = text;
        this.children = new ArrayList<>();
    }

    public StyledText(Style style, String text) {
        this.formatType = null;
        this.style = style == null ? new Style() : style;
        this.text = text;
        this.children = new ArrayList<>();
    }
    public StyledText(FormatType type, Style style, String text) {
        this.formatType = type;
        this.style = style == null ? new Style() : style;
        this.text = text;
        this.children = new ArrayList<>();
    }

    public StyledText(JSONObject data) {
        formatType = data.has("formatType") ? data.getEnum(FormatType.class, "formatType") : null;
        text = data.getString("text");
        style = new Style(data);
        this.children = new ArrayList<>();
        if (data.has("extra")) {
            JSONArray children = data.getJSONArray("extra");
            for (Object child : children) {
                if (child instanceof JSONObject jChild) {
                    this.children.add(new StyledText(jChild));
                }
            }
        }
    }

    public StyledText append(StyledText text) {
        children.add(text);
        return this;
    }

    public List<StyledText> getChildren() {
        return children;
    }

    public String getText() {
        return text;
    }

    public FormatType getFormatType() {
        return formatType;
    }

    public Style getStyle() {
        return style;
    }

    public JSONObject toJSON() {
        JSONObject data = new JSONObject();
        data.put("text", text);
        if (formatType != null) data.put("formatType", formatType.name());
        if (!children.isEmpty()) {
            JSONArray children = new JSONArray();
            for (StyledText child : this.children) {
                children.put(child.toJSON());
            }
            data.put("extra", children);
        }
        if (style != null) style.patch(data);
        return data;
    }

    public String getRawText() {
        StringBuilder sb = new StringBuilder(text);
        for (StyledText child : children) {
            sb.append(child.getRawText());
        }
        return sb.toString();
    }
}
