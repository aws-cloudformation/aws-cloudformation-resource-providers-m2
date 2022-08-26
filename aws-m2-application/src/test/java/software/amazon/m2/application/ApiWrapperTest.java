package software.amazon.m2.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.m2.model.CreateApplicationRequest;
import software.amazon.awssdk.services.m2.model.DeleteApplicationRequest;
import software.amazon.awssdk.services.m2.model.GetApplicationRequest;
import software.amazon.awssdk.services.m2.model.ListApplicationsRequest;
import software.amazon.awssdk.services.m2.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.m2.model.TagResourceRequest;
import software.amazon.awssdk.services.m2.model.UntagResourceRequest;
import software.amazon.awssdk.services.m2.model.UpdateApplicationRequest;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.m2.common.AbstractTestBase;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ApiWrapperTest extends AbstractTestBase {

    private final ApiWrapper apiWrapper = new ApiWrapper();
    @Mock
    private Logger mockLogger;

    @BeforeEach
    public void setLogger() {
        apiWrapper.setLogger(mockLogger);
    }

    @Test
    public void testCreateApplication_NullRequest() {
        assertThrows(NullPointerException.class,
                () -> apiWrapper.createApplication(null, proxyClient));
    }

    @Test
    public void testCreateApplication_NullProxy() {
        assertThrows(NullPointerException.class,
                () -> apiWrapper.createApplication(CreateApplicationRequest.builder().build(), null));
    }

    @Test
    public void testCreateApplication_success() {

        CreateApplicationRequest request = CreateApplicationRequest.builder().name("my app").build();
        apiWrapper.createApplication(request, proxyClient);

        Mockito.verify(m2Client, Mockito.times(1)).createApplication(Mockito.any(CreateApplicationRequest.class));
    }

    @Test
    public void testGetApplication_NullRequest() {
        assertThrows(NullPointerException.class,
                () -> apiWrapper.getApplication(null, proxyClient));
    }

    @Test
    public void testGetApplication_NullProxy() {
        assertThrows(NullPointerException.class,
                () -> apiWrapper.getApplication(GetApplicationRequest.builder().build(), null));
    }

    @Test
    public void testGetApplication_success() {

        GetApplicationRequest request = GetApplicationRequest.builder().applicationId("app-id").build();
        apiWrapper.getApplication(request, proxyClient);

        Mockito.verify(m2Client, Mockito.times(1)).getApplication(Mockito.any(GetApplicationRequest.class));
    }

    @Test
    public void testUpdateApplication_NullRequest() {
        assertThrows(NullPointerException.class,
                () -> apiWrapper.updateApplication(null, proxyClient));
    }

    @Test
    public void testUpdateApplication_NullProxy() {
        assertThrows(NullPointerException.class,
                () -> apiWrapper.updateApplication(UpdateApplicationRequest.builder().build(), null));
    }

    @Test
    public void testUpdateApplication_success() {
        UpdateApplicationRequest request = UpdateApplicationRequest.builder().applicationId("app-id").build();
        apiWrapper.updateApplication(request, proxyClient);

        Mockito.verify(m2Client, Mockito.times(1)).updateApplication(Mockito.any(UpdateApplicationRequest.class));
    }

    @Test
    public void testDeleteApplication_NullRequest() {
        assertThrows(NullPointerException.class,
                () -> apiWrapper.deleteApplication(null, proxyClient));
    }

    @Test
    public void testDeleteApplication_NullProxy() {
        assertThrows(NullPointerException.class,
                () -> apiWrapper.deleteApplication(DeleteApplicationRequest.builder().build(), null));
    }

    @Test
    public void testDeleteApplication_success() {
        DeleteApplicationRequest request = DeleteApplicationRequest.builder().applicationId("app-id").build();
        apiWrapper.deleteApplication(request, proxyClient);

        Mockito.verify(m2Client, Mockito.times(1)).deleteApplication(Mockito.any(DeleteApplicationRequest.class));
    }

    @Test
    public void testListApplications_NullRequest() {
        assertThrows(NullPointerException.class,
                () -> apiWrapper.listApplications(null, proxyClient));
    }

    @Test
    public void testListApplications_NullProxy() {
        assertThrows(NullPointerException.class,
                () -> apiWrapper.listApplications(ListApplicationsRequest.builder().build(), null));
    }

    @Test
    public void testListApplications_success() {
        ListApplicationsRequest request = ListApplicationsRequest.builder().build();
        apiWrapper.listApplications(request, proxyClient);

        Mockito.verify(m2Client, Mockito.times(1)).listApplications(Mockito.any(ListApplicationsRequest.class));
    }

    @Test
    public void testListTags_NullRequest() {
        assertThrows(NullPointerException.class,
                () -> apiWrapper.listTags(null, proxyClient));
    }

    @Test
    public void testListTags_NullProxy() {
        assertThrows(NullPointerException.class,
                () -> apiWrapper.listTags(ListTagsForResourceRequest.builder().build(), null));
    }

    @Test
    public void testListTags_success() {
        ListTagsForResourceRequest request = ListTagsForResourceRequest.builder().resourceArn("arn:m2:resource").build();
        apiWrapper.listTags(request, proxyClient);

        Mockito.verify(m2Client, Mockito.times(1)).listTagsForResource(Mockito.any(ListTagsForResourceRequest.class));
    }

    @Test
    public void testUntagResource_NullRequest() {
        assertThrows(NullPointerException.class,
                () -> apiWrapper.untagResource(null, proxyClient));
    }

    @Test
    public void testUntagResource_NullProxy() {
        assertThrows(NullPointerException.class,
                () -> apiWrapper.untagResource(UntagResourceRequest.builder().build(), null));
    }

    @Test
    public void testUntagResource_success() {
        UntagResourceRequest request = UntagResourceRequest.builder().build();
        apiWrapper.untagResource(request, proxyClient);

        Mockito.verify(m2Client, Mockito.times(1)).untagResource(Mockito.any(UntagResourceRequest.class));
    }

    @Test
    public void testTagResource_NullRequest() {
        assertThrows(NullPointerException.class,
                () -> apiWrapper.tagResource(null, proxyClient));
    }

    @Test
    public void testTagResource_NullProxy() {
        assertThrows(NullPointerException.class,
                () -> apiWrapper.tagResource(TagResourceRequest.builder().build(), null));
    }

    @Test
    public void testTagResource_success() {

        TagResourceRequest request = TagResourceRequest.builder().build();
        apiWrapper.tagResource(request, proxyClient);

        Mockito.verify(m2Client, Mockito.times(1)).tagResource(Mockito.any(TagResourceRequest.class));
    }
}

