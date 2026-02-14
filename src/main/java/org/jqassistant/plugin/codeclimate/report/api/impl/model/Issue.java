package org.jqassistant.plugin.codeclimate.report.api.impl.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class Issue {

    private final String type = "issue";

    private String description;

    private String checkName;

    private String fingerprint;

    private Severity severity;

    private Location location;

}
