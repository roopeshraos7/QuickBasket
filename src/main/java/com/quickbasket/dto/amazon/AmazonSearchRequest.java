package com.quickbasket.dto.amazon;

import java.util.List;

/**
 * LowerCamelCase request payload for Amazon Creators API SearchItems (/catalog/v1/searchItems).
 */
public record AmazonSearchRequest(
        String keywords,
        String partnerTag,
        String marketplace,
        Integer itemCount,
        List<String> resources
) {}
