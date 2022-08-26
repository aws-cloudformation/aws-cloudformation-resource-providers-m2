package software.amazon.m2.environment;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.awssdk.services.m2.model.EnvironmentLifecycle;
import software.amazon.awssdk.services.m2.model.GetEnvironmentRequest;
import software.amazon.awssdk.services.m2.model.GetEnvironmentResponse;
import software.amazon.awssdk.services.m2.model.TagResourceRequest;
import software.amazon.awssdk.services.m2.model.TagResourceResponse;
import software.amazon.awssdk.services.m2.model.UntagResourceRequest;
import software.amazon.awssdk.services.m2.model.UntagResourceResponse;
import software.amazon.awssdk.services.m2.model.UpdateEnvironmentRequest;
import software.amazon.awssdk.services.m2.model.UpdateEnvironmentResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.m2.common.Constants;
import software.amazon.m2.common.ExceptionHandlerWrapper;
import software.amazon.m2.common.TagHelper;

import java.util.Map;
import java.util.Set;

public class UpdateHandler extends BaseHandlerStd {

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<M2Client> proxyClient,
            final Logger logger) {

        this.logger = logger;

        // See https://github.com/aws-cloudformation/cloudformation-cli-java-plugin/blob/master/src/main/java/software/amazon/cloudformation/proxy/CallChain.java
        // and https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                // We do not need to check if environment exists;
                // - the updateEnvironment api will throw ResourceNotFoundException in that case,
                //   which is the expected behavior for update handlers.
                .then(progress ->
                        proxy.initiate("AWS::M2::Environment-Update", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToUpdateRequest)
                                .backoffDelay(Constants.BACKOFF_STRATEGY_STABILIZE)
                                .makeServiceCall((awsRequest, client) -> updateEnvironment((UpdateEnvironmentRequest) awsRequest, proxyClient))
                                .stabilize(this::waitForUpdated)
                                .progress()
                )
                .then(progress -> addNewResourceTags(proxy, request, callbackContext, progress.getResourceModel(), proxyClient, logger))
                .then(progress -> removeResourceTags(proxy, request, callbackContext, progress.getResourceModel(), proxyClient, logger))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private UpdateEnvironmentResponse updateEnvironment(final UpdateEnvironmentRequest awsRequest,
                                                        final ProxyClient<M2Client> proxyClient) {
        UpdateEnvironmentResponse awsResponse;
        try (M2Client m2client = proxyClient.client()) {
            awsResponse = ExceptionHandlerWrapper.wrapM2Exception("UpdateEnvironment",
                    () -> proxyClient.injectCredentialsAndInvokeV2(awsRequest, m2client::updateEnvironment));
        }
        logger.log(String.format("Update %s has successfully been initiated.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

    private Boolean waitForUpdated(final AwsRequest awsRequest,
                                   final UpdateEnvironmentResponse updateEnvironmentResponse,
                                   final ProxyClient<M2Client> proxyClient,
                                   final ResourceModel model,
                                   final CallbackContext callbackContext) {
        model.setEnvironmentId(updateEnvironmentResponse.environmentId());
        GetEnvironmentRequest getEnvRequest = GetEnvironmentRequest.builder()
                .environmentId(model.getEnvironmentId()).build();

        GetEnvironmentResponse getEnvResponse = getEnvironment(proxyClient, logger, getEnvRequest);
        model.setEnvironmentArn(getEnvResponse.environmentArn());

        /*
         * Normally, we would return !EnvironmentLifecycle.UPDATING.equals(getEnvResponse.status())
         * but the 'Updating' enum value is not available in sdk currently.
         */
        if (EnvironmentLifecycle.AVAILABLE.equals(getEnvResponse.status())) {
            logger.log(String.format("%s [%s] has been successfully updated.",
                    ResourceModel.TYPE_NAME, model.getEnvironmentId()));
            return true;
        } else if (EnvironmentLifecycle.FAILED.equals(getEnvResponse.status())) {
            logger.log(String.format("%s [%s] has failed to update.",
                    ResourceModel.TYPE_NAME, model.getEnvironmentId()));
            String errorMessage = String.format("UpdateEnvironment failed: %s", getEnvResponse.statusReason());
            throw new CfnGeneralServiceException(errorMessage);
        } else {
            logger.log(String.format("%s [%s] has status %s", ResourceModel.TYPE_NAME,
                    model.getEnvironmentId(), getEnvResponse.status()));
        }
        return false;
    }

    /**
     * Add any new resource tags.
     * Calls the m2:TagResource API for adding tags.
     */
    private ProgressEvent<ResourceModel, CallbackContext>
    addNewResourceTags(final AmazonWebServicesClientProxy proxy,
                       final ResourceHandlerRequest<ResourceModel> handlerRequest,
                       final CallbackContext callbackContext,
                       final ResourceModel resourceModel,
                       final ProxyClient<M2Client> serviceClient,
                       final Logger logger) {

        Map<String, String> addedTags = TagHelper.generateTagsToAdd(handlerRequest);
        if (!addedTags.isEmpty()) {
            logger.log(String.format("Going to add tags for environment: %s with AccountId: %s",
                    resourceModel.getEnvironmentId(), handlerRequest.getAwsAccountId()));

            return proxy.initiate("AWS::M2::Environment-TagResource", serviceClient, resourceModel, callbackContext)
                    .translateToServiceRequest(model -> Translator.tagResourceRequest(model, addedTags))
                    .makeServiceCall((request, client) -> tagResource(request, client, logger))
                    .progress();
        }
        return ProgressEvent.progress(resourceModel, callbackContext);
    }

    private TagResourceResponse tagResource(final TagResourceRequest awsRequest,
                                            final ProxyClient<M2Client> proxyClient,
                                            final Logger logger) {
        TagResourceResponse awsResponse;
        try (M2Client m2client = proxyClient.client()) {
            awsResponse = ExceptionHandlerWrapper.wrapM2Exception("TagResource",
                    () -> proxyClient.injectCredentialsAndInvokeV2(awsRequest, m2client::tagResource));
        }
        logger.log(String.format("%s [%s] has successfully been tagged for added tags.",
                ResourceModel.TYPE_NAME, awsRequest.resourceArn()));
        return awsResponse;
    }

    /**
     * Untag any removed resource tags.
     * Calls m2:UntagResource API for removing tags.
     */
    private ProgressEvent<ResourceModel, CallbackContext>
    removeResourceTags(final AmazonWebServicesClientProxy proxy,
                       final ResourceHandlerRequest<ResourceModel> handlerRequest,
                       final CallbackContext callbackContext,
                       final ResourceModel resourceModel,
                       final ProxyClient<M2Client> serviceClient,
                       final Logger logger) {

        Set<String> removedTags = TagHelper.generateTagsToRemove(handlerRequest);
        if (!removedTags.isEmpty()) {
            logger.log(String.format("Going to remove tags for environment: %s with AccountId: %s",
                    resourceModel.getEnvironmentId(), handlerRequest.getAwsAccountId()));

            return proxy.initiate("AWS::M2::Environment-UntagResource", serviceClient, resourceModel, callbackContext)
                    .translateToServiceRequest(model -> Translator.untagResourceRequest(model, removedTags))
                    .makeServiceCall((request, client) -> untagResource(request, client, logger))
                    .progress();
        }
        return ProgressEvent.progress(resourceModel, callbackContext);
    }

    private UntagResourceResponse untagResource(final UntagResourceRequest awsRequest,
                                                final ProxyClient<M2Client> proxyClient,
                                                final Logger logger) {
        UntagResourceResponse awsResponse;
        try (M2Client m2client = proxyClient.client()) {
            awsResponse = ExceptionHandlerWrapper.wrapM2Exception("UntagResource",
                    () -> proxyClient.injectCredentialsAndInvokeV2(awsRequest, m2client::untagResource));
        }
        logger.log(String.format("%s [%s] has successfully been untagged for removed tags.",
                ResourceModel.TYPE_NAME, awsRequest.resourceArn()));
        return awsResponse;
    }

}
