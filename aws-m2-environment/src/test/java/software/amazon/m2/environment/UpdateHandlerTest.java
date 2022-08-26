package software.amazon.m2.environment;

import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.awssdk.services.m2.model.EnvironmentLifecycle;
import software.amazon.awssdk.services.m2.model.GetEnvironmentRequest;
import software.amazon.awssdk.services.m2.model.GetEnvironmentResponse;
import software.amazon.awssdk.services.m2.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.m2.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.m2.model.ResourceNotFoundException;
import software.amazon.awssdk.services.m2.model.TagResourceRequest;
import software.amazon.awssdk.services.m2.model.TagResourceResponse;
import software.amazon.awssdk.services.m2.model.UntagResourceRequest;
import software.amazon.awssdk.services.m2.model.UntagResourceResponse;
import software.amazon.awssdk.services.m2.model.UpdateEnvironmentRequest;
import software.amazon.awssdk.services.m2.model.UpdateEnvironmentResponse;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<M2Client> proxyClient;

    @Mock
    M2Client m2Client;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        m2Client = Mockito.mock(M2Client.class);
        proxyClient = MOCK_PROXY(proxy, m2Client);
    }

    @Test
    public void handleRequest_success_updateTags() {
        final UpdateHandler handler = new UpdateHandler();
        final Map<String, String> oldTags = Map.of("tag1", "value1", "tag2", "value2");
        final Map<String, String> newTags = Map.of("tag1", "value1", "tag3", "value3");

        // the resource we want
        final ResourceModel model = ResourceModel.builder()
                .environmentId("env-id")
                .environmentArn("arn:aws:m2:us-west-2:123456:env/env-id")
                .instanceType("M2.m5.large")
                .engineType("microfocus")
                .engineVersion("new-version")
                .preferredMaintenanceWindow("sat:07:00-sat:09:00")
                .highAvailabilityConfig(HighAvailabilityConfig.builder().desiredCapacity(1).build())
                .tags(newTags)
                .build();

        UpdateEnvironmentResponse updateEnvironmentResponse = UpdateEnvironmentResponse.builder()
                .environmentId(model.getEnvironmentId())
                .build();
        Mockito.when(proxyClient.client().updateEnvironment(Mockito.any(UpdateEnvironmentRequest.class)))
                .thenReturn(updateEnvironmentResponse);

        GetEnvironmentResponse getEnvironmentResponseExisting = GetEnvironmentResponse.builder()
                .environmentId("env-id")
                .environmentArn("arn:aws:m2:us-west-2:123456:env/env-id")
                .name(model.getName())
                .engineType(model.getEngineType())
                .engineVersion("old-version")
                .preferredMaintenanceWindow("sat:07:00-sat:09:00")
                .highAvailabilityConfig(software.amazon.awssdk.services.m2.model.HighAvailabilityConfig.builder()
                        .desiredCapacity(2).build())
                .instanceType(model.getInstanceType())
                .status(EnvironmentLifecycle.CREATING)
                .tags(oldTags)
                .build();
        GetEnvironmentResponse getEnvironmentResponseAvailable = GetEnvironmentResponse.builder()
                .environmentId("env-id")
                .environmentArn("arn:aws:m2:us-west-2:123456:env/env-id")
                .name(model.getName())
                .engineType(model.getEngineType())
                .preferredMaintenanceWindow("sat:07:00-sat:09:00")
                .highAvailabilityConfig(software.amazon.awssdk.services.m2.model.HighAvailabilityConfig.builder()
                        .desiredCapacity(1).build())
                .engineVersion(model.getEngineVersion())
                .instanceType(model.getInstanceType())
                .status(EnvironmentLifecycle.AVAILABLE)
                .tags(newTags)
                .build();
        Mockito.when(proxyClient.client().getEnvironment(Mockito.any(GetEnvironmentRequest.class)))
                .thenReturn(getEnvironmentResponseExisting) // first wait call
                .thenReturn(getEnvironmentResponseAvailable) // second wait call
                .thenReturn(getEnvironmentResponseAvailable); // final read call

        Mockito.when(proxyClient.client().listTagsForResource(Mockito.any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().tags(newTags).build());
        Mockito.when(proxyClient.client().tagResource(Mockito.any(TagResourceRequest.class)))
                .thenReturn(TagResourceResponse.builder().build());
        Mockito.when(proxyClient.client().untagResource(Mockito.any(UntagResourceRequest.class)))
                .thenReturn(UntagResourceResponse.builder().build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .previousResourceState(model)
                .previousResourceTags(oldTags)
                .desiredResourceTags(newTags)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(
                proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();

        Mockito.verify(m2Client, Mockito.times(1)).updateEnvironment(Mockito.any(UpdateEnvironmentRequest.class));
        Mockito.verify(m2Client, Mockito.times(3)).getEnvironment(Mockito.any(GetEnvironmentRequest.class));
        Mockito.verify(m2Client, Mockito.times(1)).untagResource(Mockito.any(UntagResourceRequest.class));
        Mockito.verify(m2Client, Mockito.times(1)).tagResource(Mockito.any(TagResourceRequest.class));
        Mockito.verify(m2Client, Mockito.times(1)).listTagsForResource(Mockito.any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_success_sameTags() {
        final UpdateHandler handler = new UpdateHandler();
        final Map<String, String> oldTags = Map.of("tag1", "value1", "tag2", "value2");

        // the resource we want
        final ResourceModel model = ResourceModel.builder()
                .environmentId("env-id")
                .environmentArn("arn:aws:m2:us-west-2:123456:env/env-id")
                .instanceType("m2.m5.large")
                .engineType("microfocus")
                .engineVersion("new-version")
                .preferredMaintenanceWindow("sat:07:00-sat:09:00")
                .highAvailabilityConfig(HighAvailabilityConfig.builder().desiredCapacity(1).build())
                .tags(oldTags) // keep the old tags, no chnage
                .build();

        UpdateEnvironmentResponse updateEnvironmentResponse = UpdateEnvironmentResponse.builder()
                .environmentId(model.getEnvironmentId())
                .build();
        Mockito.when(proxyClient.client().updateEnvironment(Mockito.any(UpdateEnvironmentRequest.class)))
                .thenReturn(updateEnvironmentResponse);

        GetEnvironmentResponse getEnvironmentResponseExisting = GetEnvironmentResponse.builder()
                .environmentId("env-id")
                .environmentArn("arn:aws:m2:us-west-2:123456:env/env-id")
                .name(model.getName())
                .engineType(model.getEngineType())
                .engineVersion("old-version")
                .preferredMaintenanceWindow("sat:07:00-sat:09:00")
                .highAvailabilityConfig(software.amazon.awssdk.services.m2.model.HighAvailabilityConfig.builder()
                        .desiredCapacity(2).build())
                .instanceType(model.getInstanceType())
                .status(EnvironmentLifecycle.CREATING)
                .tags(oldTags)
                .build();
        GetEnvironmentResponse getEnvironmentResponseAvailable = GetEnvironmentResponse.builder()
                .environmentId("env-id")
                .environmentArn("arn:aws:m2:us-west-2:123456:env/env-id")
                .name(model.getName())
                .engineType(model.getEngineType())
                .preferredMaintenanceWindow("sat:07:00-sat:09:00")
                .highAvailabilityConfig(software.amazon.awssdk.services.m2.model.HighAvailabilityConfig.builder()
                        .desiredCapacity(1).build())
                .engineVersion(model.getEngineVersion())
                .instanceType(model.getInstanceType())
                .status(EnvironmentLifecycle.AVAILABLE)
                .tags(oldTags)
                .build();
        Mockito.when(proxyClient.client().getEnvironment(Mockito.any(GetEnvironmentRequest.class)))
                .thenReturn(getEnvironmentResponseExisting) // first wait call
                .thenReturn(getEnvironmentResponseAvailable) // second wait call
                .thenReturn(getEnvironmentResponseAvailable); // final read call

        Mockito.when(proxyClient.client().listTagsForResource(Mockito.any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .tags(oldTags)
                        .build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .previousResourceState(model)
                .previousResourceTags(oldTags)
                .desiredResourceTags(oldTags)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(
                proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();

        Mockito.verify(m2Client, Mockito.times(1)).updateEnvironment(Mockito.any(UpdateEnvironmentRequest.class));
        Mockito.verify(m2Client, Mockito.times(3)).getEnvironment(Mockito.any(GetEnvironmentRequest.class));
        Mockito.verify(m2Client, Mockito.times(0)).untagResource(Mockito.any(UntagResourceRequest.class));
        Mockito.verify(m2Client, Mockito.times(0)).tagResource(Mockito.any(TagResourceRequest.class));
        Mockito.verify(m2Client, Mockito.times(1)).listTagsForResource(Mockito.any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_noTags() {
        final UpdateHandler handler = new UpdateHandler();

        // the resource we want does not have tags
        final ResourceModel model = ResourceModel.builder()
                .environmentId("env-id")
                .environmentArn("arn:aws:m2:us-west-2:123456:env/env-id")
                .instanceType("m2.m5.large")
                .engineType("microfocus")
                .engineVersion("new-version")
                .preferredMaintenanceWindow("sat:07:00-sat:09:00")
                .highAvailabilityConfig(HighAvailabilityConfig.builder().desiredCapacity(1).build())
                .build();

        UpdateEnvironmentResponse updateEnvironmentResponse = UpdateEnvironmentResponse.builder()
                .environmentId(model.getEnvironmentId())
                .build();
        Mockito.when(proxyClient.client().updateEnvironment(Mockito.any(UpdateEnvironmentRequest.class)))
                .thenReturn(updateEnvironmentResponse);

        // existing resource does not have tags
        GetEnvironmentResponse getEnvironmentResponseExisting = GetEnvironmentResponse.builder()
                .environmentId("env-id")
                .environmentArn("arn:aws:m2:us-west-2:123456:env/env-id")
                .name(model.getName())
                .engineType(model.getEngineType())
                .engineVersion("old-version")
                .preferredMaintenanceWindow("sat:07:00-sat:09:00")
                .highAvailabilityConfig(software.amazon.awssdk.services.m2.model.HighAvailabilityConfig.builder()
                        .desiredCapacity(2).build())
                .instanceType(model.getInstanceType())
                .status(EnvironmentLifecycle.CREATING)
                .build();
        // updated resource
        GetEnvironmentResponse getEnvironmentResponseUpdated = GetEnvironmentResponse.builder()
                .environmentId("env-id")
                .environmentArn("arn:aws:m2:us-west-2:123456:env/env-id")
                .name(model.getName())
                .engineType(model.getEngineType())
                .preferredMaintenanceWindow("sat:07:00-sat:09:00")
                .highAvailabilityConfig(software.amazon.awssdk.services.m2.model.HighAvailabilityConfig.builder()
                        .desiredCapacity(1).build())
                .engineVersion(model.getEngineVersion())
                .instanceType(model.getInstanceType())
                .status(EnvironmentLifecycle.AVAILABLE)
                .build();
        Mockito.when(proxyClient.client().getEnvironment(Mockito.any(GetEnvironmentRequest.class)))
                .thenReturn(getEnvironmentResponseExisting) // first wait call
                .thenReturn(getEnvironmentResponseUpdated) // second wait call
                .thenReturn(getEnvironmentResponseUpdated); // final read call

        Mockito.when(proxyClient.client().listTagsForResource(Mockito.any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .previousResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(
                proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();

        Mockito.verify(m2Client, Mockito.times(1)).updateEnvironment(Mockito.any(UpdateEnvironmentRequest.class));
        Mockito.verify(m2Client, Mockito.times(3)).getEnvironment(Mockito.any(GetEnvironmentRequest.class));
        Mockito.verify(m2Client, Mockito.times(0)).untagResource(Mockito.any(UntagResourceRequest.class));
        Mockito.verify(m2Client, Mockito.times(0)).tagResource(Mockito.any(TagResourceRequest.class));
        Mockito.verify(m2Client, Mockito.times(1)).listTagsForResource(Mockito.any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_updateFails() {
        final UpdateHandler handler = new UpdateHandler();
        final ResourceModel model = ResourceModel.builder()
                .environmentId("env-id")
                .environmentArn("arn:aws:m2:us-west-2:123456:env/env-id")
                .instanceType("m2.m5.large")
                .engineType("microfocus")
                .engineVersion("new-version")
                .preferredMaintenanceWindow("sat:07:00-sat:09:00")
                .highAvailabilityConfig(HighAvailabilityConfig.builder().desiredCapacity(1).build())
                .build();

        UpdateEnvironmentResponse updateEnvironmentResponse = UpdateEnvironmentResponse.builder()
                .environmentId(model.getEnvironmentId())
                .build();
        Mockito.when(proxyClient.client().updateEnvironment(Mockito.any(UpdateEnvironmentRequest.class)))
                .thenReturn(updateEnvironmentResponse);

        GetEnvironmentResponse getEnvironmentResponseExisting = GetEnvironmentResponse.builder()
                .environmentId("env-id")
                .environmentArn("arn:aws:m2:us-west-2:123456:env/env-id")
                .name(model.getName())
                .engineType(model.getEngineType())
                .engineVersion("old-version")
                .preferredMaintenanceWindow("sat:07:00-sat:09:00")
                .highAvailabilityConfig(software.amazon.awssdk.services.m2.model.HighAvailabilityConfig.builder()
                        .desiredCapacity(2).build())
                .instanceType(model.getInstanceType())
                .status(EnvironmentLifecycle.CREATING)
                .build();
        // updated resource
        GetEnvironmentResponse getEnvironmentResponseUpdated = GetEnvironmentResponse.builder()
                .environmentId("env-id")
                .environmentArn("arn:aws:m2:us-west-2:123456:env/env-id")
                .name(model.getName())
                .engineType(model.getEngineType())
                .preferredMaintenanceWindow("sat:07:00-sat:09:00")
                .highAvailabilityConfig(software.amazon.awssdk.services.m2.model.HighAvailabilityConfig.builder()
                        .desiredCapacity(2).build())
                .engineVersion(model.getEngineVersion())
                .instanceType(model.getInstanceType())
                .status(EnvironmentLifecycle.FAILED)
                .build();
        Mockito.when(proxyClient.client().getEnvironment(Mockito.any(GetEnvironmentRequest.class)))
                .thenReturn(getEnvironmentResponseExisting) // first wait call
                .thenReturn(getEnvironmentResponseUpdated); // final read call

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .previousResourceState(model)
                .build();

        Assertions.assertThrows(software.amazon.cloudformation.exceptions.CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        Mockito.verify(proxyClient.client(), Mockito.times(0)).listTagsForResource(Mockito.any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_notFound() {
        final UpdateHandler handler = new UpdateHandler();
        final ResourceModel model = ResourceModel.builder()
                .environmentArn("arn:aws:m2:us-west-2:123456:env/env-to-update")
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        Mockito.when(proxyClient.client().updateEnvironment(Mockito.any(UpdateEnvironmentRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().resourceId(model.getEnvironmentId()).build());

        Assertions.assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_invalidArn() {
        final UpdateHandler handler = new UpdateHandler();
        final ResourceModel model = ResourceModel.builder().environmentArn("env-to-update").build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        Assertions.assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        Mockito.verify(proxyClient.client(), Mockito.times(0))
                .updateEnvironment(Mockito.any(UpdateEnvironmentRequest.class));
    }

}
