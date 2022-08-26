package software.amazon.m2.application;

import com.google.common.annotations.VisibleForTesting;
import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandler extends BaseHandlerStd {

    public ListHandler() {
    }

    @VisibleForTesting
    ListHandler(ApiWrapper apiWrapper) {
        super(apiWrapper);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        AmazonWebServicesClientProxy proxy,
        ResourceHandlerRequest<ResourceModel> request,
        CallbackContext callbackContext,
        ProxyClient<M2Client> proxyClient) {

        logger.log(String.format("Invoking list handler for resource type '%s'", ResourceModel.TYPE_NAME));
        return proxy.initiate(getCallGraphFormat("ListApplications"), proxyClient, request.getDesiredResourceState(), callbackContext)
            .translateToServiceRequest(model -> Translator.toListApplicationsRequest(request.getNextToken()))
            .makeServiceCall(apiWrapper::listApplications)
            .done(listEnvsResponse -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(Translator.toListApplicationsResponse(listEnvsResponse))
                .status(OperationStatus.SUCCESS)
                .nextToken(listEnvsResponse.nextToken())
                .build());
    }
}
