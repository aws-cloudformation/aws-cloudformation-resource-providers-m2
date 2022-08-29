package software.amazon.m2.application;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.m2.model.ApplicationSummary;
import software.amazon.awssdk.services.m2.model.ListApplicationsRequest;
import software.amazon.awssdk.services.m2.model.ListApplicationsResponse;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.m2.common.AbstractTestBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    @Mock
    private ApiWrapper apiWrapper;

    private ListHandler handler;

    @BeforeEach
    public void setup() {
        handler = new ListHandler(apiWrapper);
    }


    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ListApplicationsResponse listResponse = ListApplicationsResponse.builder()
                .applications(ImmutableList.of(ApplicationSummary.builder()
                        .applicationId("app-id")
                        .applicationArn("app-arn")
                        .build()))
                .build();
        when(apiWrapper.listApplications(eq(ListApplicationsRequest.builder().build()), Mockito.any()))
                .thenReturn(listResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertNotNull(response);
        assertEquals(OperationStatus.SUCCESS, response.getStatus());
        assertNull(response.getNextToken());

        final List<ResourceModel> actualModels = response.getResourceModels();
        assertNotNull(actualModels);
        assertEquals(1, actualModels.size());
        assertEquals("app-id", actualModels.get(0).getApplicationId());
        assertEquals("app-arn", actualModels.get(0).getApplicationArn());

        verify(apiWrapper).listApplications(eq(ListApplicationsRequest.builder().build()), Mockito.any());
    }

    @Test
    public void handleRequest_WithNextToken() {
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ListApplicationsResponse listResponse = ListApplicationsResponse.builder()
                .applications(ImmutableList.of(ApplicationSummary.builder()
                        .applicationId("app-id")
                        .applicationArn("app-arn")
                        .build()))
                .nextToken("next-token")
                .build();
        when(apiWrapper.listApplications(eq(ListApplicationsRequest.builder().build()), Mockito.any()))
                .thenReturn(listResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertNotNull(response);
        assertEquals(OperationStatus.SUCCESS, response.getStatus());
        assertEquals("next-token", response.getNextToken());

        final List<ResourceModel> actualModels = response.getResourceModels();
        assertNotNull(actualModels);
        assertEquals(1, actualModels.size());
        assertEquals("app-id", actualModels.get(0).getApplicationId());
        assertEquals("app-arn", actualModels.get(0).getApplicationArn());

        verify(apiWrapper).listApplications(eq(ListApplicationsRequest.builder().build()), Mockito.any());
    }
}
