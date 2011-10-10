package com.atlassian.activeobjects.admin.condition;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.atlassian.sal.api.user.UserManager;

import java.util.Map;

import static com.google.common.base.Preconditions.*;

public final class UserIsSysAdminCondition implements Condition
{
    private final UserManager userManager;

    public UserIsSysAdminCondition(final UserManager userManager)
    {
        this.userManager = checkNotNull(userManager);
    }

    public void init(final Map<String, String> params) throws PluginParseException
    {
    }

    public boolean shouldDisplay(final Map<String, Object> context)
    {
        final String userName = userManager.getRemoteUsername();
        return userName != null && userManager.isSystemAdmin(userName);
    }
}
