package me.mrgazdag.programs.reflectioneval.exception;

import me.mrgazdag.programs.reflectioneval.text.StyledText;

public class ExecuteException extends RuntimeException {
    private final StyledText styledMessage;
    public ExecuteException(StyledText styledMessage) {
        super(styledMessage.getRawText());
        this.styledMessage = styledMessage;
    }

    public ExecuteException(StyledText styledMessage, Throwable cause) {
        super(styledMessage.getRawText(), cause);
        this.styledMessage = styledMessage;
    }

    public ExecuteException(Throwable cause) {
        super(cause);
        this.styledMessage = null;
    }

    public StyledText getStyledMessage() {
        return styledMessage;
    }
}
