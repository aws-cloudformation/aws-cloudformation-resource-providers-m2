package software.amazon.m2.environment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.awssdk.services.m2.model.FsxStorageConfiguration;
import software.amazon.awssdk.services.m2.model.GetEnvironmentRequest;
import software.amazon.awssdk.services.m2.model.GetEnvironmentResponse;
import software.amazon.awssdk.services.m2.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.m2.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.m2.model.MaintenanceSchedule;
import software.amazon.awssdk.services.m2.model.PendingMaintenance;
import software.amazon.awssdk.services.m2.model.ResourceNotFoundException;
import software.amazon.awssdk.services.m2.model.StorageConfiguration;
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
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<M2Client> proxyClient;

    @Mock
    private M2Client m2Client;

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
    public void handleRequest_SimpleSuccess() {
        final ReadHandler handler = new ReadHandler();
        Instant startMaintenance = Instant.ofEpochMilli(1657129893277L);
        Instant endMaintenance = Instant.ofEpochMilli(1657159893277L);

        final ResourceModel model = ResourceModel.builder()
                .environmentArn("arn:aws:m2:us-west-2:123456:env/env-id")
                .name("for-test").engineType("bluage")
                .engineVersion("1.2.3").publiclyAccessible(true)
                .subnetIds(Collections.singletonList("subnet-12"))
                .securityGroupIds(Collections.singletonList("sg-id-1"))
                .storageConfigurations(Collections.singletonList(
                        software.amazon.m2.environment.StorageConfiguration.builder().fsx(
                                        software.amazon.m2.environment.FsxStorageConfiguration.builder()
                                                .fileSystemId("fs-123456").mountPoint("mount/fsx/")
                                                .build())
                                .build()))
                .tags(Map.of("k1", "v1"))
                .build();

        GetEnvironmentResponse getEnvironmentResponse = GetEnvironmentResponse.builder()
                .environmentArn(model.getEnvironmentArn())
                .name(model.getName())
                .engineType(model.getEngineType()).engineVersion(model.getEngineVersion())
                .instanceType(model.getInstanceType()).publiclyAccessible(model.getPubliclyAccessible())
                .vpcId("vpc-1")
                .subnetIds(model.getSubnetIds())
                .securityGroupIds(Collections.singletonList("sg-id-1"))
                .storageConfigurations(StorageConfiguration.builder().fsx(
                                FsxStorageConfiguration.builder().fileSystemId("fs-123456").mountPoint("mount/fsx/").build())
                        .build())
                .pendingMaintenance(PendingMaintenance.builder()
                        .schedule(MaintenanceSchedule.builder()
                                .startTime(startMaintenance)
                                .endTime(endMaintenance)
                                .build())
                        .build())
                .loadBalancerArn("https://destination/location")
                .build();
        Mockito.when(proxyClient.client().getEnvironment(Mockito.any(GetEnvironmentRequest.class)))
                .thenReturn(getEnvironmentResponse);

        ListTagsForResourceResponse listTagsResponse = ListTagsForResourceResponse.builder()
                .tags(Map.of("k1", "v1")).build();
        Mockito.when(proxyClient.client().listTagsForResource(Mockito.any(ListTagsForResourceRequest.class)))
                .thenReturn(listTagsResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(
                proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_envNotFound() {
        final ReadHandler handler = new ReadHandler();

        final ResourceModel model = ResourceModel.builder()
                .environmentArn("arn:aws:m2:us-west-2:123456:env/env-id")
                .name("for-test").engineType("bluage")
                .build();

        Mockito.when(proxyClient.client().getEnvironment(Mockito.any(GetEnvironmentRequest.class)))
                .thenThrow(ResourceNotFoundException.builder()
                        .resourceType("Environment").resourceId("arn:aws:m2:us-west-2:123456:env/env-id").build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        Assertions.assertThrows(CfnNotFoundException.class, () -> {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        });
    }
}
