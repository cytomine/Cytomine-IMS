package be.cytomine.exception;

public class WrongParameterException extends CytomineException {
    /**
     * Message map with this exception
     *
     * @param msg  Message
     */
    public WrongParameterException(String msg) {
        super(msg, 400);
    }
}
