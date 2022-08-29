package software.amazon.m2.environment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.awssdk.services.m2.model.AccessDeniedException;
import software.amazon.awssdk.services.m2.model.ConflictException;
import software.amazon.awssdk.services.m2.model.CreateEnvironmentRequest;
import software.amazon.awssdk.services.m2.model.CreateEnvironmentResponse;
import software.amazon.awssdk.services.m2.model.GetEnvironmentRequest;
import software.amazon.awssdk.services.m2.model.InternalServerException;
import software.amazon.awssdk.services.m2.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.m2.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.m2.model.M2Exception;
import software.amazon.awssdk.services.m2.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.m2.model.TagResourceRequest;
import software.amazon.awssdk.services.m2.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<M2Client> proxyM2Client;

    @Mock
    private M2Client m2Client;

    final CreateHandler handler = new CreateHandler();

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        m2Client = Mockito.mock(M2Client.class);
        proxyM2Client = MOCK_PROXY(proxy, m2Client);
    }

    @AfterEach
    public void tear_down() {
        Mockito.verify(m2Client, Mockito.atLeastOnce()).serviceName();
    }

    @Test
    public void handleRequest_success_onlyRequiredFields() {

        final ResourceModel model = ResourceModel.builder()
                .name("env-name")
                .instanceType("m2.m5.large")
                .engineType("microfocus")
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .clientRequestToken("client-token")
                .build();

        String envId = "env-id";
        mockCreateEnvironment(envId);
        Mockito.when(proxyM2Client.client().getEnvironment(Mockito.any(GetEnvironmentRequest.class)))
                .thenReturn(TestDataProvider.getEnvironmentResponseFromModel(model, envId, "Available"))  // wait call
                .thenReturn(TestDataProvider.getEnvironmentResponseFromModel(model, envId, "Available")); // final read call

        Mockito.when(proxyM2Client.client().listTagsForResource(Mockito.any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyM2Client, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        Mockito.verify(m2Client, Mockito.times(1)).createEnvironment(Mockito.any(CreateEnvironmentRequest.class));
        Mockito.verify(m2Client, Mockito.times(2)).getEnvironment(Mockito.any(GetEnvironmentRequest.class));
        Mockito.verify(m2Client, Mockito.times(0)).tagResource(Mockito.any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_success_allFields() {

        final ResourceModel model = TestDataProvider.resourceModel();
        final Map<String, String> tags = model.getTags();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .clientRequestToken("client-token")
                .systemTags(Map.of("aws:cloudformation:stack-id", "12345678"))
                .desiredResourceTags(tags)
                .build();

        String envId = "env-id";
        mockCreateEnvironment(envId);
        Mockito.when(proxyM2Client.client().getEnvironment(Mockito.any(GetEnvironmentRequest.class)))
                .thenReturn(TestDataProvider.getEnvironmentResponseFromModel(model, envId, "Creating"))  // wait call
                .thenReturn(TestDataProvider.getEnvironmentResponseFromModel(model, envId, "Available"))  // wait call
                .thenReturn(TestDataProvider.getEnvironmentResponseFromModel(model, envId, "Available")); // final read call

        Mockito.when(proxyM2Client.client().listTagsForResource(Mockito.any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .tags(tags).build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyM2Client, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        Mockito.verify(m2Client, Mockito.times(1)).createEnvironment(Mockito.any(CreateEnvironmentRequest.class));
        Mockito.verify(m2Client, Mockito.times(3)).getEnvironment(Mockito.any(GetEnvironmentRequest.class));
        Mockito.verify(m2Client, Mockito.times(0)).tagResource(Mockito.any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_createFails() {
        final ResourceModel model = ResourceModel.builder()
                .name("env-name")
                .instanceType("m2.m5.large")
                .engineType("microfocus")
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .clientRequestToken("client-token")
                .build();

        String envId = "env-id";
        mockCreateEnvironment(envId);
        Mockito.when(proxyM2Client.client().getEnvironment(Mockito.any(GetEnvironmentRequest.class)))
                .thenReturn(TestDataProvider.getEnvironmentResponseFromModel(model, envId, "Failed")); // read call

        Assertions.assertThrows(software.amazon.cloudformation.exceptions.CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyM2Client, logger));

        Mockito.verify(proxyM2Client.client(), Mockito.times(0)).listTagsForResource(Mockito.any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_envAlreadyExists() {

        final ResourceHandlerRequest<ResourceModel> request = getResourceHandlerRequest();

        Mockito.when(m2Client.createEnvironment(Mockito.any(CreateEnvironmentRequest.class)))
                .thenThrow(ConflictException.builder().message("already exists").build());

        Assertions.assertThrows(software.amazon.cloudformation.exceptions.CfnAlreadyExistsException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyM2Client, logger));
    }

    @Test
    public void handleRequest_accessDenied() {

        final ResourceHandlerRequest<ResourceModel> request = getResourceHandlerRequest();

        Mockito.when(m2Client.createEnvironment(Mockito.any(CreateEnvironmentRequest.class)))
                .thenThrow(AccessDeniedException.builder().message("not allowed").build());

        Assertions.assertThrows(CfnAccessDeniedException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyM2Client, logger));
    }

    @Test
    public void handleRequest_quotaExceeded() {

        final ResourceHandlerRequest<ResourceModel> request = getResourceHandlerRequest();

        Mockito.when(m2Client.createEnvironment(Mockito.any(CreateEnvironmentRequest.class)))
                .thenThrow(ServiceQuotaExceededException.builder().message("too much").build());

        Assertions.assertThrows(software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyM2Client, logger));
    }

    @Test
    public void handleRequest_validationException() {

        final ResourceHandlerRequest<ResourceModel> request = getResourceHandlerRequest();

        Mockito.when(m2Client.createEnvironment(Mockito.any(CreateEnvironmentRequest.class)))
                .thenThrow(ValidationException.builder().message("invalid").build());

        Assertions.assertThrows(software.amazon.cloudformation.exceptions.CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyM2Client, logger));
    }

    @Test
    public void handleRequest_internalError() {

        final ResourceHandlerRequest<ResourceModel> request = getResourceHandlerRequest();

        Mockito.when(m2Client.createEnvironment(Mockito.any(CreateEnvironmentRequest.class)))
                .thenThrow(InternalServerException.builder().message("too much").build());

        Assertions.assertThrows(software.amazon.cloudformation.exceptions.CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyM2Client, logger));
    }

    @Test
    public void handleRequest_generalException() {

        final ResourceHandlerRequest<ResourceModel> request = getResourceHandlerRequest();

        Mockito.when(m2Client.createEnvironment(Mockito.any(CreateEnvironmentRequest.class)))
                .thenThrow(M2Exception.builder().message("too much").build());

        Assertions.assertThrows(software.amazon.cloudformation.exceptions.CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyM2Client, logger));
    }

    private ResourceHandlerRequest<ResourceModel> getResourceHandlerRequest() {
        final ResourceModel model = ResourceModel.builder()
                .name("env-name")
                .instanceType("m2.m5.large")
                .engineType("microfocus")
                .build();

        return ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .clientRequestToken("client-token")
                .build();
    }

    private void mockCreateEnvironment(String envId) {
        CreateEnvironmentResponse createEnvironmentResponse = CreateEnvironmentResponse.builder()
                .environmentId(envId).build();
        Mockito.when(proxyM2Client.client().createEnvironment(Mockito.any(CreateEnvironmentRequest.class)))
                .thenReturn(createEnvironmentResponse);
    }
}