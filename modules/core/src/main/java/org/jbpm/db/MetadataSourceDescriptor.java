package org.jbpm.db;

import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.SourceDescriptor;

public class MetadataSourceDescriptor implements SourceDescriptor {

    @Override
    public SourceType getSourceType() {
        return SourceType.METADATA;
    }

    @Override
    public ScriptSourceInput getScriptSourceInput() {
        return null;
    }
}