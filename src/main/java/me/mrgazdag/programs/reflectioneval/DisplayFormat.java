package me.mrgazdag.programs.reflectioneval;

import me.mrgazdag.programs.reflectioneval.text.Style;
import me.mrgazdag.programs.reflectioneval.text.StyledText;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class DisplayFormat {
    /*
    private final Map<FormatType, String> colors;
    private final Map<FormatType, Boolean> italics;
    private final Map<FormatType, Boolean> bolds;
    private final Map<FormatType, Boolean> underlineds;
    */

    private final Map<FormatType, Style> style;

    public DisplayFormat() {
        /*
        colors = new HashMap<>();
        italics = new HashMap<>();
        bolds = new HashMap<>();
        underlineds = new HashMap<>();
        */
        style = new HashMap<>();
        reset();
    }

    public void reset() {
        //intellij defaults
        initColor(FormatType.STRING,            Color.decode("#6A8759"),      false,  false,  false,  false);
        initColor(FormatType.NUMBER,            Color.decode("#6897BB"),      false,  false,  false,  false);
        initColor(FormatType.VARNAME,           Color.decode("#9876AA"),      false,  false,  false,  false);
        initColor(FormatType.KEYWORD,           Color.decode("#CC7832"),      false,  true,   false,  false);
        initColor(FormatType.VOID,              Color.decode("#555555"),      true,   false,  false,  false);
        initColor(FormatType.OBJECT,            Color.decode("#A9B7C6"),      false,  false,  false,  false);
        initColor(FormatType.CLASS,             Color.decode("#A9B7C6"),      false,  false,  false,  false);
        initColor(FormatType.METHOD_DOT,        Color.decode("#A9B7C6"),      false,  false,  false,  false);
        initColor(FormatType.PARENTHESES,       Color.decode("#A9B7C6"),      false,  false,  false,  false);
        initColor(FormatType.BRACKETS,       Color.decode("#A9B7C6"),      false,  false,  false,  false);
        initColor(FormatType.STATIC_METHOD,     Color.decode("#A9B7C6"),      false,  false,  false,  false);
        initColor(FormatType.INSTANCE_METHOD,   Color.decode("#A9B7C6"),      false,  false,  false,  false);
        initColor(FormatType.ENUM_CONSTANT,     Color.decode("#9876AA"),      true,   false,  false,  false);
        initColor(FormatType.OPERATOR,          Color.decode("#A9B7C6"),      false,  false,  false,  false);
        initColor(FormatType.COMMA,             Color.decode("#CC7832"),      false,  false,  false,  false);
        initColor(FormatType.SEMICOLON,         Color.decode("#CC7832"),      false,  false,  false,  false);
        initColor(FormatType.SPACE,             Color.decode("#A9B7C6"),      false,  false,  false,  false);
        initColor(FormatType.WARNING,           Color.decode("#BE9117"),      false,  false,  false,  false);
    }

    public Style getStyle(FormatType type) {
        return style.computeIfAbsent(type,(key) -> new Style());
    }

    private void initColor(FormatType type, Color color, boolean italic, boolean bold, @SuppressWarnings("SameParameterValue") boolean underlined, boolean strikethrough) {
        /*
        colors.put(type, color);
        italics.put(type, italic);
        bolds.put(type, bold);
        underlineds.put(type, underlined);
        */
        getStyle(type).color(color).italic(italic).bold(bold).underlined(underlined).strikethrough(strikethrough);
    }

    public void setItalic(FormatType type, boolean value) {
        getStyle(type).italic(value);
    }

    public boolean isItalic(FormatType type) {
        return getStyle(type).isItalic();
    }

    public void setBold(FormatType type, boolean value) {
        getStyle(type).bold(value);
    }

    public boolean isBold(FormatType type) {
        return getStyle(type).isBold();
    }

    public void setUnderlined(FormatType type, boolean value) {
        getStyle(type).underlined(value);
    }

    public boolean isUnderlined(FormatType type) {
        return getStyle(type).isUnderlined();
    }

    public void setStrikethrough(FormatType type, boolean value) {
        getStyle(type).strikethrough(value);
    }

    public boolean isStrikethrough(FormatType type) {
        return getStyle(type).isStrikethrough();
    }

    public void setColor(FormatType type, Color value) {
        getStyle(type).color(value);
    }

    public Color getColor(FormatType type) {
        return getStyle(type).getColor();
    }

    /*
    public Style getChatModifier(FormatType type) {
        return Style.EMPTY
                .withColor(TextColor.parseColor(getColor(type)))
                .withItalic(isItalic(type))
                .withBold(isBold(type))
                .withUnderlined(isUnderlined(type));
    }
    */
    public StyledText text(FormatType type, String str) {
        return new StyledText(type, getStyle(type), str);
    }
    /*

    public TextComponent text(FormatType type, String str) {
        return (TextComponent) new TextComponent(str).setStyle(getChatModifier(type));
    }
    */
}