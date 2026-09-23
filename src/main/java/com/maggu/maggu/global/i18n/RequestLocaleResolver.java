package com.maggu.maggu.global.i18n;

import com.maggu.maggu.global.entity.enums.AppLocale;
import com.maggu.maggu.global.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RequestLocaleResolver {

    public AppLocale resolve(String lang) {
        if (StringUtils.hasText(lang)) {
            return AppLocale.from(lang);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getAppUser().getLocale();
        }
        return AppLocale.defaultLocale();
    }
}
