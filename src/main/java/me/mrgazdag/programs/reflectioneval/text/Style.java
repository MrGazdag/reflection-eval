package me.mrgazdag.programs.reflectioneval.text;

import org.json.JSONObject;

import java.awt.*;

public class Style {
    private Boolean bold;
    private Boolean italic;
    private Boolean underlined;
    private Boolean strikethrough;
    private Color color;

    public Style() {
        bold = null;
        italic = null;
        underlined = null;
        strikethrough = null;
        color = null;
    }

    public Style(JSONObject data) {
        if (data.has("bold"))  bold = data.getBoolean("bold");
        if (data.has("italic")) italic = data.getBoolean("italic");
        if (data.has("underlined"))  underlined = data.getBoolean("underlined");
        if (data.has("strikethrough"))  strikethrough = data.getBoolean("strikethrough");
        if (data.has("color"))  color = Color.decode(data.getString("color"));
    }

    public Style bold(Boolean value) {
        bold = value;
        return this;
    }

    public boolean isBold() {
        return bold;
    }

    public Style italic(Boolean value) {
        italic = value;
        return this;
    }

    public boolean isItalic() {
        return italic;
    }

    public Style underlined(Boolean value) {
        underlined = value;
        return this;
    }

    public boolean isUnderlined() {
        return underlined;
    }

    public Style strikethrough(Boolean value) {
        strikethrough = value;
        return this;
    }

    public boolean isStrikethrough() {
        return strikethrough;
    }

    public Style color(Color color) {
        this.color = color;
        return this;
    }
    public Style color(String color) {
        this.color = Color.decode(color);
        return this;
    }

    public Color getColor() {
        return color;
    }

    public JSONObject patch(JSONObject data) {
        if (color != null) data.put("color", getHexString(color));
        if (italic != null) data.put("italic", italic);
        if (bold != null) data.put("bold", bold);
        if (underlined != null) data.put("underlined", underlined);
        if (strikethrough != null) data.put("strikethrough", strikethrough);
        return data;
    }
    private static String getHexString(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();
        return a != 255 ? String.format("#%02X%02X%02X%02X", r, g, b, a) : String.format("#%02X%02X%02X", r, g, b);
    }
}
