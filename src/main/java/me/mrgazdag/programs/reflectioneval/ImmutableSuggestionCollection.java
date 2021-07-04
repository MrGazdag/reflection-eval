package me.mrgazdag.programs.reflectioneval;

import java.util.Collections;
import java.util.List;

public class ImmutableSuggestionCollection {
    private final int offset;
    private final List<SuggestionEntry> suggestions;

    ImmutableSuggestionCollection(int offset, List<SuggestionEntry> suggestions) {
        this.offset = offset;
        this.suggestions = Collections.unmodifiableList(suggestions);
    }

    public int getOffset() {
        return offset;
    }

    public List<SuggestionEntry> getSuggestions() {
        return suggestions;
    }
}
