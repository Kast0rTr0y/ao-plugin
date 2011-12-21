package com.atlassian.activeobjects.internal.config;

import com.atlassian.activeobjects.ao.ActiveObjectsTableNameConverter;
import com.atlassian.activeobjects.internal.Prefix;
import net.java.ao.builder.SimpleNameConverters;
import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.IndexNameConverter;
import net.java.ao.schema.NameConverters;
import net.java.ao.schema.SequenceNameConverter;
import net.java.ao.schema.TriggerNameConverter;

import static com.google.common.base.Preconditions.*;

public final class ActiveObjectsNameConvertersFactory implements NameConvertersFactory
{
    private final FieldNameConverter fieldNameConverter;
    private final SequenceNameConverter sequenceNameConverter;
    private final TriggerNameConverter triggerNameConverter;
    private final IndexNameConverter indexNameConverter;

    public ActiveObjectsNameConvertersFactory(FieldNameConverter fieldNameConverter, SequenceNameConverter sequenceNameConverter,
                                              TriggerNameConverter triggerNameConverter, IndexNameConverter indexNameConverter)
    {
        this.fieldNameConverter = checkNotNull(fieldNameConverter);
        this.sequenceNameConverter = checkNotNull(sequenceNameConverter);
        this.triggerNameConverter = checkNotNull(triggerNameConverter);
        this.indexNameConverter = checkNotNull(indexNameConverter);
    }

    @Override
    public NameConverters getNameConverters(Prefix prefix)
    {
        return new SimpleNameConverters(
                new ActiveObjectsTableNameConverter(prefix),
                fieldNameConverter,
                sequenceNameConverter,
                triggerNameConverter,
                indexNameConverter
        );
    }
}