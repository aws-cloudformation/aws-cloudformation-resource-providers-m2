package software.amazon.m2.environment;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.awssdk.services.m2.model.CreateEnvironmentRequest;
import software.amazon.awssdk.services.m2.model.CreateEnvironmentResponse;
import software.amazon.awssdk.services.m2.model.EnvironmentLifecycle;
import software.amazon.awssdk.services.m2.model.GetEnvironmentRequest;
import software.amazon.awssdk.services.m2.model.GetEnvironmentResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.m2.common.Constants;
import software.amazon.m2.common.ExceptionHandlerWrapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<M2Client> proxyClient,
            final Logger logger) {

        this.logger = logger;
        String clientRequestToken = request.getClientRequestToken();

        Map<String, String> tags = generateTagsForCreate(request.getDesiredResourceState(), request);

        // See // https://github.com/aws-cloudformation/cloudformation-cli-java-plugin/blob/master/src/main/java/software/amazon/cloudformation/proxy/CallChain.java
        // and https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html

        // Tagging is done by M2 on create
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> createEnvironment(proxy, proxyClient, progress.getCallbackContext(), progress.getResourceModel(), clientRequestToken, tags))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, progress.getCallbackContext(), proxyClient, logger));
    }

    private ProgressEvent<ResourceModel, CallbackContext> createEnvironment(final AmazonWebServicesClientProxy proxy,
                                                                            final ProxyClient<M2Client> proxyClient,
                                                                            final CallbackContext callbackContext,
                                                                            final ResourceModel model,
                                                                            final String clientRequestToken,
                                                                            final Map<String, String> tags) {
        return proxy.initiate("AWS::M2::Environment-Create", proxyClient, model, callbackContext)
                .translateToServiceRequest((resourceModel) ->
                        Translator.translateToCreateRequest(resourceModel, clientRequestToken, tags))
                .backoffDelay(Constants.BACKOFF_STRATEGY_STABILIZE)
                .makeServiceCall((createEnvRequest, client) -> callCreateEnvironmentApi(proxyClient, (CreateEnvironmentRequest) createEnvRequest))
                .stabilize(this::waitForEnvironmentToCreate)
                .progress();
    }

    private boolean waitForEnvironmentToCreate(
            final AwsRequest awsRequest,
            final CreateEnvironmentResponse createEnvironmentResponse,
            final ProxyClient<M2Client> proxyClient,
            final ResourceModel model,
            final CallbackContext callbackContext) {

        GetEnvironmentRequest getEnvRequest = GetEnvironmentRequest.builder()
                .environmentId(createEnvironmentResponse.environmentId()).build();
        model.setEnvironmentId(createEnvironmentResponse.environmentId());

        GetEnvironmentResponse getEnvResponse = getEnvironment(proxyClient, logger, getEnvRequest);
        model.setEnvironmentArn(getEnvResponse.environmentArn());

        if (EnvironmentLifecycle.AVAILABLE.equals(getEnvResponse.status())) {
            logger.log(String.format("%s [%s] has been successfully created.",
                    ResourceModel.TYPE_NAME, model.getEnvironmentId()));
            return true;
        } else if (EnvironmentLifecycle.FAILED.equals(getEnvResponse.status())) {
            logger.log(String.format("%s [%s] has failed to create.",
                    ResourceModel.TYPE_NAME, model.getEnvironmentId()));
            String errorMessage = String.format("CreateEnvironment failed: %s", getEnvResponse.statusReason());
            throw new CfnGeneralServiceException(errorMessage);
        } else {
            logger.log(String.format("%s [%s] has status %s", ResourceModel.TYPE_NAME,
                    model.getEnvironmentId(), getEnvResponse.status()));
        }
        return false;
    }

    private CreateEnvironmentResponse callCreateEnvironmentApi(final ProxyClient<M2Client> proxyClient,
                                                               final CreateEnvironmentRequest awsRequest) {
        try (M2Client m2client = proxyClient.client()) {
            return ExceptionHandlerWrapper.wrapM2Exception("CreateEnvironment",
                    () -> proxyClient.injectCredentialsAndInvokeV2(awsRequest, m2client::createEnvironment));
        }
    }

    /**
     * Generate tags to put into resource creation request.
     * This includes stack tags and user defined resource tags.
     * Stack tags are tags specified in the AWS console,
     * they should be shared by all resources created inside that stack.
     * Resource tags are tags that the customer specified in CloudFormation templates.
     * CloudFormation system tags (aws:cloudformation:<tag>) are not accepted by M2.
     */
    private Map<String, String> generateTagsForCreate(final ResourceModel resourceModel,
                                                      final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        final Map<String, String> tagMap = new HashMap<>();
        // Do not merge system tags into Tags map for M2 requests, not supported yet

        if (handlerRequest.getDesiredResourceTags() != null) {
            tagMap.putAll(handlerRequest.getDesiredResourceTags());
        }
        if (resourceModel.getTags() != null) {
            tagMap.putAll(resourceModel.getTags());
        }

        return Collections.unmodifiableMap(tagMap);
    }

}

