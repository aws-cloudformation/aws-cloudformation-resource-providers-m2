package software.amazon.m2.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.m2.model.DeleteApplicationRequest;
import software.amazon.awssdk.services.m2.model.DeleteApplicationResponse;
import software.amazon.awssdk.services.m2.model.GetApplicationRequest;
import software.amazon.awssdk.services.m2.model.GetApplicationResponse;
import software.amazon.awssdk.services.m2.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.m2.common.AbstractTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    private ApiWrapper apiWrapper;

    private DeleteHandler handler;

    @BeforeEach
    public void setup() {
        handler = new DeleteHandler(apiWrapper);
    }

    @Test
    public void handleRequest_Success() {
        final ResourceModel model = ResourceModel.builder()
                .applicationArn("arn:aws:m2:us-west-2:123456:app/app-id")
                .build();

        when(apiWrapper.deleteApplication(any(DeleteApplicationRequest.class), Mockito.any()))
                .thenReturn(DeleteApplicationResponse.builder().build());

        when(apiWrapper.getApplication(any(GetApplicationRequest.class), Mockito.any()))
                .thenReturn(GetApplicationResponse.builder().status("Available").build())
                .thenThrow(new CfnNotFoundException(ResourceNotFoundException.builder().build()));

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertNotNull(response);
        assertEquals(OperationStatus.SUCCESS, response.getStatus());
        assertEquals(0, response.getCallbackDelaySeconds());
        assertNull(response.getResourceModel());

        verify(apiWrapper).deleteApplication(any(DeleteApplicationRequest.class), Mockito.any());
        verify(apiWrapper, times(2)).getApplication(any(GetApplicationRequest.class), Mockito.any());
    }

    @Test
    public void handleRequest_waitUntilStabilized() {
        final ResourceModel model = ResourceModel.builder()
                .applicationArn("arn:aws:m2:us-west-2:123456:app/app-id")
                .build();

        when(apiWrapper.deleteApplication(any(DeleteApplicationRequest.class), Mockito.any()))
                .thenReturn(DeleteApplicationResponse.builder().build());

        when(apiWrapper.getApplication(any(GetApplicationRequest.class), Mockito.any()))
                .thenReturn(GetApplicationResponse.builder().status("Available").build())
                .thenReturn(GetApplicationResponse.builder().status("Deleting").build())
                .thenThrow(new CfnNotFoundException(ResourceNotFoundException.builder().build()));

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertNotNull(response);
        assertEquals(OperationStatus.SUCCESS, response.getStatus());
        assertEquals(0, response.getCallbackDelaySeconds());
        assertNull(response.getResourceModel());

        verify(apiWrapper).deleteApplication(any(DeleteApplicationRequest.class), Mockito.any());
        verify(apiWrapper, times(3)).getApplication(any(GetApplicationRequest.class), Mockito.any());
    }
}
