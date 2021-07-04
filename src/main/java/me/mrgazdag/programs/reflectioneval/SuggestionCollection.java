package me.mrgazdag.programs.reflectioneval;

import me.mrgazdag.programs.reflectioneval.text.StyledText;

import java.util.ArrayList;
import java.util.List;

public class SuggestionCollection {
    private final int offset;
    private final String input;
    private final String remaining;
    private final List<SuggestionEntry> suggestions;

    public SuggestionCollection(String input, int offset) {
        this.offset = 0;
        this.input = input;
        this.remaining = input.substring(offset);
        this.suggestions = new ArrayList<>();
    }

    public String getInput() {
        return input;
    }

    public String getRemaining() {
        return remaining;
    }

    public SuggestionCollection createOffset(int offset) {
        return new SuggestionCollection(input, this.offset + offset);
    }

    public SuggestionCollection suggest(String suggestion) {
        suggestions.add(new SuggestionEntry(suggestion, null));
        return this;
    }
    public SuggestionCollection suggest(String suggestion, StyledText hover) {
        suggestions.add(new SuggestionEntry(suggestion, hover));
        return this;
    }

    public ImmutableSuggestionCollection build() {
        return new ImmutableSuggestionCollection(offset, suggestions);
    }
}
