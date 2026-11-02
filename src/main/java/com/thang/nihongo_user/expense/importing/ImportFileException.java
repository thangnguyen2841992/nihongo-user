package com.thang.nihongo_user.expense.importing;

/** The whole file is unusable (wrong format, missing columns, too many rows). Message is shown to the user. */
public class ImportFileException extends RuntimeException {
    public ImportFileException(String message) { super(message); }
    public ImportFileException(String message, Throwable cause) { super(message, cause); }
}
