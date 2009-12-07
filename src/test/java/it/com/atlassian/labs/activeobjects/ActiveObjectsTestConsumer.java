package it.com.atlassian.labs.activeobjects;

public interface ActiveObjectsTestConsumer
{
    Object run() throws Exception;
    void init() throws Exception;
}
