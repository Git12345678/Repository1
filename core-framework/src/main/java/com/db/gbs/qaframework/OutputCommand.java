package com.db.gbs.qaframework;


/**
 *
 */
public class OutputCommand {

    private boolean result;
    private String output;
    private String error;

    public OutputCommand() {
        super();
    }

    public OutputCommand(boolean result, String output, String error) {
        super();
        this.result = result;
        this.output = output;
        this.error = error;
    }

    /**
     * @return Returns the result.
     */
    public boolean isResult() {
        return result;
    }

    /**
     * @param result The result to set.
     */
    public void setResult(boolean result) {
        this.result = result;
    }

    /**
     * @return Returns the output.
     */
    public String getOutput() {
        return output;
    }

    /**
     * @param output The output to set.
     */
    public void setOutput(String output) {
        this.output = output;
    }

    /**
     * @return Returns the error.
     */
    public String getError() {
        return error;
    }

    /**
     * @param error The error to set.
     */
    public void setError(String error) {
        this.error = error;
    }
}

