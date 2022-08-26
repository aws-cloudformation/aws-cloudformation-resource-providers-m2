package software.amazon.m2.environment;

import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.awssdk.services.m2.model.ListEnvironmentsRequest;
import software.amazon.awssdk.services.m2.model.ListEnvironmentsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.m2.common.ExceptionHandlerWrapper;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<M2Client> proxyClient,
            final Logger logger) {

        // See https://github.com/aws-cloudformation/cloudformation-cli-java-plugin/blob/master/src/main/java/software/amazon/cloudformation/proxy/CallChain.java
        // and https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
        return proxy.initiate("AWS::M2::Environment-List", proxyClient, request.getDesiredResourceState(), callbackContext)
                .translateToServiceRequest(model -> Translator.translateToListRequest(request.getNextToken()))
                .makeServiceCall((awsRequest, client) -> listEnvironments(proxyClient, logger, (ListEnvironmentsRequest) awsRequest))
                .done(listEnvsResponse -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModels(Translator.translateFromListResponse(listEnvsResponse))
                        .status(OperationStatus.SUCCESS)
                        .nextToken(listEnvsResponse.nextToken())
                        .build());
    }

    private ListEnvironmentsResponse listEnvironments(final ProxyClient<M2Client> proxyClient,
                                                      final Logger logger,
                                                      final ListEnvironmentsRequest awsRequest) {
        ListEnvironmentsResponse awsResponse;
        try (M2Client m2client = proxyClient.client()) {
            awsResponse = ExceptionHandlerWrapper.wrapM2Exception("ListEnvironments",
                    () -> proxyClient.injectCredentialsAndInvokeV2(awsRequest, m2client::listEnvironments));
        }
        logger.log(String.format("Successfully listed %ss.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

}
