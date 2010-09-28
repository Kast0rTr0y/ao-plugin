package com.atlassian.activeobjects.backup;

import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 */
public interface BackupSerialiser<T>
{
    void serialise(T t, OutputStream os);

    T deserialise(InputStream is);
}
