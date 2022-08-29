package software.amazon.m2.application;

import com.google.common.annotations.VisibleForTesting;
import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {

    public ReadHandler() {
    }

    @VisibleForTesting
    ReadHandler(ApiWrapper apiWrapper) {
        super(apiWrapper);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        AmazonWebServicesClientProxy proxy,
        ResourceHandlerRequest<ResourceModel> request,
        CallbackContext callbackContext,
        ProxyClient<M2Client> proxyClient) {
        final ResourceModel model = request.getDesiredResourceState();

        logger.log(String.format("Invoking read handler for resource type '%s'. ApplicationArn - %s",
            ResourceModel.TYPE_NAME, model.getApplicationArn()));

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress -> getApplication(proxy, progress, proxyClient))
            .then(progress -> getApplicationTags(proxy, progress, proxyClient))
            .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }

    private ProgressEvent<ResourceModel, CallbackContext> getApplication(
        AmazonWebServicesClientProxy proxy,
        ProgressEvent<ResourceModel, CallbackContext> progress,
        ProxyClient<M2Client> proxyClient) {

        return proxy.initiate(getCallGraphFormat("GetApplication"), proxyClient, progress.getResourceModel(),
                progress.getCallbackContext())
            .translateToServiceRequest(Translator::toGetApplicationRequest)
            .makeServiceCall(apiWrapper::getApplication)
            .done((awsRequest, awsResponse, clientProxy, resourceModel, context) ->
                ProgressEvent.progress(Translator.getApplicationResourceModel(awsResponse), context));
    }

    private ProgressEvent<ResourceModel, CallbackContext> getApplicationTags(
        AmazonWebServicesClientProxy proxy,
        ProgressEvent<ResourceModel, CallbackContext> progress,
        ProxyClient<M2Client> proxyClient) {

        final ResourceModel resourceModel = progress.getResourceModel();
        return proxy.initiate(getCallGraphFormat("ListTags"), proxyClient,
                resourceModel, progress.getCallbackContext())
            .translateToServiceRequest(Translator::toListTagsRequest)
            .makeServiceCall(apiWrapper::listTags)
            .done((awsResponse) -> {
                resourceModel.setTags(awsResponse.tags());
                return ProgressEvent.progress(resourceModel, progress.getCallbackContext());
            });
    }
}
