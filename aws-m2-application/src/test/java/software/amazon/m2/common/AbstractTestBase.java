package software.amazon.m2.common;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public abstract class AbstractTestBase {
    protected static final LoggerProxy logger = new LoggerProxy();
    protected static final Credentials credentials = new Credentials("accessKey", "secretKey", "token");

    @Mock
    protected AmazonWebServicesClientProxy proxy;

    @Mock
    protected ProxyClient<M2Client> proxyClient;

    @Mock
    protected M2Client m2Client;

    public static ProxyClient<M2Client> mockedProxyClient(
            final AmazonWebServicesClientProxy proxy,
            final M2Client sdkClient) {

        return new ProxyClient<>() {
            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseT
            injectCredentialsAndInvokeV2(RequestT request, Function<RequestT, ResponseT> requestFunction) {
                return proxy.injectCredentialsAndInvokeV2(request, requestFunction);
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> CompletableFuture<ResponseT>
            injectCredentialsAndInvokeV2Async(RequestT request, Function<RequestT, CompletableFuture<ResponseT>> requestFunction) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse, IterableT extends SdkIterable<ResponseT>> IterableT
            injectCredentialsAndInvokeIterableV2(RequestT request, Function<RequestT, IterableT> requestFunction) {
                return proxy.injectCredentialsAndInvokeIterableV2(request, requestFunction);
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseInputStream<ResponseT>
            injectCredentialsAndInvokeV2InputStream(RequestT requestT, Function<RequestT, ResponseInputStream<ResponseT>> function) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseBytes<ResponseT>
            injectCredentialsAndInvokeV2Bytes(RequestT requestT, Function<RequestT, ResponseBytes<ResponseT>> function) {
                throw new UnsupportedOperationException();
            }

            @Override
            public M2Client client() {
                return sdkClient;
            }
        };
    }

    @BeforeEach
    public void initMocks() {
        proxy = new AmazonWebServicesClientProxy(logger, credentials, () -> Duration.ofSeconds(600).toMillis());
        proxyClient = mockedProxyClient(proxy, m2Client);
    }

}
