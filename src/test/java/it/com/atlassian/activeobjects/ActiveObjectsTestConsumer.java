package it.com.atlassian.activeobjects;

public interface ActiveObjectsTestConsumer
{
    Object run() throws Exception;
    void init() throws Exception;
}
