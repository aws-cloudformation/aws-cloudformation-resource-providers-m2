package software.amazon.m2.environment;

import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.awssdk.services.m2.model.DeleteEnvironmentRequest;
import software.amazon.awssdk.services.m2.model.DeleteEnvironmentResponse;
import software.amazon.awssdk.services.m2.model.EnvironmentLifecycle;
import software.amazon.awssdk.services.m2.model.GetEnvironmentRequest;
import software.amazon.awssdk.services.m2.model.GetEnvironmentResponse;
import software.amazon.awssdk.services.m2.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<M2Client> proxyClient;

    @Mock
    private M2Client m2Client;

    final DeleteHandler handler = new DeleteHandler();

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        m2Client = mock(M2Client.class);
        proxyClient = MOCK_PROXY(proxy, m2Client);
    }

    @Test
    public void handleRequest_success_withStabilization() {
        final ResourceModel model = ResourceModel.builder()
                .environmentArn("arn:aws:m2:us-west-2:123456:env/env-id")
                .build();

        GetEnvironmentResponse getEnvironmentResponse = GetEnvironmentResponse.builder()
                .environmentArn(model.getEnvironmentArn())
                .status(EnvironmentLifecycle.AVAILABLE)
                .build();
        Mockito.when(proxyClient.client().getEnvironment(Mockito.any(GetEnvironmentRequest.class)))
                .thenReturn(getEnvironmentResponse) // pre-delete check
                .thenReturn(getEnvironmentResponse) // wait for delete check
                .thenThrow(ResourceNotFoundException.builder()
                        .resourceType("Environment").resourceId("arn").build());

        DeleteEnvironmentResponse deleteEnvironmentResponse = DeleteEnvironmentResponse.builder().build();
        Mockito.when(proxyClient.client().deleteEnvironment(Mockito.any(DeleteEnvironmentRequest.class)))
                .thenReturn(deleteEnvironmentResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(
                proxy, request, new CallbackContext(), proxyClient, logger);

        Mockito.verify(proxyClient.client(), Mockito.times(3))
                .getEnvironment(Mockito.any(GetEnvironmentRequest.class));
        Mockito.verify(proxyClient.client(), Mockito.times(1))
                .deleteEnvironment(Mockito.any(DeleteEnvironmentRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getCallbackContext()).isNull();
    }

    @Test
    public void handleRequest_envNotFound() {
        final ResourceModel model = ResourceModel.builder()
                .environmentArn("arn:aws:m2:us-west-2:123456:env/env-id")
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        Mockito.when(proxyClient.client().getEnvironment(Mockito.any(GetEnvironmentRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().resourceId(model.getEnvironmentId()).build());

        Assertions.assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

}
