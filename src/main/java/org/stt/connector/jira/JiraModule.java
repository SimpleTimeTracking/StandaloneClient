package org.stt.connector.jira;

import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import dagger.Module;
import dagger.Provides;

@Module
public class JiraModule {
    @Provides
    static JiraRestClientFactory provideJiraRestClientFactory() {
        return new AsynchronousJiraRestClientFactory();
    }
}
