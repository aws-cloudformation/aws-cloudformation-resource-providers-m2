package software.amazon.m2.common;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.cloudformation.LambdaWrapper;

public final class ClientBuilder {
    private static final RetryPolicy RETRY_POLICY =
        RetryPolicy.builder()
            .numRetries(6)
            .retryCondition(RetryCondition.defaultRetryCondition())
            .build();

    public static M2Client getClient() {
        return M2Client.builder()
            .httpClient(LambdaWrapper.HTTP_CLIENT)
            .overrideConfiguration(ClientOverrideConfiguration.builder().retryPolicy(RETRY_POLICY).build())
            .build();
    }

    private ClientBuilder() {
    }
}
