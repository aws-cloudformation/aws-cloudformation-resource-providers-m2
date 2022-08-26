package software.amazon.m2.application;

import com.google.common.annotations.VisibleForTesting;
import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.m2.common.TagHelper;

import java.util.Map;
import java.util.Set;

public class UpdateHandler extends BaseHandlerStd {

    public UpdateHandler() {
    }

    @VisibleForTesting
    UpdateHandler(ApiWrapper apiWrapper) {
        super(apiWrapper);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            AmazonWebServicesClientProxy proxy,
            ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext,
            ProxyClient<M2Client> proxyClient) {
        logger.log(String.format("Invoking update handler for resource type '%s' with Arn '%s'",
                ResourceModel.TYPE_NAME, request.getDesiredResourceState().getApplicationArn()));

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> getCurrentVersion(proxy, progress, request, proxyClient))
                .then(progress -> updateApplication(proxy, progress, proxyClient))
                .then(progress -> removeTagsForApplication(proxy, progress, callbackContext, request, proxyClient))
                .then(progress -> addTagsForApplication(proxy, progress, callbackContext, request, proxyClient))
                .then(progress -> new ReadHandler(apiWrapper).handleRequest(proxy, request, callbackContext, logger));
    }

    private ProgressEvent<ResourceModel, CallbackContext> getCurrentVersion(final AmazonWebServicesClientProxy proxy,
                                                                            final ProgressEvent<ResourceModel, CallbackContext> progress,
                                                                            final ResourceHandlerRequest<ResourceModel> request,
                                                                            final ProxyClient<M2Client> proxyClient) {
        return proxy.initiate(getCallGraphFormat("UpdateApplication-GetCurrentVersion"), proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(Translator::toGetApplicationRequest)
                .makeServiceCall((awsRequest, client) -> apiWrapper.getApplication(awsRequest, proxyClient))
                .done(getAppResponse -> {
                            progress.getCallbackContext().setCurrentApplicationVersion(getAppResponse.latestVersion().applicationVersion());
                            return ProgressEvent.defaultInProgressHandler(progress.getCallbackContext(), 0,
                                    progress.getResourceModel());
                        }
                );
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateApplication(
        AmazonWebServicesClientProxy proxy,
        ProgressEvent<ResourceModel, CallbackContext> progress,
        ProxyClient<M2Client> proxyClient) {

        return proxy.initiate(getCallGraphFormat("UpdateApplication"), proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(model ->
                        Translator.toUpdateApplicationRequest(model,
                                progress.getCallbackContext().getCurrentApplicationVersion()))
                .makeServiceCall(apiWrapper::updateApplication)
                .stabilize((request, response, client, model, callback)
                        -> waitForApplicationToStabilize(request.applicationId(), model, client))
                .progress();
    }

    private ProgressEvent<ResourceModel, CallbackContext> removeTagsForApplication(
        AmazonWebServicesClientProxy proxy,
        ProgressEvent<ResourceModel, CallbackContext> progress,
        CallbackContext callbackContext,
        ResourceHandlerRequest<ResourceModel> request,
        ProxyClient<M2Client> proxyClient) {

        final ResourceModel resourceModel = progress.getResourceModel();
        final Set<String> tagsToRemove = TagHelper.generateTagsToRemove(request);
        if (!tagsToRemove.isEmpty()) {
            logger.log(String.format("Removing tags for application with id '%s'", resourceModel.getApplicationId()));
            return proxy.initiate(getCallGraphFormat("UntagResource"), proxyClient, resourceModel, callbackContext)
                .translateToServiceRequest(model -> Translator.toUntagResourceRequest(model, tagsToRemove))
                .makeServiceCall(apiWrapper::untagResource)
                .progress();
        }
        return ProgressEvent.progress(resourceModel, callbackContext);
    }

    private ProgressEvent<ResourceModel, CallbackContext> addTagsForApplication(
        AmazonWebServicesClientProxy proxy,
        ProgressEvent<ResourceModel, CallbackContext> progress,
        CallbackContext callbackContext, ResourceHandlerRequest<ResourceModel> requestHandler,
        ProxyClient<M2Client> proxyClient) {

        final ResourceModel resourceModel = progress.getResourceModel();
        final Map<String, String> tagsToAdd = TagHelper.generateTagsToAdd(requestHandler);
        if (!tagsToAdd.isEmpty()) {
            logger.log(String.format("Adding tags for application with id '%s'", resourceModel.getApplicationId()));
            return proxy.initiate(getCallGraphFormat("TagResource"), proxyClient, resourceModel, callbackContext)
                .translateToServiceRequest(model -> Translator.toTagResourceRequest(model, tagsToAdd))
                .makeServiceCall(apiWrapper::tagResource)
                .progress();
        }
        return ProgressEvent.progress(resourceModel, callbackContext);
    }
}
