package software.amazon.m2.application;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Map;

public class CreateHandler extends BaseHandlerStd {

    public CreateHandler() {
    }

    @VisibleForTesting
    CreateHandler(ApiWrapper apiWrapper) {
        super(apiWrapper);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        AmazonWebServicesClientProxy proxy,
        ResourceHandlerRequest<ResourceModel> request,
        CallbackContext callbackContext,
        ProxyClient<M2Client> proxyClient) {
        logger.log(String.format("%s create handler is being invoked", ResourceModel.TYPE_NAME));

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress -> createApplication(proxy, progress, request, proxyClient))
            .then(progress -> new ReadHandler(apiWrapper).handleRequest(proxy, request, callbackContext, logger));
    }

    private ProgressEvent<ResourceModel, CallbackContext> createApplication(
        AmazonWebServicesClientProxy proxy,
        ProgressEvent<ResourceModel, CallbackContext> progress,
        ResourceHandlerRequest<ResourceModel> handlerRequest,
        ProxyClient<M2Client> proxyClient) {

        final String clientRequestToken = handlerRequest.getClientRequestToken();
        final Map<String, String> tags = generateTagsForCreate(handlerRequest);

        return proxy.initiate(getCallGraphFormat("CreateApplication"), proxyClient, progress.getResourceModel(),
                progress.getCallbackContext())
            .translateToServiceRequest((model) -> Translator.toCreateApplicationRequest(model, clientRequestToken, tags))
            .makeServiceCall(apiWrapper::createApplication)
            .stabilize((request, response, client, model, callback)
                -> waitForApplicationToStabilize(response.applicationId(), model, client))
            .progress();
    }

    private Map<String, String> generateTagsForCreate(final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        final ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.builder();

        if (handlerRequest.getDesiredResourceTags() != null) {
            mapBuilder.putAll(handlerRequest.getDesiredResourceTags());
        }

        final ResourceModel resourceModel = handlerRequest.getDesiredResourceState();
        if (resourceModel.getTags() != null) {
            mapBuilder.putAll(resourceModel.getTags());
        }
        return mapBuilder.build();
    }
}
