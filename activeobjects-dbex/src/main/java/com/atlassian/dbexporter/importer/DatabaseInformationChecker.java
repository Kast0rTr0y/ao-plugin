package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.DatabaseInformation;

public interface DatabaseInformationChecker
{
    void check(DatabaseInformation information, Context context);
}
