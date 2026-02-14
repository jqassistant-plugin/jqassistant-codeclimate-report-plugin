package org.jqassistant.plugin.codeclimate.report.api.impl.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class Issue {

    private final String type = "issue";

    private String description;

    @JsonProperty("check_name")
    private String checkName;

    private String fingerprint;

    private Severity severity;

    private Location location;

}
