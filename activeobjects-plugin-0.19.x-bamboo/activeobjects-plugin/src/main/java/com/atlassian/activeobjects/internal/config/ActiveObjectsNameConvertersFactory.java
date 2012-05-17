package com.atlassian.activeobjects.internal.config;

import com.atlassian.activeobjects.ao.AtlassianTablePrefix;
import com.atlassian.activeobjects.internal.Prefix;
import net.java.ao.atlassian.AtlassianTableNameConverter;
import net.java.ao.builder.SimpleNameConverters;
import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.IndexNameConverter;
import net.java.ao.schema.NameConverters;
import net.java.ao.schema.SequenceNameConverter;
import net.java.ao.schema.TriggerNameConverter;
import net.java.ao.schema.UniqueNameConverter;

import static com.google.common.base.Preconditions.*;

public final class ActiveObjectsNameConvertersFactory implements NameConvertersFactory
{
    private final FieldNameConverter fieldNameConverter;
    private final SequenceNameConverter sequenceNameConverter;
    private final TriggerNameConverter triggerNameConverter;
    private final IndexNameConverter indexNameConverter;
    private final UniqueNameConverter uniqueNameConverter;

    public ActiveObjectsNameConvertersFactory(FieldNameConverter fieldNameConverter, SequenceNameConverter sequenceNameConverter,
                                              TriggerNameConverter triggerNameConverter, IndexNameConverter indexNameConverter,
                                              UniqueNameConverter uniqueNameConverter)
    {
        this.fieldNameConverter = checkNotNull(fieldNameConverter);
        this.sequenceNameConverter = checkNotNull(sequenceNameConverter);
        this.triggerNameConverter = checkNotNull(triggerNameConverter);
        this.indexNameConverter = checkNotNull(indexNameConverter);
        this.uniqueNameConverter = checkNotNull(uniqueNameConverter);
    }

    @Override
    public NameConverters getNameConverters(Prefix prefix)
    {
        return new SimpleNameConverters(
                new AtlassianTableNameConverter(new AtlassianTablePrefix(prefix)),
                fieldNameConverter,
                sequenceNameConverter,
                triggerNameConverter,
                indexNameConverter,
                uniqueNameConverter
        );
    }
}
