package software.amazon.m2.application;

import com.google.common.annotations.VisibleForTesting;
import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.awssdk.services.m2.model.DeleteApplicationRequest;
import software.amazon.awssdk.services.m2.model.DeleteApplicationResponse;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {

    public DeleteHandler() {
    }

    @VisibleForTesting
    DeleteHandler(ApiWrapper apiWrapper) {
        super(apiWrapper);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        AmazonWebServicesClientProxy proxy,
        ResourceHandlerRequest<ResourceModel> request,
        CallbackContext callbackContext,
        ProxyClient<M2Client> proxyClient) {

        final ResourceModel model = request.getDesiredResourceState();
        logger.log(String.format("Invoking delete handler for resource type '%s', Arn '%s'",
            ResourceModel.TYPE_NAME, model.getApplicationArn()));

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> checkIfExists(proxy, progress, request, proxyClient))
                .then(progress -> deleteApplication(proxy, progress, request, proxyClient))
                .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }

    private ProgressEvent<ResourceModel, CallbackContext> checkIfExists(final AmazonWebServicesClientProxy proxy,
                                                                        final ProgressEvent<ResourceModel, CallbackContext> progress,
                                                                        final ResourceHandlerRequest<ResourceModel> request,
                                                                        final ProxyClient<M2Client> proxyClient) {
        return proxy.initiate(getCallGraphFormat("DeleteApplication-PreExistenceCheck"), proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(Translator::toGetApplicationRequest)
                .makeServiceCall((awsRequest, client) -> apiWrapper.getApplication(awsRequest, proxyClient))
                .done(getAppResponse -> ProgressEvent.defaultInProgressHandler(progress.getCallbackContext(), 0,
                        Translator.getApplicationResourceModel(getAppResponse)));
    }

    private ProgressEvent<ResourceModel, CallbackContext> deleteApplication(
        AmazonWebServicesClientProxy proxy,
        ProgressEvent<ResourceModel, CallbackContext> progress,
        ResourceHandlerRequest<ResourceModel> request,
        ProxyClient<M2Client> proxyClient) {

        return proxy.initiate(getCallGraphFormat("DeleteApplication"), proxyClient, progress.getResourceModel(),
                progress.getCallbackContext())
            .translateToServiceRequest(Translator::toDeleteApplicationRequest)
            .makeServiceCall(apiWrapper::deleteApplication)
            .stabilize(this::waitForApplicationToDelete)
            .progress();
    }

    private boolean waitForApplicationToDelete(final DeleteApplicationRequest request,
                                               final DeleteApplicationResponse deleteResponse,
                                               final ProxyClient<M2Client> proxyClient,
                                               final ResourceModel model,
                                               final CallbackContext callbackContext) {
        try {
            apiWrapper.getApplication(Translator.toGetApplicationRequest(model.getApplicationArn()), proxyClient);
            // not deleted yet
            return false;
        } catch (CfnNotFoundException e) {
            logger.log(String.format("Successfully deleted resource '%s' with id '%s'",
                ResourceModel.TYPE_NAME, request.applicationId()));
            return true;
        }
    }
}
