package software.amazon.m2.environment;

import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;
import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.awssdk.services.m2.model.EnvironmentSummary;
import software.amazon.awssdk.services.m2.model.ListEnvironmentsRequest;
import software.amazon.awssdk.services.m2.model.ListEnvironmentsResponse;
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
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<M2Client> proxyClient;

    @Mock
    private M2Client m2Client;

    private static final String NEXT_TOKEN = "nextToken";

    final ListHandler handler = new ListHandler();

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        m2Client = mock(M2Client.class);
        proxyClient = MOCK_PROXY(proxy, m2Client);
    }

    @AfterEach
    public void tear_down() {
        verify(m2Client, atLeastOnce()).serviceName();
    }

    @Test
    public void handleRequest_SimpleSuccess_withEnvs() {
        final ResourceModel model = ResourceModel.builder()
                .environmentArn("arn:aws:m2:us-west-2:123456:env/env-id")
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        ListEnvironmentsResponse listEnvironmentsResponse = ListEnvironmentsResponse.builder()
                .environments(EnvironmentSummary.builder()
                        .environmentArn("arn:aws:m2:us-west-2:123456:env/env-id")
                        .build())
                .nextToken(NEXT_TOKEN)
                .build();

        Mockito.when(proxyClient.client().listEnvironments(Mockito.any(ListEnvironmentsRequest.class)))
                .thenReturn(listEnvironmentsResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getResourceModels().size()).isEqualTo(1);
        assertThat(response.getNextToken()).isEqualTo(NEXT_TOKEN);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_noEnvs() {

        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        ListEnvironmentsResponse listEnvironmentsResponse = ListEnvironmentsResponse.builder()
                .environments(Collections.emptyList()).build();
        Mockito.when(proxyClient.client().listEnvironments(Mockito.any(ListEnvironmentsRequest.class)))
                .thenReturn(listEnvironmentsResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getResourceModels().size()).isEqualTo(0);
        assertThat(response.getNextToken()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
