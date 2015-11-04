package com.mtihc.regionselfservice.v2.plots.exceptions;

import com.mtihc.regionselfservice.v2.plugin.SelfServiceMessage;
import com.mtihc.regionselfservice.v2.plugin.SelfServiceMessage.MessageKey;


public class PlotBoundsException extends PlotControlException {
    
    private static final long serialVersionUID = -9136937024689204439L;
    
    public enum Type {
	SELECTION_TOO_SMALL(SelfServiceMessage.getMessage(MessageKey.error_selection_to_small)),
	SELECTION_TOO_BIG(SelfServiceMessage.getMessage(MessageKey.error_selection_to_big)),
	SELECTION_TOO_LOW(SelfServiceMessage.getMessage(MessageKey.error_selection_to_low)),
	SELECTION_TOO_HIGH(SelfServiceMessage.getMessage(MessageKey.error_selection_to_high));
	
	private String message;
	
	private Type(String msg) {
	    this.message = msg;
	}
	
	public String getMessage() {
	    return this.message;
	}
	
	public String getMessage(Object... args) {
	    return String.format(getMessage(), args);
	}
	
    }
    
    private Type type;
    
    public PlotBoundsException(Type type) {
	super(type.getMessage());
	this.type = type;
    }
    
    public PlotBoundsException(Type type, int topY, int bottomY, int min, int max) {
	super(getMessage(type, topY, bottomY, min, max));
	this.type = type;
    }
    
    static private String getMessage(Type type, int topY, int bottomY, int min, int max) {
	if (type.equals(Type.SELECTION_TOO_LOW)) {
	    return type.getMessage(bottomY, min);
	} else if (type.equals(Type.SELECTION_TOO_HIGH)) {
	    return type.getMessage(topY, max);
	} else {
	    return SelfServiceMessage.getMessage(MessageKey.error_selection);
	}
    }
    
    public PlotBoundsException(Type type, int width, int length, int height, int minimum, int maximum, int minHeight, int maxHeight) {
	super(getMessage(type, width, length, height, minimum, maximum, minHeight, maxHeight));
	this.type = type;
    }
    
    static private String getMessage(Type type, int width, int length, int height, int minimum, int maximum, int minHeight, int maxHeight) {
	if (type.equals(Type.SELECTION_TOO_BIG)) {
	    return type.getMessage(width, length, height, maximum, maximum, maxHeight);
	} else if (type.equals(Type.SELECTION_TOO_SMALL)) {
	    return type.getMessage(width, length, height, minimum, minimum, minHeight);
	} else {
	    return SelfServiceMessage.getMessage(MessageKey.error_selection);
	}
    }
    
    public Type getType() {
	return this.type;
    }
}
