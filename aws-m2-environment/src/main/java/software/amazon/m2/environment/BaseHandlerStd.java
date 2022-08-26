package software.amazon.m2.environment;

// Functionality shared across Create/Read/Update/Delete/List Handlers

import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.awssdk.services.m2.model.GetEnvironmentRequest;
import software.amazon.awssdk.services.m2.model.GetEnvironmentResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.m2.common.ClientBuilder;
import software.amazon.m2.common.ExceptionHandlerWrapper;

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        return handleRequest(
                proxy,
                request,
                callbackContext != null ? callbackContext : new CallbackContext(),
                proxy.newProxy(ClientBuilder::getClient),
                logger
        );
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<M2Client> proxyClient,
            final Logger logger);

    protected GetEnvironmentResponse getEnvironment(ProxyClient<M2Client> proxyClient, Logger logger, GetEnvironmentRequest awsRequest) {
        GetEnvironmentResponse awsResponse;
        try (M2Client m2client = proxyClient.client()) {
            awsResponse = ExceptionHandlerWrapper.wrapM2Exception("GetEnvironment",
                    () -> proxyClient.injectCredentialsAndInvokeV2(awsRequest, m2client::getEnvironment));
        }
        logger.log(String.format("%s with ID %s has successfully been read.",
                ResourceModel.TYPE_NAME, awsRequest.environmentId()));
        return awsResponse;
    }

}
