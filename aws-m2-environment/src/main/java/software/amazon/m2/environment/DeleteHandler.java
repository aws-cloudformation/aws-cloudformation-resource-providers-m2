package software.amazon.m2.environment;

import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.awssdk.services.m2.model.DeleteEnvironmentRequest;
import software.amazon.awssdk.services.m2.model.DeleteEnvironmentResponse;
import software.amazon.awssdk.services.m2.model.GetEnvironmentRequest;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.m2.common.Constants;
import software.amazon.m2.common.ExceptionHandlerWrapper;


public class DeleteHandler extends BaseHandlerStd {
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
                // DeleteHandler contract expects a failure response when the resource being deleted doesn't exist.
                // getEnvironment throws a CfnNotFoundException for that case, and the framework sends a FAILED response for it.
                .then(progress -> proxy.initiate("AWS::M2::Environment-Delete-PreExistenceCheck", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                        .translateToServiceRequest(Translator::translateToReadRequest)
                        .makeServiceCall((awsRequest, client) -> getEnvironment(proxyClient, logger, (GetEnvironmentRequest) awsRequest))
                        .done(getEnvResponse -> ProgressEvent.defaultInProgressHandler(callbackContext, 0,
                                Translator.translateFromReadResponse(getEnvResponse)))
                )
                .then(progress ->
                        proxy.initiate("AWS::M2::Environment-Delete", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToDeleteRequest)
                                .backoffDelay(Constants.BACKOFF_STRATEGY_STABILIZE)
                                .makeServiceCall((deleteEnvRequest, client) -> deleteEnvironment(proxyClient, (DeleteEnvironmentRequest) deleteEnvRequest))
                                .stabilize((awsRequest, awsResponse, pClient, model, callback) -> waitForEnvironmentToBeDeleted(proxyClient, model))
                                .progress()
                )
                // When the delete handler returns SUCCESS, the ProgressEvent object MUST NOT contain a model.
                .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }

    private boolean waitForEnvironmentToBeDeleted(final ProxyClient<M2Client> proxyClient, final ResourceModel model) {
        boolean stabilized = false;
        try {
            GetEnvironmentRequest getRequest = GetEnvironmentRequest.builder().environmentId(model.getEnvironmentId()).build();
            getEnvironment(proxyClient, logger, getRequest);
        } catch (CfnNotFoundException rnfe) {
            stabilized = true;
        }
        logger.log(String.format("%s [%s] delete has stabilized: %s", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier(), stabilized));
        return stabilized;
    }

    private DeleteEnvironmentResponse deleteEnvironment
            (final ProxyClient<M2Client> proxyClient, final DeleteEnvironmentRequest awsRequest) {
        DeleteEnvironmentResponse awsResponse = null;
        try (M2Client m2client = proxyClient.client()) {
            awsResponse = ExceptionHandlerWrapper.wrapM2Exception("DeleteEnvironment",
                    () -> proxyClient.injectCredentialsAndInvokeV2(awsRequest,
                            m2client::deleteEnvironment));
        }
        logger.log(String.format("Delete %s successfully initiated.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

}

