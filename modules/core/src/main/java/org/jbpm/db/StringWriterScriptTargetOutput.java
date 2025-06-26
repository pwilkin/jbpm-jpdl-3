package org.jbpm.db;

import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import java.io.StringWriter;
import java.io.Writer;

public class StringWriterScriptTargetOutput implements ScriptTargetOutput {
    private final StringWriter writer = new StringWriter();

    @Override
    public void prepare() {
    }

    @Override
    public void accept(String command) {
        writer.write(command);
        writer.write(";\n");
    }

    @Override
    public void release() {
    }

    public StringWriter getWriter() {
        return writer;
    }
}
