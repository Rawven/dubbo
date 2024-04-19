/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.protocol.tri.rest.cors;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpMethods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CorsMeta {

    // Default value

    public static final String ALL = "*";

    public static final List<String> ALL_LIST = Collections.singletonList(ALL);

    private static final OriginPattern ALL_PATTERN = new OriginPattern("*");

    private static final List<OriginPattern> ALL_PATTERN_LIST = Collections.singletonList(ALL_PATTERN);

    public static final List<String> DEFAULT_PERMIT_ALL = Collections.singletonList(ALL);

    public static final List<HttpMethods> DEFAULT_METHODS =
            Collections.unmodifiableList(Arrays.asList(HttpMethods.GET, HttpMethods.HEAD));

    public static final List<String> DEFAULT_PERMIT_METHODS = Collections.unmodifiableList(
            Arrays.asList(HttpMethods.GET.name(), HttpMethods.HEAD.name(), HttpMethods.POST.name()));

    public static final Long DEFAULT_MAX_AGE = 1800L;

    @Nullable
    private List<String> allowedOrigins;

    @Nullable
    private List<OriginPattern> allowedOriginPatterns;

    @Nullable
    private List<String> allowedMethods;

    @Nullable
    private List<HttpMethods> resolvedMethods = DEFAULT_METHODS;

    @Nullable
    private List<String> allowedHeaders;

    @Nullable
    private List<String> exposedHeaders;

    @Nullable
    private Boolean allowCredentials;

    @Nullable
    private Boolean allowPrivateNetwork;

    @Nullable
    private Long maxAge;

    public CorsMeta() {}

    public CorsMeta(CorsMeta other) {
        this.allowedOrigins = other.allowedOrigins;
        this.allowedOriginPatterns = other.allowedOriginPatterns;
        this.allowedMethods = other.allowedMethods;
        this.resolvedMethods = other.resolvedMethods;
        this.allowedHeaders = other.allowedHeaders;
        this.exposedHeaders = other.exposedHeaders;
        this.allowCredentials = other.allowCredentials;
        this.allowPrivateNetwork = other.allowPrivateNetwork;
        this.maxAge = other.maxAge;
    }

    public void setAllowedOrigins(@Nullable List<String> origins) {
        this.allowedOrigins = (origins == null
                ? null
                : origins.stream()
                        .filter(Objects::nonNull)
                        .map(this::trimTrailingSlash)
                        .collect(Collectors.toList()));
    }

    private String trimTrailingSlash(String origin) {
        return (origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin);
    }

    public List<String> getAllowedOrigins() {
        return this.allowedOrigins;
    }

    public void addAllowedOrigin(@Nullable String origin) {
        if (origin == null) {
            return;
        }
        if (this.allowedOrigins == null) {
            this.allowedOrigins = new ArrayList<>(4);
        } else if (this.allowedOrigins == DEFAULT_PERMIT_ALL && CollectionUtils.isEmpty(this.allowedOriginPatterns)) {
            setAllowedOrigins(DEFAULT_PERMIT_ALL);
        }
        origin = trimTrailingSlash(origin);
        this.allowedOrigins.add(origin);
    }

    public void setAllowedOriginPatterns(@Nullable List<String> allowedOriginPatterns) {
        if (allowedOriginPatterns == null) {
            this.allowedOriginPatterns = null;
        } else {
            this.allowedOriginPatterns = new ArrayList<>(allowedOriginPatterns.size());
            for (String patternValue : allowedOriginPatterns) {
                addAllowedOriginPattern(patternValue);
            }
        }
    }

    @Nullable
    public List<String> getAllowedOriginPatterns() {
        if (this.allowedOriginPatterns == null) {
            return null;
        }
        return this.allowedOriginPatterns.stream()
                .map(OriginPattern::getDeclaredPattern)
                .collect(Collectors.toList());
    }

    public void addAllowedOriginPattern(@Nullable String originPattern) {
        if (originPattern == null) {
            return;
        }
        if (this.allowedOriginPatterns == null) {
            this.allowedOriginPatterns = new ArrayList<>(4);
        }
        originPattern = trimTrailingSlash(originPattern);
        this.allowedOriginPatterns.add(new OriginPattern(originPattern));
        if (this.allowedOrigins == DEFAULT_PERMIT_ALL) {
            this.allowedOrigins = null;
        }
    }

    public void setAllowedMethods(@Nullable List<String> allowedMethods) {
        if (allowedMethods != null) {
            this.allowedMethods = new ArrayList<>(allowedMethods);
            setResolvedMethods(allowedMethods);
        } else {
            this.allowedMethods = null;
        }
    }

    @Nullable
    public void setResolvedMethods(List<String> allowedMethods) {
        if (!allowedMethods.isEmpty()) {
            this.resolvedMethods = new ArrayList<>(allowedMethods.size());
            for (String method : allowedMethods) {
                if (ALL.equals(method)) {
                    this.resolvedMethods = null;
                    break;
                }
                this.resolvedMethods.add(HttpMethods.valueOf(method));
            }
        } else {
            this.resolvedMethods = DEFAULT_METHODS;
        }
    }

    public List<String> getAllowedMethods() {
        return this.allowedMethods;
    }

    public void addAllowedMethod(HttpMethods method) {
        addAllowedMethod(method.name());
    }

    public void addAllowedMethod(String method) {
        if (StringUtils.hasText(method)) {
            if (this.allowedMethods == null) {
                this.allowedMethods = new ArrayList<>(4);
                this.resolvedMethods = new ArrayList<>(4);
            } else if (this.allowedMethods == DEFAULT_PERMIT_METHODS) {
                setAllowedMethods(DEFAULT_PERMIT_METHODS);
            }
            this.allowedMethods.add(method);
            if (ALL.equals(method)) {
                this.resolvedMethods = null;
            } else if (this.resolvedMethods != null) {
                this.resolvedMethods.add(HttpMethods.valueOf(method));
            }
        }
    }

    public void setAllowedHeaders(@Nullable List<String> allowedHeaders) {
        this.allowedHeaders = (allowedHeaders != null ? new ArrayList<>(allowedHeaders) : null);
    }

    public List<String> getAllowedHeaders() {
        return this.allowedHeaders;
    }

    public void addAllowedHeader(String allowedHeader) {
        if (this.allowedHeaders == null) {
            this.allowedHeaders = new ArrayList<>(4);
        } else if (this.allowedHeaders == DEFAULT_PERMIT_ALL) {
            setAllowedHeaders(DEFAULT_PERMIT_ALL);
        }
        this.allowedHeaders.add(allowedHeader);
    }

    public void setExposedHeaders(@Nullable List<String> exposedHeaders) {
        this.exposedHeaders = (exposedHeaders != null ? new ArrayList<>(exposedHeaders) : null);
    }

    public List<String> getExposedHeaders() {
        return this.exposedHeaders;
    }

    public void addExposedHeader(String exposedHeader) {
        if (this.exposedHeaders == null) {
            this.exposedHeaders = new ArrayList<>(4);
        }
        this.exposedHeaders.add(exposedHeader);
    }

    public void setAllowCredentials(@Nullable Boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    @Nullable
    public Boolean getAllowCredentials() {
        return this.allowCredentials;
    }

    public void setAllowPrivateNetwork(Boolean allowPrivateNetwork) {
        this.allowPrivateNetwork = allowPrivateNetwork;
    }

    public Boolean getAllowPrivateNetwork() {
        return this.allowPrivateNetwork;
    }

    public void setMaxAge(@Nullable Long maxAge) {
        this.maxAge = maxAge;
    }

    @Nullable
    public Long getMaxAge() {
        return this.maxAge;
    }

    public CorsMeta applyPermitDefaultValues() {
        if (this.allowedOrigins == null && this.allowedOriginPatterns == null) {
            this.allowedOrigins = DEFAULT_PERMIT_ALL;
        }
        if (this.allowedMethods == null) {
            this.allowedMethods = DEFAULT_PERMIT_METHODS;
            this.resolvedMethods =
                    DEFAULT_PERMIT_METHODS.stream().map(HttpMethods::valueOf).collect(Collectors.toList());
        }
        if (this.allowedHeaders == null) {
            this.allowedHeaders = DEFAULT_PERMIT_ALL;
        }
        if (this.maxAge == null) {
            this.maxAge = DEFAULT_MAX_AGE;
        }
        if (this.allowCredentials == null) {
            this.allowCredentials = false;
        }
        if (this.allowPrivateNetwork == null) {
            this.allowPrivateNetwork = false;
        }
        return this;
    }

    public boolean validateAllowCredentials() {
        //      When allowCredentials is true, allowedOrigins cannot contain the special value \"*\"
        //      since that cannot be set on the \"Access-Control-Allow-Origin\" response header.
        //      To allow credentials to a set of origins, list them explicitly
        //      or consider using \"allowedOriginPatterns\" instead.
        return this.allowCredentials != null
                && this.allowCredentials.equals(Boolean.TRUE)
                && this.allowedOrigins != null
                && this.allowedOrigins.contains(ALL);
    }

    public boolean validateAllowPrivateNetwork() {

        //       When allowPrivateNetwork is true, allowedOrigins cannot contain the special value \"*\"
        //       as it is not recommended from a security perspective.
        //       To allow private network access to a set of origins, list them explicitly
        //       or consider using \"allowedOriginPatterns\" instead.
        return this.allowPrivateNetwork != null
                && this.allowPrivateNetwork.equals(Boolean.TRUE)
                && this.allowedOrigins != null
                && this.allowedOrigins.contains(ALL);
    }

    /**
     * Combines two lists of strings, with a priority on this
     * @param other other
     * @return {@link CorsMeta}
     */
    public static CorsMeta combine(@Nullable CorsMeta priority, @Nullable CorsMeta other) {
        if (priority == null && other == null) {
            return new CorsMeta();
        }
        if (priority == null) {
            return other;
        }
        if (other == null) {
            return priority;
        }
        CorsMeta config = new CorsMeta(priority);
        List<String> origins = priority.combine(priority.getAllowedOrigins(), other.getAllowedOrigins());
        List<OriginPattern> patterns =
                priority.combinePatterns(priority.allowedOriginPatterns, other.allowedOriginPatterns);
        config.allowedOrigins = (origins == DEFAULT_PERMIT_ALL && !CollectionUtils.isEmpty(patterns) ? null : origins);
        config.allowedOriginPatterns = patterns;
        config.setAllowedMethods(priority.combine(priority.getAllowedMethods(), other.getAllowedMethods()));
        config.setAllowedHeaders(priority.combine(priority.getAllowedHeaders(), other.getAllowedHeaders()));
        config.setExposedHeaders(priority.combine(priority.getExposedHeaders(), other.getExposedHeaders()));
        if (priority.allowCredentials == null) {
            config.setAllowCredentials(other.getAllowCredentials());
        }
        if (priority.allowPrivateNetwork == null) {
            config.setAllowPrivateNetwork(other.allowPrivateNetwork);
        }
        if (priority.maxAge == null) {
            config.setMaxAge(other.maxAge);
        }
        return config;
    }

    /**
     * Combines two lists of strings, with a priority on the source list.
     *
     * @param source The primary list of strings.
     * @param other  The secondary list of strings to be combined with the source.
     * @return A combined list of strings, with the source list taking priority in case of conflicts.
     */
    private List<String> combine(@Nullable List<String> source, @Nullable List<String> other) {
        if (other == null) {
            return (source != null ? source : Collections.emptyList());
        }
        if (source == null) {
            return other;
        }
        // save setting value at first
        if (source == DEFAULT_PERMIT_ALL
                || source == DEFAULT_PERMIT_METHODS
                || other == DEFAULT_PERMIT_ALL
                || other == DEFAULT_PERMIT_METHODS) {
            return source;
        }

        if (source.contains(ALL) || other.contains(ALL)) {
            return ALL_LIST;
        }
        Set<String> combined = new LinkedHashSet<>(source.size() + other.size());
        combined.addAll(source);
        combined.addAll(other);
        return new ArrayList<>(combined);
    }

    private List<OriginPattern> combinePatterns(
            @Nullable List<OriginPattern> source, @Nullable List<OriginPattern> other) {

        if (other == null) {
            return (source != null ? source : Collections.emptyList());
        }
        if (source == null) {
            return other;
        }
        if (source.contains(ALL_PATTERN) || other.contains(ALL_PATTERN)) {
            return ALL_PATTERN_LIST;
        }
        Set<OriginPattern> combined = new LinkedHashSet<>(source.size() + other.size());
        combined.addAll(source);
        combined.addAll(other);
        return new ArrayList<>(combined);
    }

    public String checkOrigin(@Nullable String origin) {
        if (!StringUtils.hasText(origin)) {
            return null;
        }
        String originToCheck = trimTrailingSlash(origin);
        if (!CollectionUtils.isEmpty(this.allowedOrigins)) {
            if (this.allowedOrigins.contains(ALL)) {
                return allOriginAllowed();
            }
            if (isOriginAllowed(originToCheck)) {
                return origin;
            }
        }
        if (isOriginPatternAllowed(originToCheck)) {
            return origin;
        }
        return null;
    }

    private String allOriginAllowed() {
        return (validateAllowCredentials() || validateAllowPrivateNetwork() ? null : ALL);
    }

    private boolean isOriginAllowed(String originToCheck) {
        for (String allowedOrigin : this.allowedOrigins) {
            if (originToCheck.equalsIgnoreCase(allowedOrigin)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOriginPatternAllowed(String originToCheck) {
        if (!CollectionUtils.isEmpty(this.allowedOriginPatterns)) {
            for (OriginPattern p : this.allowedOriginPatterns) {
                if (p.getDeclaredPattern().equals(ALL)
                        || p.getPattern().matcher(originToCheck).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public List<HttpMethods> checkHttpMethods(@Nullable HttpMethods requestMethod) {
        if (requestMethod == null) {
            return null;
        }
        if (this.resolvedMethods == null) {
            return Collections.singletonList(requestMethod);
        }
        return (this.resolvedMethods.contains(requestMethod) ? this.resolvedMethods : null);
    }

    @Nullable
    public List<String> checkHeaders(@Nullable List<String> requestHeaders) {
        if (requestHeaders == null) {
            return null;
        }
        if (requestHeaders.isEmpty()) {
            return Collections.emptyList();
        }
        if (CollectionUtils.isEmpty(this.allowedHeaders)) {
            return null;
        }
        boolean allowAnyHeader = this.allowedHeaders.contains(ALL);
        List<String> result = new ArrayList<>(requestHeaders.size());
        loadAllowedHeaders(requestHeaders, result, allowAnyHeader);
        return (result.isEmpty() ? null : result);
    }

    private void loadAllowedHeaders(List<String> requestHeaders, List<String> result, boolean allowAnyHeader) {
        for (String requestHeader : requestHeaders) {
            if (StringUtils.hasText(requestHeader)) {
                requestHeader = requestHeader.trim();
                if (allowAnyHeader) {
                    result.add(requestHeader);
                } else {
                    loadAllowedHeaders(result, requestHeader);
                }
            }
        }
    }

    private void loadAllowedHeaders(List<String> result, String requestHeader) {
        for (String allowedHeader : this.allowedHeaders) {
            if (requestHeader.equalsIgnoreCase(allowedHeader)) {
                result.add(requestHeader);
                break;
            }
        }
    }

    private static class OriginPattern {

        private static final Pattern PORTS_PATTERN = Pattern.compile("(.*):\\[(\\*|\\d+(,\\d+)*)]");

        private final String declaredPattern;

        private final Pattern pattern;

        OriginPattern(String declaredPattern) {
            this.declaredPattern = declaredPattern;
            this.pattern = initPattern(declaredPattern);
        }

        private static Pattern initPattern(String patternValue) {
            String portList = null;
            Matcher matcher = PORTS_PATTERN.matcher(patternValue);
            if (matcher.matches()) {
                patternValue = matcher.group(1);
                portList = matcher.group(2);
            }

            patternValue = "\\Q" + patternValue + "\\E";
            patternValue = patternValue.replace("*", "\\E.*\\Q");

            if (portList != null) {
                patternValue += (portList.equals(ALL) ? "(:\\d+)?" : ":(" + portList.replace(',', '|') + ")");
            }

            return Pattern.compile(patternValue);
        }

        public String getDeclaredPattern() {
            return this.declaredPattern;
        }

        public Pattern getPattern() {
            return this.pattern;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || !getClass().equals(other.getClass())) {
                return false;
            }
            return Objects.equals(this.declaredPattern, ((OriginPattern) other).declaredPattern);
        }

        @Override
        public int hashCode() {
            return this.declaredPattern.hashCode();
        }

        @Override
        public String toString() {
            return this.declaredPattern;
        }
    }
}