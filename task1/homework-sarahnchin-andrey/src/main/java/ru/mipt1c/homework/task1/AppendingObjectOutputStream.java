package ru.mipt1c.homework.task1;

import java.io.*;

/* ObjectOutputStream specialization that does not write a header. */
public class AppendingObjectOutputStream extends ObjectOutputStream {

    public AppendingObjectOutputStream(OutputStream out) throws IOException {
        super(out);
    }

    @Override
    protected void writeStreamHeader() throws IOException {
        reset();
    }

}