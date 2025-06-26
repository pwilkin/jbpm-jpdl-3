package org.jbpm.db;

import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.hibernate.tool.schema.TargetType;
import java.util.EnumSet;

public class ScriptTargetDescriptor implements TargetDescriptor {
    private final ScriptTargetOutput scriptTargetOutput;

    public ScriptTargetDescriptor(ScriptTargetOutput scriptTargetOutput) {
        this.scriptTargetOutput = scriptTargetOutput;
    }

    @Override
    public EnumSet<TargetType> getTargetTypes() {
        return EnumSet.of(TargetType.SCRIPT);
    }

    @Override
    public ScriptTargetOutput getScriptTargetOutput() {
        return scriptTargetOutput;
    }
}
