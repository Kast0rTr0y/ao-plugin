package com.atlassian.activeobjects.backup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.java.ao.types.DatabaseType;
import net.java.ao.types.TypeManager;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Type;

import static com.atlassian.activeobjects.util.ActiveObjectsUtils.checkNotNull;

/**
 *
 */
public class GsonBackupSerialiser<T> implements BackupSerialiser<T>
{
    private static final String UTF_8 = "UTF-8";

    private final Type type;
    private final Gson gson;

    public GsonBackupSerialiser(Type type)
    {
        this.type = checkNotNull(type);
        this.gson = new GsonBuilder()
                .registerTypeAdapter(DatabaseType.class, new DatabaseTypeAdapter())
                .setPrettyPrinting()
                .create();
    }

    private static class DatabaseTypeAdapter implements JsonSerializer<DatabaseType<?>>, JsonDeserializer<DatabaseType<?>>
    {
        public JsonElement serialize(DatabaseType<?> src, Type typeOfSrc, JsonSerializationContext context)
        {
            return new JsonPrimitive(src.getType());
        }

        public DatabaseType<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
        {
            return TypeManager.getInstance().getType(json.getAsJsonPrimitive().getAsInt());
        }
    }

    public void serialise(T t, OutputStream os)
    {
        Writer writer = null;
        try
        {
            writer = getWriter(os);
            serialise(t, writer);
        }
        finally
        {
            closeQuietly(writer);
        }
    }

    public T deserialise(InputStream is)
    {
        Reader reader = null;
        try
        {
            reader = getReader(is);
            return (T) gson.fromJson(reader, type);
        }
        finally
        {
            closeQuietly(reader);
        }
    }

    private void serialise(T t, Appendable appendable)
    {
        gson.toJson(t, appendable);
    }

    private Reader getReader(InputStream is)
    {
        try
        {
            return new InputStreamReader(is, UTF_8);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new IllegalStateException("This is impossible! How did you get there?", e);
        }
    }

    private Writer getWriter(OutputStream os)
    {
        try
        {
            return new OutputStreamWriter(os, UTF_8);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new IllegalStateException("This is impossible! How did you get there?", e);
        }
    }

    private void closeQuietly(Closeable closeable)
    {
        if (closeable != null)
        {
            try
            {
                closeable.close();
            }
            catch (IOException ignored)
            {
                // ignored
            }
        }
    }
}
