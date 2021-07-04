package me.mrgazdag.programs.reflectioneval.exception;

import me.mrgazdag.programs.reflectioneval.text.StyledText;

public class ParseException extends RuntimeException {
    private final StyledText styledMessage;
    private final int position;
    public ParseException(StyledText styledMessage) {
        super(styledMessage.getRawText());
        this.styledMessage = styledMessage;
        this.position = -1;
    }

    public ParseException(StyledText styledMessage, int position) {
        super(styledMessage.getRawText());
        this.styledMessage = styledMessage;
        this.position = position;
    }

    public ParseException(StyledText styledMessage, Throwable cause) {
        super(styledMessage.getRawText(), cause);
        this.styledMessage = styledMessage;
        this.position = -1;
    }

    public ParseException(StyledText styledMessage, Throwable cause, int position) {
        super(styledMessage.getRawText(), cause);
        this.styledMessage = styledMessage;
        this.position = position;
    }

    public ParseException(Throwable cause) {
        super(cause);
        this.styledMessage = null;
        this.position = -1;
    }

    public ParseException(Throwable cause, int position) {
        super(cause);
        this.styledMessage = null;
        this.position = position;
    }

    public StyledText getStyledMessage() {
        return styledMessage;
    }

    public int getPosition() {
        return position;
    }
}
