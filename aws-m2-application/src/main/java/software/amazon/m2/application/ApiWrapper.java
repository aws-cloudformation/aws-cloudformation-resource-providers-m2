package software.amazon.m2.application;

import org.apache.commons.lang3.Validate;
import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.awssdk.services.m2.model.CreateApplicationRequest;
import software.amazon.awssdk.services.m2.model.CreateApplicationResponse;
import software.amazon.awssdk.services.m2.model.DeleteApplicationRequest;
import software.amazon.awssdk.services.m2.model.DeleteApplicationResponse;
import software.amazon.awssdk.services.m2.model.GetApplicationRequest;
import software.amazon.awssdk.services.m2.model.GetApplicationResponse;
import software.amazon.awssdk.services.m2.model.ListApplicationsRequest;
import software.amazon.awssdk.services.m2.model.ListApplicationsResponse;
import software.amazon.awssdk.services.m2.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.m2.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.m2.model.TagResourceRequest;
import software.amazon.awssdk.services.m2.model.TagResourceResponse;
import software.amazon.awssdk.services.m2.model.UntagResourceRequest;
import software.amazon.awssdk.services.m2.model.UntagResourceResponse;
import software.amazon.awssdk.services.m2.model.UpdateApplicationRequest;
import software.amazon.awssdk.services.m2.model.UpdateApplicationResponse;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.m2.common.ExceptionHandlerWrapper;

/**
 * Wrapper over M2 SDK to make API calls and wrap M2 exceptions to appropriate CFN exceptions
 */
public class ApiWrapper {

    private static final String CREATE_APPLICATION_API_NAME = "CreateApplication";
    private static final String GET_APPLICATION_API_NAME = "GetApplication";
    private static final String UPDATE_APPLICATION_API_NAME = "UpdateApplication";
    private static final String DELETE_APPLICATION_API_NAME = "DeleteApplication";
    private static final String LIST_APPLICATIONS_API_NAME = "ListApplications";
    private static final String UNTAG_RESOURCE_API_NAME = "UntagResource";
    private static final String TAG_RESOURCE_API_NAME = "TagResource";
    private static final String LIST_TAGS_API_NAME = "ListTagsForResource";

    private Logger logger;

    void setLogger(Logger logger) {
        this.logger = logger;
    }

    CreateApplicationResponse createApplication(CreateApplicationRequest request, ProxyClient<M2Client> proxyClient) {
        Validate.notNull(request);
        Validate.notNull(proxyClient);
        try (M2Client m2client = proxyClient.client()) {
            logger.log(String.format("Calling %s API with application name - %s",
                CREATE_APPLICATION_API_NAME, request.name()));
            return ExceptionHandlerWrapper.wrapM2Exception(CREATE_APPLICATION_API_NAME,
                () -> proxyClient.injectCredentialsAndInvokeV2(request, m2client::createApplication));
        }
    }

    GetApplicationResponse getApplication(GetApplicationRequest request, ProxyClient<M2Client> proxyClient) {
        Validate.notNull(request);
        Validate.notNull(proxyClient);
        try (M2Client m2client = proxyClient.client()) {
            logger.log(String.format("Calling %s API with application id - %s",
                GET_APPLICATION_API_NAME, request.applicationId()));
            return ExceptionHandlerWrapper.wrapM2Exception(GET_APPLICATION_API_NAME,
                () -> proxyClient.injectCredentialsAndInvokeV2(request, m2client::getApplication));
        }
    }

    UpdateApplicationResponse updateApplication(UpdateApplicationRequest request, ProxyClient<M2Client> proxyClient) {
        Validate.notNull(request);
        Validate.notNull(proxyClient);
        try (M2Client m2client = proxyClient.client()) {
            logger.log(String.format("Calling %s API with application id - %s",
                UPDATE_APPLICATION_API_NAME, request.applicationId()));
            return ExceptionHandlerWrapper.wrapM2Exception(UPDATE_APPLICATION_API_NAME,
                () -> proxyClient.injectCredentialsAndInvokeV2(request, m2client::updateApplication));
        }
    }

    DeleteApplicationResponse deleteApplication(DeleteApplicationRequest request,
                                                ProxyClient<M2Client> proxyClient) {
        Validate.notNull(request);
        Validate.notNull(proxyClient);
        try (M2Client m2client = proxyClient.client()) {
            logger.log(String.format("Calling %s API with application id - %s",
                DELETE_APPLICATION_API_NAME, request.applicationId()));
            return ExceptionHandlerWrapper.wrapM2Exception(DELETE_APPLICATION_API_NAME,
                () -> proxyClient.injectCredentialsAndInvokeV2(request, m2client::deleteApplication));
        }
    }

    ListApplicationsResponse listApplications(ListApplicationsRequest request,
                                              ProxyClient<M2Client> proxyClient) {
        Validate.notNull(request);
        Validate.notNull(proxyClient);
        try (M2Client m2client = proxyClient.client()) {
            logger.log(String.format("Calling %s API", LIST_APPLICATIONS_API_NAME));
            return ExceptionHandlerWrapper.wrapM2Exception(LIST_APPLICATIONS_API_NAME,
                () -> proxyClient.injectCredentialsAndInvokeV2(request, m2client::listApplications));
        }
    }

    UntagResourceResponse untagResource(UntagResourceRequest request, ProxyClient<M2Client> proxyClient) {
        Validate.notNull(request);
        Validate.notNull(proxyClient);
        try (M2Client m2client = proxyClient.client()) {
            logger.log(String.format("Calling %s API with application arn - %s",
                UNTAG_RESOURCE_API_NAME, request.resourceArn()));
            return ExceptionHandlerWrapper.wrapM2Exception(UNTAG_RESOURCE_API_NAME,
                () -> proxyClient.injectCredentialsAndInvokeV2(request, m2client::untagResource));
        }
    }

    TagResourceResponse tagResource(TagResourceRequest request, ProxyClient<M2Client> proxyClient) {
        Validate.notNull(request);
        Validate.notNull(proxyClient);
        try (M2Client m2client = proxyClient.client()) {
            logger.log(String.format("Calling %s API with application arn - %s",
                TAG_RESOURCE_API_NAME, request.resourceArn()));
            return ExceptionHandlerWrapper.wrapM2Exception(TAG_RESOURCE_API_NAME,
                () -> proxyClient.injectCredentialsAndInvokeV2(request, m2client::tagResource));
        }
    }

    ListTagsForResourceResponse listTags(ListTagsForResourceRequest request,
                                         ProxyClient<M2Client> proxyClient) {
        Validate.notNull(request);
        Validate.notNull(proxyClient);
        try (M2Client m2client = proxyClient.client()) {
            logger.log(String.format("Calling %s API with application arn - %s",
                LIST_TAGS_API_NAME, request.resourceArn()));
            return ExceptionHandlerWrapper.wrapM2Exception(LIST_TAGS_API_NAME,
                () -> proxyClient.injectCredentialsAndInvokeV2(request, m2client::listTagsForResource));
        }
    }
}
