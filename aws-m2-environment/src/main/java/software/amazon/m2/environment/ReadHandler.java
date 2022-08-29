package software.amazon.m2.environment;

import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.awssdk.services.m2.model.GetEnvironmentRequest;
import software.amazon.awssdk.services.m2.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.m2.model.ListTagsForResourceResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.m2.common.ExceptionHandlerWrapper;

public class ReadHandler extends BaseHandlerStd {

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<M2Client> proxyClient,
            final Logger logger) {

        // See https://github.com/aws-cloudformation/cloudformation-cli-java-plugin/blob/master/src/main/java/software/amazon/cloudformation/proxy/CallChain.java
        // and https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> proxy.initiate("AWS::M2::Environment-Read", proxyClient,  request.getDesiredResourceState(), callbackContext)
                        .translateToServiceRequest(Translator::translateToReadRequest)
                        .makeServiceCall((awsRequest, client) -> getEnvironment(proxyClient, logger, (GetEnvironmentRequest) awsRequest))
                        .done(awsResponse -> ProgressEvent.progress(Translator.translateFromReadResponse(awsResponse), callbackContext))
                )
                .then(progress -> retrieveResourceTags(proxy, proxyClient, progress.getResourceModel(), callbackContext, logger))
                .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }

    private ProgressEvent<ResourceModel, CallbackContext> retrieveResourceTags(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<M2Client> proxyClient,
            final ResourceModel model,
            final CallbackContext callbackContext,
            final Logger logger
    ) {
        return proxy.initiate("AWS::M2:Environment-ListTags", proxyClient,
                        model, callbackContext)
                .translateToServiceRequest(Translator::translateToListResourceTagsRequest)
                .makeServiceCall((awsRequest, client) -> listTags(awsRequest, proxyClient, logger))
                .done((awsResponse) -> {
                    if (awsResponse.tags() != null && !awsResponse.tags().isEmpty()) {
                        model.setTags(awsResponse.tags());
                    }
                    return ProgressEvent.progress(model, callbackContext);
                });
    }

    private ListTagsForResourceResponse listTags(final ListTagsForResourceRequest awsRequest,
                                                 final ProxyClient<M2Client> proxyClient,
                                                 final Logger logger) {
        ListTagsForResourceResponse awsResponse;
        try (M2Client m2client = proxyClient.client()) {
            awsResponse = ExceptionHandlerWrapper.wrapM2Exception("ListTags",
                    () -> proxyClient.injectCredentialsAndInvokeV2(awsRequest, m2client::listTagsForResource)
            );
        }
        return awsResponse;
    }
}