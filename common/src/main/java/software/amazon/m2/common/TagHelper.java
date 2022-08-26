package software.amazon.m2.common;

import com.google.common.collect.ImmutableMap;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class TagHelper {

    private TagHelper() {
    }

    /*
     * If stack tags and resource tags are not merged together in Configuration class,
     * we need to get previous attached user defined tags from both handlerRequest.getPreviousResourceTags (stack tags)
     * and handlerRequest.getPreviousResourceState (resource tags).
     */
    private static <T> Map<String, String> getPreviouslyAttachedTags(final ResourceHandlerRequest<T> handlerRequest) {
        // get previous stack level tags from handlerRequest
        return handlerRequest.getPreviousResourceTags() != null
            ? ImmutableMap.copyOf(handlerRequest.getPreviousResourceTags())
            : Collections.emptyMap();
    }

    /*
     * If stack tags and resource tags are not merged together in Configuration class,
     * we need to get new user defined tags from both resource model and previous stack tags.
     */
    private static <T> Map<String, String> getNewDesiredTags(final ResourceHandlerRequest<T> handlerRequest) {
        // get new stack level tags from handlerRequest
        return handlerRequest.getDesiredResourceTags() != null
            ? ImmutableMap.copyOf(handlerRequest.getDesiredResourceTags())
            : Collections.emptyMap();
    }

    /**
     * Determines the tags to update.
     */
    public static <T> Map<String, String> generateTagsToAdd(final ResourceHandlerRequest<T> handlerRequest) {
        Map<String, String> desiredTags = getNewDesiredTags(handlerRequest);
        Map<String, String> previousTags = getPreviouslyAttachedTags(handlerRequest);
        return desiredTags.entrySet().stream()
            .filter(e -> !Objects.equals(previousTags.get(e.getKey()), e.getValue()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue));
    }

    /**
     * Determines the tags to be removed.
     */
    public static <T> Set<String> generateTagsToRemove(final ResourceHandlerRequest<T> handlerRequest) {
        final Set<String> desiredTagNames = getNewDesiredTags(handlerRequest).keySet();
        final Map<String, String> previousTags = getPreviouslyAttachedTags(handlerRequest);
        return previousTags.keySet().stream()
            .filter(tagName -> !desiredTagNames.contains(tagName))
            .collect(Collectors.toSet());
    }
}
