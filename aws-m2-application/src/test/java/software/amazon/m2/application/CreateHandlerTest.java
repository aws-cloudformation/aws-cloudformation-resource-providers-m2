package software.amazon.m2.application;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.m2.model.ApplicationLifecycle;
import software.amazon.awssdk.services.m2.model.ConflictException;
import software.amazon.awssdk.services.m2.model.CreateApplicationRequest;
import software.amazon.awssdk.services.m2.model.CreateApplicationResponse;
import software.amazon.awssdk.services.m2.model.GetApplicationRequest;
import software.amazon.awssdk.services.m2.model.GetApplicationResponse;
import software.amazon.awssdk.services.m2.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.m2.model.ListTagsForResourceResponse;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.m2.common.AbstractTestBase;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    private ApiWrapper apiWrapper;

    private CreateHandler createHandler;

    @BeforeEach
    public void setup() {
        createHandler = new CreateHandler(apiWrapper);
    }

    @Test
    public void handleCreatedRequest_Success() {
        final ImmutableMap<String, String> desiredTags = ImmutableMap.of("Key1", "Value1", "Key2", "Value2");

        final ResourceModel model = ResourceModel.builder()
                .name("app-name")
                .engineType("microfocus")
                .definition(Definition.builder().s3Location("s3://bucket/location").build())
                .description("app description")
                .tags(desiredTags)
                .build();
        CreateApplicationResponse createApplicationResponse = CreateApplicationResponse.builder()
                .applicationId(model.getApplicationId())
                .build();

        Mockito.when(apiWrapper.createApplication(Mockito.any(CreateApplicationRequest.class), Mockito.any()))
                .thenReturn(createApplicationResponse);

        String appArn = "arn:aws:m2:us-west-2:123456:app/app-id";
        final GetApplicationResponse getApplicationResponse = GetApplicationResponse.builder()
                .applicationId("app-id")
                .applicationArn(appArn)
                .name(model.getName())
                .engineType(model.getEngineType())
                .status(ApplicationLifecycle.AVAILABLE)
                .description("app description")
                .tags(desiredTags)
                .build();
        Mockito.when(apiWrapper.getApplication(Mockito.any(GetApplicationRequest.class), Mockito.any()))
                .thenReturn(getApplicationResponse);

        final ImmutableMap<String, String> tags = ImmutableMap.of("Key1", "Value1", "Key2", "Value2");
        Mockito.when(apiWrapper.listTags(Mockito.any(ListTagsForResourceRequest.class), Mockito.any()))
                .thenReturn(ListTagsForResourceResponse.builder().tags(tags).build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .clientRequestToken("client-token")
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = createHandler.handleRequest(proxy, request, new CallbackContext(), logger);

        assertNotNull(response);
        assertEquals(OperationStatus.SUCCESS, response.getStatus());
        assertEquals(0, response.getCallbackDelaySeconds());
        assertNull(response.getErrorCode());

        final ResourceModel actualModel = response.getResourceModel();
        assertNotNull(actualModel);
        assertEquals("app-id", actualModel.getApplicationId());
        assertEquals(appArn, actualModel.getApplicationArn());
        assertEquals(tags, actualModel.getTags());

        verify(apiWrapper).createApplication(Mockito.any(CreateApplicationRequest.class), Mockito.any());
        verify(apiWrapper, times(2)).getApplication(Mockito.any(GetApplicationRequest.class), Mockito.any());
        verify(apiWrapper).listTags(Mockito.any(ListTagsForResourceRequest.class), Mockito.any());
    }

    @Test
    public void handleCreatedRequest_WaitUntilStabilized() {
        final ResourceModel model = ResourceModel.builder()
                .name("app-name")
                .engineType("microfocus")
                .definition(Definition.builder().s3Location("s3://bucket/location").content("dummyContext").build())
                .build();
        CreateApplicationResponse createApplicationResponse = CreateApplicationResponse.builder()
                .applicationId("app-id")
                .build();

        Mockito.when(apiWrapper.createApplication(Mockito.any(CreateApplicationRequest.class), Mockito.any()))
                .thenReturn(createApplicationResponse);

        final GetApplicationResponse creatingResponse = GetApplicationResponse.builder()
                .applicationId("app-id")
                .applicationArn("arn:aws:m2:us-west-2:123456:app/app-id")
                .name(model.getName())
                .status(ApplicationLifecycle.CREATING)
                .build();
        final GetApplicationResponse createdResponse = GetApplicationResponse.builder()
                .applicationId("app-id")
                .applicationArn("arn:aws:m2:us-west-2:123456:app/app-id")
                .status(ApplicationLifecycle.AVAILABLE)
                .build();

        Mockito.when(apiWrapper.getApplication(Mockito.any(GetApplicationRequest.class), Mockito.any()))
                .thenReturn(creatingResponse)
                .thenReturn(creatingResponse)
                .thenReturn(createdResponse);

        final ImmutableMap<String, String> tags = ImmutableMap.of("Key1", "Value1", "Key2", "Value2");
        Mockito.when(apiWrapper.listTags(Mockito.any(ListTagsForResourceRequest.class), Mockito.any()))
                .thenReturn(ListTagsForResourceResponse.builder().tags(tags).build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .desiredResourceTags(Map.of("k1", "v1"))
                .clientRequestToken("client-token")
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = createHandler.handleRequest(proxy, request, new CallbackContext(), logger);

        assertNotNull(response);
        assertEquals(OperationStatus.SUCCESS, response.getStatus());
        assertEquals(0, response.getCallbackDelaySeconds());
        assertNull(response.getErrorCode());

        final ResourceModel actualModel = response.getResourceModel();
        assertNotNull(actualModel);
        assertEquals("app-id", actualModel.getApplicationId());
        assertEquals(createdResponse.applicationArn(), actualModel.getApplicationArn());
        assertEquals(tags, actualModel.getTags());

        verify(apiWrapper).createApplication(Mockito.any(CreateApplicationRequest.class), Mockito.any());
        verify(apiWrapper, times(4)).getApplication(Mockito.any(GetApplicationRequest.class), Mockito.any());
        verify(apiWrapper).listTags(Mockito.any(ListTagsForResourceRequest.class), Mockito.any());
    }

    @Test
    public void handleCreatedRequest_ApplicationAlreadyExists() {
        final ResourceModel model = ResourceModel.builder()
                .name("app-name")
                .engineType("microfocus")
                .definition(Definition.builder().s3Location("s3://bucket/location").content("dummyContext").build())
                .build();

        Mockito.when(apiWrapper.createApplication(Mockito.any(CreateApplicationRequest.class), Mockito.any()))
                .thenThrow(new CfnAlreadyExistsException(ConflictException.builder().build()));

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .clientRequestToken("client-token")
                .build();
        assertThrows(CfnAlreadyExistsException.class,
                () -> createHandler.handleRequest(proxy, request, new CallbackContext(), logger));

        verify(apiWrapper, times(1))
                .createApplication(Mockito.any(CreateApplicationRequest.class), Mockito.any());
    }
}
