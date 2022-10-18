package software.amazon.m2.application;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.Validate;
import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.awssdk.services.m2.model.ApplicationLifecycle;
import software.amazon.awssdk.services.m2.model.ApplicationVersionLifecycle;
import software.amazon.awssdk.services.m2.model.GetApplicationRequest;
import software.amazon.awssdk.services.m2.model.GetApplicationResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.m2.common.ClientBuilder;

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    private final String CALL_GRAPH_FORMAT = ResourceModel.TYPE_NAME + "::%s";

    protected final ApiWrapper apiWrapper;

    protected Logger logger;

    public BaseHandlerStd() {
        this(new ApiWrapper());
    }

    @VisibleForTesting
    BaseHandlerStd(ApiWrapper apiWrapper) {
        this.apiWrapper = apiWrapper;
    }

    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        this.logger = logger;
        this.apiWrapper.setLogger(logger);
        return handleRequest(
            proxy,
            request,
            callbackContext != null ? callbackContext : new CallbackContext(),
            proxy.newProxy(ClientBuilder::getClient)
        );
    }

    /**
     * Returns true if the application has stabilized. Throws {@link CfnNotStabilizedException} if the application
     * status indicates failure
     */
    protected boolean waitForApplicationToStabilize(final String applicationId,
                                                    final ResourceModel model,
                                                    final ProxyClient<M2Client> proxyClient) {

        final GetApplicationResponse getApplicationResponse =
                apiWrapper.getApplication(
                        applicationId != null ?
                                GetApplicationRequest.builder().applicationId(applicationId).build()
                                : Translator.toGetApplicationRequest(model.getApplicationArn()), proxyClient);
        model.setApplicationArn(getApplicationResponse.applicationArn());
        final ApplicationLifecycle applicationStatus = getApplicationResponse.status();
        ApplicationVersionLifecycle latestVersionStatus = getApplicationResponse.latestVersion().status();
        switch (applicationStatus) {
            case CREATING:
            case DELETING:
                // still in-progress
                return false;
            case CREATED:
            case READY:
            case STOPPED:
            case AVAILABLE:
                if (ApplicationVersionLifecycle.CREATING.equals(latestVersionStatus)) {
                    // update application: a new version is being created
                    return false;
                }
                // stabilized
                logger.log(String.format("%s [%s] has been stabilized.", ResourceModel.TYPE_NAME, applicationId));
                return true;
            case FAILED:
                String message = String.format("Error stabilizing resource '%s' with id '%s. Reason - %s",
                        ResourceModel.TYPE_NAME, applicationId, getApplicationResponse.statusReason());
                logger.log(message);
                throw new CfnGeneralServiceException(message);
            default:
                logger.log(String.format("Error stabilizing resource '%s' with id '%s. Unexpected application status - %s",
                        ResourceModel.TYPE_NAME, applicationId, applicationStatus));
                throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, applicationId);
        }
    }

    protected String getCallGraphFormat(String api) {
        Validate.notBlank(api);
        return String.format(CALL_GRAPH_FORMAT, api);
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<M2Client> proxyClient);
}

