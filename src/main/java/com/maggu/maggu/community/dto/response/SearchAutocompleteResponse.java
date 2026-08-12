package com.maggu.maggu.community.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SearchAutocompleteResponse {
    private List<String> keywords;
}