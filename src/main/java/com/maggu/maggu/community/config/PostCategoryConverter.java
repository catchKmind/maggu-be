package com.maggu.maggu.community.config;

import com.maggu.maggu.community.entity.PostCategory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class PostCategoryConverter implements Converter<String, PostCategory> {

    @Override
    public PostCategory convert(String source) {
        return PostCategory.from(source);
    }
}
