package software.amazon.m2.environment;

import software.amazon.awssdk.utils.CollectionUtils;

import java.util.Map;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("aws-m2-environment.json");
    }

    @Override
    public Map<String, String> resourceDefinedTags(final ResourceModel resourceModel) {
        if (CollectionUtils.isNullOrEmpty(resourceModel.getTags())) {
            return null;
        }

        return resourceModel.getTags();
    }
}
