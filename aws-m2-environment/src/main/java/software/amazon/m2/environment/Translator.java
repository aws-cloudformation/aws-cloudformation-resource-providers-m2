package software.amazon.m2.environment;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.m2.model.CreateEnvironmentRequest;
import software.amazon.awssdk.services.m2.model.DeleteEnvironmentRequest;
import software.amazon.awssdk.services.m2.model.EfsStorageConfiguration;
import software.amazon.awssdk.services.m2.model.FsxStorageConfiguration;
import software.amazon.awssdk.services.m2.model.GetEnvironmentRequest;
import software.amazon.awssdk.services.m2.model.GetEnvironmentResponse;
import software.amazon.awssdk.services.m2.model.HighAvailabilityConfig;
import software.amazon.awssdk.services.m2.model.ListEnvironmentsRequest;
import software.amazon.awssdk.services.m2.model.ListEnvironmentsResponse;
import software.amazon.awssdk.services.m2.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.m2.model.StorageConfiguration;
import software.amazon.awssdk.services.m2.model.TagResourceRequest;
import software.amazon.awssdk.services.m2.model.UntagResourceRequest;
import software.amazon.awssdk.services.m2.model.UpdateEnvironmentRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Translator {

    /**
     * Request to create a resource
     *
     * @param model       resource model
     * @param clientToken client idempotency token
     * @param tags        tags to associate to resource
     * @return awsRequest the aws service request to create a resource
     */
    static AwsRequest translateToCreateRequest(final ResourceModel model,
                                               final String clientToken,
                                               final Map<String, String> tags) {
        return CreateEnvironmentRequest.builder()
                .clientToken(clientToken)
                .engineType(model.getEngineType())
                .engineVersion(model.getEngineVersion())
                .description(model.getDescription())
                .name(model.getName())
                .instanceType(model.getInstanceType())
                .publiclyAccessible(model.getPubliclyAccessible())
                .highAvailabilityConfig(translateToEnvHighAvailabilityConfig(model.getHighAvailabilityConfig()))
                .storageConfigurations(translateToEnvStorageConfigurations(model.getStorageConfigurations()))
                .subnetIds(model.getSubnetIds())
                .securityGroupIds(model.getSecurityGroupIds())
                .preferredMaintenanceWindow(model.getPreferredMaintenanceWindow())
                .tags(tags)
                .build();
    }

    private static Collection<software.amazon.awssdk.services.m2.model.StorageConfiguration> translateToEnvStorageConfigurations(
            List<software.amazon.m2.environment.StorageConfiguration> resourceStorageConfigs) {

        return streamOfOrEmpty(resourceStorageConfigs)
                .map(stc -> StorageConfiguration.builder()
                        .efs(getEfsStorageConfiguration(stc.getEfs()))
                        .fsx(getFsxStorageConfiguration(stc.getFsx()))
                        .build())
                .collect(Collectors.toList());
    }

    private static software.amazon.awssdk.services.m2.model.FsxStorageConfiguration getFsxStorageConfiguration(
            software.amazon.m2.environment.FsxStorageConfiguration resourceFsx) {
        if (resourceFsx == null) {
            return null;
        }
        return FsxStorageConfiguration.builder()
                .fileSystemId(resourceFsx.getFileSystemId())
                .mountPoint(resourceFsx.getMountPoint())
                .build();
    }

    private static software.amazon.awssdk.services.m2.model.EfsStorageConfiguration getEfsStorageConfiguration(
            software.amazon.m2.environment.EfsStorageConfiguration resourceEfs) {
        if (resourceEfs == null) {
            return null;
        }
        return EfsStorageConfiguration.builder()
                .fileSystemId(resourceEfs.getFileSystemId())
                .mountPoint(resourceEfs.getMountPoint())
                .build();
    }

    private static software.amazon.awssdk.services.m2.model.HighAvailabilityConfig translateToEnvHighAvailabilityConfig(
            software.amazon.m2.environment.HighAvailabilityConfig resourceHaConfig) {
        if (resourceHaConfig == null) {
            return null;
        }
        return HighAvailabilityConfig.builder()
                .desiredCapacity(resourceHaConfig.getDesiredCapacity())
                .build();
    }

    /**
     * Request to read a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to describe a resource
     */
    static AwsRequest translateToReadRequest(final ResourceModel model) {
        GetEnvironmentRequest.Builder builder = GetEnvironmentRequest.builder();
        if (model.getEnvironmentId() != null) {
            builder.environmentId(model.getEnvironmentId());
        } else if (model.getEnvironmentArn() != null) {
            builder.environmentId(extractEnvironmentId(model.getEnvironmentArn()));
        }
        return builder.build();
    }

    private static String extractEnvironmentId(String environmentArn) {
        // Extract environmentId from arn:{Partition}:m2:{Region}:{Account}:env/{environmentId}
        if (environmentArn == null || environmentArn.trim().isEmpty()) {
            return null;
        }
        int delimIndex = environmentArn.indexOf('/');
        if (delimIndex < 0) {
            throw new CfnInvalidRequestException("Invalid environment ARN: " + environmentArn);
        }
        return environmentArn.substring(delimIndex + 1);
    }

    /**
     * Translates resource object from sdk into a resource model
     *
     * @param awsResponse the aws service describe resource response
     * @return model resource model
     */
    static ResourceModel translateFromReadResponse(final GetEnvironmentResponse awsResponse) {
        // The model MUST NOT return any properties that are null or don't have values.
        ResourceModel.ResourceModelBuilder builder = ResourceModel.builder();
        if (awsResponse.environmentId() != null) {
            builder.environmentId(awsResponse.environmentId());
        }
        if (awsResponse.environmentArn() != null) {
            builder.environmentArn(awsResponse.environmentArn());
        }
        if (awsResponse.name() != null) {
            builder.name(awsResponse.name());
        }
        if (awsResponse.engineType() != null) {
            builder.engineType(awsResponse.engineType().toString());
        }
        if (awsResponse.engineVersion() != null) {
            builder.engineVersion(awsResponse.engineVersion());
        }
        if (awsResponse.instanceType() != null) {
            builder.instanceType(awsResponse.instanceType());
        }
        if (awsResponse.description() != null) {
            builder.description(awsResponse.description());
        }
        if (awsResponse.publiclyAccessible() != null) {
            builder.publiclyAccessible(awsResponse.publiclyAccessible());
        }
        if (awsResponse.highAvailabilityConfig() != null) {
            builder.highAvailabilityConfig(software.amazon.m2.environment.HighAvailabilityConfig.builder()
                    .desiredCapacity(awsResponse.highAvailabilityConfig().desiredCapacity()).build());
        }
        if (awsResponse.subnetIds() != null && !awsResponse.subnetIds().isEmpty()) {
            builder.subnetIds(awsResponse.subnetIds());
        }
        if (awsResponse.securityGroupIds() != null && !awsResponse.securityGroupIds().isEmpty()) {
            builder.securityGroupIds(awsResponse.securityGroupIds());
        }
        if (awsResponse.storageConfigurations() != null && !awsResponse.storageConfigurations().isEmpty()) {
            List<software.amazon.m2.environment.StorageConfiguration> configs =
                    getStorageConfigurations(awsResponse.storageConfigurations());
            if (!configs.isEmpty()) {
                builder.storageConfigurations(configs);
            }
        }
        if (awsResponse.preferredMaintenanceWindow() != null) {
            builder.preferredMaintenanceWindow(awsResponse.preferredMaintenanceWindow());
        }
        return builder.build();
    }

    static List<software.amazon.m2.environment.StorageConfiguration> getStorageConfigurations(List<StorageConfiguration> sgcs) {
        return streamOfOrEmpty(sgcs)
                .map(sc -> software.amazon.m2.environment.StorageConfiguration.builder()
                        .efs(sc.efs() == null ? null :
                                software.amazon.m2.environment.EfsStorageConfiguration.builder()
                                        .fileSystemId(sc.efs().fileSystemId())
                                        .mountPoint(sc.efs().mountPoint())
                                        .build())
                        .fsx(sc.fsx() == null ? null :
                                software.amazon.m2.environment.FsxStorageConfiguration.builder()
                                        .fileSystemId(sc.fsx().fileSystemId())
                                        .mountPoint(sc.fsx().mountPoint())
                                        .build())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Request to delete a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to delete a resource
     */
    static AwsRequest translateToDeleteRequest(final ResourceModel model) {
        DeleteEnvironmentRequest.Builder builder = DeleteEnvironmentRequest.builder();
        if (model.getEnvironmentId() != null) {
            builder.environmentId(model.getEnvironmentId());
        } else if (model.getEnvironmentArn() != null) {
            builder.environmentId(extractEnvironmentId(model.getEnvironmentArn()));
        }
        return builder.build();
    }

    /**
     * Request to update properties of a previously created resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to modify a resource
     */
    static AwsRequest translateToUpdateRequest(final ResourceModel model) {
        UpdateEnvironmentRequest.Builder builder = UpdateEnvironmentRequest.builder();
        if (model.getEnvironmentId() != null) {
            builder.environmentId(model.getEnvironmentId());
        } else {
            builder.environmentId(extractEnvironmentId(model.getEnvironmentArn()));
        }
        if (model.getHighAvailabilityConfig() != null) {
            builder.desiredCapacity(model.getHighAvailabilityConfig().getDesiredCapacity());
        }
        if (model.getEngineVersion() != null) {
            builder.engineVersion(model.getEngineVersion());
        }
        if (model.getInstanceType() != null) {
            builder.instanceType(model.getInstanceType());
        }
        if (model.getPreferredMaintenanceWindow() != null) {
            builder.preferredMaintenanceWindow(model.getPreferredMaintenanceWindow());
        }
        // keep it always false - we want the update to be handled immediately and not to wait for the maintenance window
        builder.applyDuringMaintenanceWindow(false);
        return builder.build();
    }

    /**
     * Request to list resources
     *
     * @param nextToken token passed to the aws service list resources request
     * @return awsRequest the aws service request to list resources within aws account
     */
    static AwsRequest translateToListRequest(final String nextToken) {
        return ListEnvironmentsRequest.builder()
                .nextToken(nextToken)
                .build();
    }

    /**
     * Translates resource objects from sdk into a resource model (primary identifier only)
     *
     * @param awsResponse the aws service list resources response
     * @return list of resource models
     */
    static List<ResourceModel> translateFromListResponse(final ListEnvironmentsResponse awsResponse) {
        return streamOfOrEmpty(awsResponse.environments())
                .map(envSummary -> ResourceModel.builder()
                        // include only primary identifier
                        .environmentArn(envSummary.environmentArn())
                        .build())
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection).stream().flatMap(Collection::stream);
    }

    /**
     * Request to add tags to a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to create a resource
     */
    static TagResourceRequest tagResourceRequest(final ResourceModel model, final Map<String, String> addedTags) {
        return TagResourceRequest.builder()
                .resourceArn(model.getEnvironmentArn())
                .tags(addedTags)
                .build();
    }

    /**
     * Request to remove tags from a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to remove tags for a resource
     */
    static UntagResourceRequest untagResourceRequest(final ResourceModel model, final Set<String> removedTags) {
        return UntagResourceRequest.builder()
                .resourceArn(model.getEnvironmentArn())
                .tagKeys(removedTags)
                .build();
    }

    static ListTagsForResourceRequest translateToListResourceTagsRequest(ResourceModel model) {
        return ListTagsForResourceRequest.builder().resourceArn(model.getEnvironmentArn()).build();
    }
}
