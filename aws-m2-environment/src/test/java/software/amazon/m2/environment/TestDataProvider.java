package software.amazon.m2.environment;

import software.amazon.awssdk.services.m2.model.GetEnvironmentResponse;
import software.amazon.awssdk.services.m2.model.HighAvailabilityConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TestDataProvider {

    static ResourceModel resourceModel() {

        return ResourceModel.builder()
                .environmentId("env-id")
                .environmentArn("arn:aws:m2:us-west-2:123456:env/env-id")
                .name("env-name")
                .instanceType("m2.m5.large")
                .engineType("microfocus")
                .engineVersion("7.0.3")
                .description("for testing cfn")
                .subnetIds(List.of("subnet-id-1"))
                .securityGroupIds(List.of("sec-g1"))
                .storageConfigurations(List.of(StorageConfiguration.builder()
                        .fsx(FsxStorageConfiguration.builder().mountPoint("/mnt/fsx").fileSystemId("fsx-id").build())
                        .efs(EfsStorageConfiguration.builder().mountPoint("mnt/efs").fileSystemId("efs-123").build())
                        .build()))
                .publiclyAccessible(true)
                .highAvailabilityConfig(software.amazon.m2.environment.HighAvailabilityConfig.builder().desiredCapacity(2).build())
                .tags(Map.of("key1", "val1", "key2", "val2"))
                .preferredMaintenanceWindow("thu:10:00-thu:12:00")
                .build();
    }

    static GetEnvironmentResponse getEnvironmentResponseFromModel(ResourceModel model, String envId, String status) {
        GetEnvironmentResponse.Builder responseBuilder = GetEnvironmentResponse.builder();
        if (envId != null) {
            responseBuilder.environmentId(envId);
        }
        if (model.getEnvironmentArn() != null) {
            responseBuilder.environmentArn(model.getEnvironmentArn());
        }
        if (model.getName() != null) {
            responseBuilder.name(model.getName());
        }
        if (model.getEngineType() != null) {
            responseBuilder.engineType(model.getEngineType());
        }
        if (model.getEngineVersion() != null) {
            responseBuilder.engineVersion(model.getEngineVersion());
        }
        if (model.getInstanceType() != null) {
            responseBuilder.instanceType(model.getInstanceType());
        }
        if (model.getDescription() != null) {
            responseBuilder.description(model.getDescription());
        }
        if (model.getSubnetIds() != null && !model.getSecurityGroupIds().isEmpty()) {
            responseBuilder.subnetIds(model.getSubnetIds());
        }
        if (model.getSecurityGroupIds() != null && !model.getSecurityGroupIds().isEmpty()) {
            responseBuilder.securityGroupIds(model.getSecurityGroupIds());
        }
        if (model.getStorageConfigurations() != null && !model.getStorageConfigurations().isEmpty()) {
            responseBuilder.storageConfigurations(getStorageConfigs(model.getStorageConfigurations()));
        }
        if (model.getPubliclyAccessible() != null) {
            responseBuilder.publiclyAccessible(model.getPubliclyAccessible());
        }
        if (model.getHighAvailabilityConfig() != null) {
            responseBuilder.highAvailabilityConfig(HighAvailabilityConfig.builder().desiredCapacity(
                            model.getHighAvailabilityConfig().getDesiredCapacity()).build());
        }
        if (model.getTags() != null && !model.getTags().isEmpty()) {
            responseBuilder.tags(model.getTags());
        }
        if (model.getPreferredMaintenanceWindow() != null) {
            responseBuilder.preferredMaintenanceWindow(model.getPreferredMaintenanceWindow());
        }
        responseBuilder.status(status);

        return responseBuilder.build();
    }

    private static Collection<software.amazon.awssdk.services.m2.model.StorageConfiguration> getStorageConfigs(
            List<software.amazon.m2.environment.StorageConfiguration> storageConfigurations) {

        Collection<software.amazon.awssdk.services.m2.model.StorageConfiguration> storageConfigs = new ArrayList<>();
        if (storageConfigurations != null && !storageConfigurations.isEmpty()) {
            for (software.amazon.m2.environment.StorageConfiguration stg : storageConfigurations) {
                software.amazon.awssdk.services.m2.model.StorageConfiguration.Builder stgBuilder =
                        software.amazon.awssdk.services.m2.model.StorageConfiguration.builder();
                if (stg.getEfs() != null) {
                    stgBuilder.efs(software.amazon.awssdk.services.m2.model.EfsStorageConfiguration.builder()
                            .mountPoint(stg.getEfs().getMountPoint())
                            .fileSystemId(stg.getEfs().getFileSystemId())
                            .build());
                }
                if (stg.getFsx() != null) {
                    stgBuilder.fsx(software.amazon.awssdk.services.m2.model.FsxStorageConfiguration.builder()
                            .mountPoint(stg.getFsx().getMountPoint())
                            .fileSystemId(stg.getFsx().getFileSystemId())
                            .build());
                }
                storageConfigs.add(stgBuilder.build());
            }
        }
        return storageConfigs;
    }
}
