package software.amazon.m2.application;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.m2.model.GetApplicationRequest;
import software.amazon.awssdk.services.m2.model.GetApplicationResponse;
import software.amazon.awssdk.services.m2.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.m2.model.ListTagsForResourceResponse;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.m2.common.AbstractTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {
    @Mock
    private ApiWrapper apiWrapper;

    private ReadHandler handler;

    @BeforeEach
    public void setup() {
        handler = new ReadHandler(apiWrapper);
    }

    @Test
    public void handleRequest_Success() {
        final ImmutableMap<String, String> tags = ImmutableMap.of("Key1", "Value1", "Key2", "Value2");
        final ResourceModel model = ResourceModel.builder()
                .applicationId("app-id")
                .applicationArn("arn:aws:m2:us-west-2:123456:app/app-id")
                .build();

        GetApplicationResponse getApplicationResponse = GetApplicationResponse
                .builder()
                .applicationId(model.getApplicationId())
                .applicationArn(model.getApplicationArn())
                .build();

        when(apiWrapper.getApplication(any(GetApplicationRequest.class), Mockito.any()))
                .thenReturn(getApplicationResponse);

        when(apiWrapper.listTags(eq(ListTagsForResourceRequest.builder()
                .resourceArn(model.getApplicationArn()).build()), Mockito.any()))
                .thenReturn(ListTagsForResourceResponse.builder().tags(tags).build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), logger);

        assertNotNull(response);
        assertNull(response.getErrorCode(), response.getMessage());
        assertEquals(OperationStatus.SUCCESS, response.getStatus());

        final ResourceModel actualModel = response.getResourceModel();
        assertNotNull(actualModel);
        assertEquals(model.getApplicationId(), actualModel.getApplicationId());
        assertEquals(model.getApplicationArn(), actualModel.getApplicationArn());
        assertEquals(tags, actualModel.getTags());

        verify(apiWrapper).getApplication(any(GetApplicationRequest.class), Mockito.any());
        verify(apiWrapper).listTags(eq(ListTagsForResourceRequest.builder()
                .resourceArn(model.getApplicationArn()).build()), Mockito.any());
    }

    @Test
    public void handleRequest_invalidArn() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().applicationArn("not:an:m2:arn").build()).build();
        assertThrows(CfnInvalidRequestException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), logger));
    }
}
