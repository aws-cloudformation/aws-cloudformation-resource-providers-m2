package software.amazon.m2.application;

import software.amazon.awssdk.services.m2.model.CreateApplicationRequest;
import software.amazon.awssdk.services.m2.model.Definition;
import software.amazon.awssdk.services.m2.model.DeleteApplicationRequest;
import software.amazon.awssdk.services.m2.model.GetApplicationRequest;
import software.amazon.awssdk.services.m2.model.GetApplicationResponse;
import software.amazon.awssdk.services.m2.model.ListApplicationsRequest;
import software.amazon.awssdk.services.m2.model.ListApplicationsResponse;
import software.amazon.awssdk.services.m2.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.m2.model.TagResourceRequest;
import software.amazon.awssdk.services.m2.model.UntagResourceRequest;
import software.amazon.awssdk.services.m2.model.UpdateApplicationRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Translator {

    /**
     * Request to create a resource
     *
     * @param model resource model
     * @param tags
     * @return awsRequest the aws service request to create a resource
     */
    static CreateApplicationRequest toCreateApplicationRequest(ResourceModel model,
                                                               String clientToken,
                                                               Map<String, String> tags) {
        return CreateApplicationRequest.builder()
            .clientToken(clientToken)
            .definition(toApplicationDefinition(model.getDefinition()))
            .description(model.getDescription())
            .engineType(model.getEngineType())
            .name(model.getName())
            .tags(tags)
            .build();
    }

    static GetApplicationRequest toGetApplicationRequest(ResourceModel model) {
        return toGetApplicationRequest(model.getApplicationArn());
    }

    static GetApplicationRequest toGetApplicationRequest(String applicationArn) {
        return GetApplicationRequest.builder().applicationId(getApplicationId(applicationArn)).build();
    }

    private static String getApplicationId(String applicationArn) {
        // Extract applicationId from arn:{Partition}:m2:{Region}:{Account}:app/{applicationId}
        if (applicationArn == null || applicationArn.trim().isEmpty()) {
            return null;
        }
        int delimIndex = applicationArn.indexOf('/');
        if (delimIndex < 0) {
            throw new CfnInvalidRequestException("Invalid application ARN: " + applicationArn);
        }
        return applicationArn.substring(delimIndex + 1);
    }

    static UpdateApplicationRequest toUpdateApplicationRequest(ResourceModel model, Integer currentApplicationVersion) {
        return UpdateApplicationRequest.builder()
                .applicationId(getApplicationId(model.getApplicationArn()))
                .currentApplicationVersion(currentApplicationVersion)
                .definition(model.getDefinition() == null ? null : Definition.builder()
                        .content(model.getDefinition().getContent())
                        .s3Location(model.getDefinition().getS3Location())
                        .build())
                .description(model.getDescription())
                .build();
    }

    static DeleteApplicationRequest toDeleteApplicationRequest(ResourceModel model) {
        return DeleteApplicationRequest.builder()
                .applicationId(getApplicationId(model.getApplicationArn()))
                .build();
    }

    static ListApplicationsRequest toListApplicationsRequest(String nextToken) {
        return ListApplicationsRequest.builder()
            .nextToken(nextToken)
            .build();
    }

    static List<ResourceModel> toListApplicationsResponse(ListApplicationsResponse response) {
        return response.applications().stream().map(application ->
            ResourceModel.builder()
                .applicationArn(application.applicationArn())
                .applicationId(application.applicationId())
                .build()
        ).collect(Collectors.toList());
    }

    static UntagResourceRequest toUntagResourceRequest(ResourceModel resourceModel, Set<String> tagsToRemove) {
        return UntagResourceRequest.builder()
            .resourceArn(resourceModel.getApplicationArn())
            .tagKeys(tagsToRemove)
            .build();
    }

    static TagResourceRequest toTagResourceRequest(ResourceModel model, Map<String, String> tagsToAdd) {
        return TagResourceRequest
            .builder()
            .resourceArn(model.getApplicationArn())
            .tags(tagsToAdd)
            .build();
    }

    static ResourceModel getApplicationResourceModel(final GetApplicationResponse response) {
        ResourceModel.ResourceModelBuilder builder = ResourceModel.builder()
                .applicationId(response.applicationId())
                .applicationArn(response.applicationArn())
                .name(response.name())
                .engineType(response.engineTypeAsString());
        if (response.description() != null) {
            builder.description(response.description());
        }
        return builder.build();
    }

    static ListTagsForResourceRequest toListTagsRequest(ResourceModel resourceModel) {
        return ListTagsForResourceRequest.builder()
            .resourceArn(resourceModel.getApplicationArn())
            .build();
    }

    private static Definition toApplicationDefinition(
            software.amazon.m2.application.Definition definition) {
        return Definition.builder()
            .content(definition.getContent())
            .s3Location(definition.getS3Location())
            .build();
    }
}
