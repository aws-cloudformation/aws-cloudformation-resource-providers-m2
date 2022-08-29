package software.amazon.m2.application;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.m2.model.ApplicationLifecycle;
import software.amazon.awssdk.services.m2.model.ApplicationVersionSummary;
import software.amazon.awssdk.services.m2.model.ConflictException;
import software.amazon.awssdk.services.m2.model.GetApplicationRequest;
import software.amazon.awssdk.services.m2.model.GetApplicationResponse;
import software.amazon.awssdk.services.m2.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.m2.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.m2.model.TagResourceRequest;
import software.amazon.awssdk.services.m2.model.TagResourceResponse;
import software.amazon.awssdk.services.m2.model.UntagResourceRequest;
import software.amazon.awssdk.services.m2.model.UntagResourceResponse;
import software.amazon.awssdk.services.m2.model.UpdateApplicationRequest;
import software.amazon.awssdk.services.m2.model.UpdateApplicationResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.m2.common.AbstractTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    private ApiWrapper apiWrapper;

    private UpdateHandler handler;

    final String appId = "app-id";
    final String appArn = "arn:aws:m2:us-west-2:123456:app/app-id";

    @BeforeEach
    public void setup() {
        handler = new UpdateHandler(apiWrapper);
    }

    @Test
    public void handleRequest_Success() {
        final ResourceModel model = getResourceModel();
        when(apiWrapper.updateApplication(any(UpdateApplicationRequest.class), Mockito.any()))
                .thenReturn(UpdateApplicationResponse.builder().build());

        final GetApplicationResponse getApplicationResponse = GetApplicationResponse.builder()
                .applicationArn(appArn)
                .applicationId(appId)
                .latestVersion(ApplicationVersionSummary.builder().applicationVersion(1).build())
                .status(ApplicationLifecycle.AVAILABLE)
                .build();
        when(apiWrapper.getApplication(any(GetApplicationRequest.class), Mockito.any()))
                .thenReturn(getApplicationResponse);

        final ImmutableMap<String, String> tags = ImmutableMap.of("Key1", "Value1", "Key2", "Value2");
        when(apiWrapper.listTags(any(ListTagsForResourceRequest.class), Mockito.any()))
                .thenReturn(ListTagsForResourceResponse.builder().tags(tags).build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertNotNull(response);
        assertEquals(OperationStatus.SUCCESS, response.getStatus());
        assertEquals(0, response.getCallbackDelaySeconds());

        final ResourceModel actualModel = response.getResourceModel();
        assertNotNull(actualModel);
        assertEquals(appId, actualModel.getApplicationId());
        assertEquals(appArn, actualModel.getApplicationArn());

        verify(apiWrapper).updateApplication(any(UpdateApplicationRequest.class), Mockito.any());
        verify(apiWrapper, times(3)).getApplication(any(GetApplicationRequest.class), Mockito.any());
        verify(apiWrapper).listTags(any(ListTagsForResourceRequest.class), Mockito.any());
    }

    @Test
    public void handleRequest_AddRemoveTags() {
        final ImmutableMap<String, String> previousTags = ImmutableMap.of("Tag1", "Value1", "Tag2", "Value2");
        final ImmutableMap<String, String> newTags = ImmutableMap.of("Tag1", "Value11", "Tag3", "Value3");

        final ResourceModel model = getResourceModel();
        when(apiWrapper.updateApplication(any(UpdateApplicationRequest.class), Mockito.any()))
                .thenReturn(UpdateApplicationResponse.builder().build());

        final GetApplicationResponse getApplicationResponse = GetApplicationResponse.builder()
                .applicationArn(appArn)
                .applicationId(appId)
                .latestVersion(ApplicationVersionSummary.builder().applicationVersion(1).build())
                .status(ApplicationLifecycle.AVAILABLE)
                .build();
        when(apiWrapper.getApplication(any(GetApplicationRequest.class), Mockito.any()))
                .thenReturn(getApplicationResponse);

        UntagResourceRequest untagRequest = UntagResourceRequest.builder()
                .resourceArn(appArn)
                .tagKeys(ImmutableSet.of("Tag2"))
                .build();
        when(apiWrapper.untagResource(eq(untagRequest), Mockito.any()))
                .thenReturn(UntagResourceResponse.builder().build());

        TagResourceRequest tagRequest = TagResourceRequest.builder()
                .resourceArn(appArn)
                .tags(ImmutableMap.copyOf(newTags))
                .build();
        when(apiWrapper.tagResource(eq(tagRequest), Mockito.any()))
                .thenReturn(TagResourceResponse.builder().build());

        ImmutableMap<String, String> listTags = ImmutableMap.of("Tag1", "Value11", "Tag2", "Value2", "Tag3", "Value3");
        when(apiWrapper.listTags(any(ListTagsForResourceRequest.class), Mockito.any()))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .tags(listTags)
                        .build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .previousResourceTags(previousTags)
                .desiredResourceTags(newTags)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertNotNull(response);
        assertEquals(OperationStatus.SUCCESS, response.getStatus());
        assertEquals(0, response.getCallbackDelaySeconds());

        final ResourceModel actualModel = response.getResourceModel();
        assertNotNull(actualModel);
        assertEquals(appId, actualModel.getApplicationId());
        assertEquals(appArn, actualModel.getApplicationArn());
        assertEquals(listTags, actualModel.getTags());

        verify(apiWrapper).updateApplication(any(UpdateApplicationRequest.class), Mockito.any());
        verify(apiWrapper, times(3)).getApplication(any(GetApplicationRequest.class), Mockito.any());

        verify(apiWrapper).untagResource(eq(untagRequest), Mockito.any());
        verify(apiWrapper).tagResource(eq(tagRequest), Mockito.any());
        verify(apiWrapper).listTags(any(ListTagsForResourceRequest.class), Mockito.any());
    }

    @Test
    public void handleRequest_addNewTags() {
        final ImmutableMap<String, String> previousTags = ImmutableMap.of("Tag1", "Value1");
        final ImmutableMap<String, String> newTags = ImmutableMap.of("Tag1", "Value1", "Tag2", "Value2");

        final ResourceModel model = getResourceModel();
        when(apiWrapper.updateApplication(any(UpdateApplicationRequest.class), Mockito.any()))
                .thenReturn(UpdateApplicationResponse.builder().build());

        final GetApplicationResponse getApplicationResponse = GetApplicationResponse.builder()
                .applicationArn(appArn)
                .applicationId(appId)
                .latestVersion(ApplicationVersionSummary.builder().applicationVersion(1).build())
                .status(ApplicationLifecycle.AVAILABLE)
                .build();
        when(apiWrapper.getApplication(any(GetApplicationRequest.class), Mockito.any()))
                .thenReturn(getApplicationResponse);

        TagResourceRequest tagRequest = TagResourceRequest.builder()
                .resourceArn(appArn)
                .tags(ImmutableMap.of("Tag2", "Value2"))
                .build();
        when(apiWrapper.tagResource(eq(tagRequest), Mockito.any()))
                .thenReturn(TagResourceResponse.builder().build());

        when(apiWrapper.listTags(any(ListTagsForResourceRequest.class), Mockito.any()))
                .thenReturn(ListTagsForResourceResponse.builder().tags(ImmutableMap.copyOf(newTags)).build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .previousResourceTags(previousTags)
                .desiredResourceTags(newTags)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertNotNull(response);
        assertEquals(OperationStatus.SUCCESS, response.getStatus());
        assertEquals(0, response.getCallbackDelaySeconds());

        final ResourceModel actualModel = response.getResourceModel();
        assertNotNull(actualModel);
        assertEquals(appId, actualModel.getApplicationId());
        assertEquals(appArn, actualModel.getApplicationArn());
        assertEquals(newTags, actualModel.getTags());

        verify(apiWrapper).updateApplication(any(UpdateApplicationRequest.class), Mockito.any());
        verify(apiWrapper, times(3))
                .getApplication(any(GetApplicationRequest.class), Mockito.any());

        verify(apiWrapper, never()).untagResource(any(UntagResourceRequest.class), Mockito.any());
        verify(apiWrapper).tagResource(eq(tagRequest), Mockito.any());
        verify(apiWrapper).listTags(any(ListTagsForResourceRequest.class), Mockito.any());
    }

    @Test
    public void handleRequest_removeOldTags() {
        final ImmutableMap<String, String> previousTags = ImmutableMap.of("Tag1", "Value1", "Tag2", "Value2");
        final ImmutableMap<String, String> newTags = ImmutableMap.of("Tag1", "Value1");

        final ResourceModel model = getResourceModel();
        when(apiWrapper.updateApplication(any(UpdateApplicationRequest.class), Mockito.any()))
                .thenReturn(UpdateApplicationResponse.builder().build());

        final GetApplicationResponse getApplicationResponse = GetApplicationResponse.builder()
                .applicationArn(appArn)
                .applicationId(appId)
                .latestVersion(ApplicationVersionSummary.builder().applicationVersion(1).build())
                .status(ApplicationLifecycle.AVAILABLE)
                .build();
        when(apiWrapper.getApplication(any(GetApplicationRequest.class), Mockito.any()))
                .thenReturn(getApplicationResponse);

        UntagResourceRequest untagRequest = UntagResourceRequest.builder()
                .resourceArn(appArn)
                .tagKeys(ImmutableSet.of("Tag2"))
                .build();
        when(apiWrapper.untagResource(eq(untagRequest), Mockito.any()))
                .thenReturn(UntagResourceResponse.builder().build());

        when(apiWrapper.listTags(any(ListTagsForResourceRequest.class), Mockito.any()))
                .thenReturn(ListTagsForResourceResponse.builder().tags(ImmutableMap.copyOf(newTags)).build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .previousResourceTags(previousTags)
                .desiredResourceTags(newTags)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertNotNull(response);
        assertEquals(OperationStatus.SUCCESS, response.getStatus());
        assertEquals(0, response.getCallbackDelaySeconds());

        final ResourceModel actualModel = response.getResourceModel();
        assertNotNull(actualModel);
        assertEquals(appId, actualModel.getApplicationId());
        assertEquals(appArn, actualModel.getApplicationArn());
        assertEquals(newTags, actualModel.getTags());

        verify(apiWrapper).updateApplication(any(UpdateApplicationRequest.class), Mockito.any());
        verify(apiWrapper, times(3))
                .getApplication(any(GetApplicationRequest.class), Mockito.any());

        verify(apiWrapper).untagResource(eq(untagRequest), Mockito.any());
        verify(apiWrapper, never()).tagResource(any(TagResourceRequest.class), Mockito.any());
        verify(apiWrapper).listTags(any(ListTagsForResourceRequest.class), Mockito.any());
    }

    @Test
    public void handleRequest_updateInvalidRequestFailure() {
        final ResourceModel model = getResourceModel();
        final GetApplicationResponse getApplicationResponse = GetApplicationResponse.builder()
                .applicationArn(appArn)
                .applicationId(appId)
                .latestVersion(ApplicationVersionSummary.builder().applicationVersion(1).build())
                .status(ApplicationLifecycle.AVAILABLE)
                .build();
        when(apiWrapper.getApplication(any(GetApplicationRequest.class), Mockito.any()))
                .thenReturn(getApplicationResponse);

        when(apiWrapper.updateApplication(any(UpdateApplicationRequest.class), Mockito.any()))
                .thenThrow(new CfnInvalidRequestException(ConflictException.builder().build()));

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        assertThrows(CfnInvalidRequestException.class,
                () -> handler.handleRequest(proxy, request, null, logger));

        verify(apiWrapper, times(1)) // get app version call
                .getApplication(any(GetApplicationRequest.class), Mockito.any());
        verify(apiWrapper).updateApplication(any(UpdateApplicationRequest.class), Mockito.any());
    }

    @Test
    public void handleRequest_updateFailed() {
        final ResourceModel model = getResourceModel();
        when(apiWrapper.updateApplication(any(UpdateApplicationRequest.class), Mockito.any()))
                .thenReturn(UpdateApplicationResponse.builder().build());

        final GetApplicationResponse getApplicationResponse = GetApplicationResponse.builder()
                .applicationArn(appArn)
                .applicationId(appId)
                .latestVersion(ApplicationVersionSummary.builder().applicationVersion(1).build())
                .status(ApplicationLifecycle.FAILED)
                .build();
        when(apiWrapper.getApplication(any(GetApplicationRequest.class), Mockito.any()))
                .thenReturn(getApplicationResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        assertThrows(CfnGeneralServiceException.class,
                () -> handler.handleRequest(proxy, request, null, logger));

        verify(apiWrapper).updateApplication(any(UpdateApplicationRequest.class), Mockito.any());
        verify(apiWrapper, times(2)).getApplication(any(GetApplicationRequest.class), Mockito.any());
    }

    private ResourceModel getResourceModel() {
        return ResourceModel.builder()
                .applicationId(appId)
                .applicationArn(appArn)
                .definition(Definition.builder().s3Location("s3://test_bucket/path").content("dummy").build())
                .description("update application")
                .build();
    }
}
