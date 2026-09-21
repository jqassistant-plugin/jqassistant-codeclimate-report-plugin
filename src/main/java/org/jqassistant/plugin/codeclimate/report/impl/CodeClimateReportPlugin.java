package org.jqassistant.plugin.codeclimate.report.impl;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.buschmais.jqassistant.core.report.api.ReportContext;
import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.ReportPlugin;
import com.buschmais.jqassistant.core.report.api.ReportPlugin.Default;
import com.buschmais.jqassistant.core.report.api.model.Column;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.report.api.model.Row;
import com.buschmais.jqassistant.core.report.api.model.source.FileLocation;
import com.buschmais.jqassistant.core.rule.api.model.Constraint;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jqassistant.plugin.codeclimate.report.api.impl.model.Issue;
import org.jqassistant.plugin.codeclimate.report.api.impl.model.Location;
import org.mapstruct.factory.Mappers;

import static com.buschmais.jqassistant.core.report.api.model.Result.Status.FAILURE;
import static com.buschmais.jqassistant.core.report.api.model.Result.Status.WARNING;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.stream.Collectors.joining;

@Default
@Slf4j
public class CodeClimateReportPlugin implements ReportPlugin {

    public static final String REPORT_DIRECTORY = "codeclimate";

    public static final String REPORT_FILE = "jqassistant-codeclimate-report.json";

    private static final Location DEFAULT_LOCATION = Location.builder()
        .path(".jqassistant.yml")
        .lines(Location.Lines.builder()
            .begin(1)
            .end(1)
            .build())
        .build();

    private static final SeverityMapper SEVERITY_MAPPER = Mappers.getMapper(SeverityMapper.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setDefaultPropertyInclusion(NON_NULL);

    private ReportContext reportContext;

    private List<Issue> issues;

    @Override
    public void configure(ReportContext reportContext, Map<String, Object> properties) {
        this.reportContext = reportContext;
    }

    @Override
    public void begin() {
        issues = new LinkedList<>();
    }

    @Override
    public void setResult(Result<? extends ExecutableRule> result) {
        Result.Status status = result.getStatus();
        if (FAILURE.equals(status) || WARNING.equals(status)) {
            ExecutableRule<?> executableRule = result.getRule();
            Constraint constraint = (Constraint) executableRule;
            for (Row row : result.getRows()) {
                if (!row.isHidden()) {
                    issues.add(getIssue(result, constraint, row));
                }
            }
        }
    }

    @Override
    public void end() throws ReportException {
        File reportDirectory = reportContext.getReportDirectory(REPORT_DIRECTORY);
        try {
            File file = new File(reportDirectory, REPORT_FILE).getCanonicalFile();
            log.info("Writing CodeClimate report to {}.", file);
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                .writeValue(file, issues);
        } catch (IOException e) {
            throw new ReportException("Failed to write CodeClimate report file.", e);
        }
    }

    private Issue getIssue(Result<? extends ExecutableRule> result, Constraint constraint, Row row) {
        Issue.IssueBuilder issueBuilder = Issue.builder()
            .checkName("[jQAssistant]" + constraint.getId())
            .severity(SEVERITY_MAPPER.toReport(constraint.getSeverity()))
            .fingerprint(row.getKey());
        StringBuilder description = new StringBuilder(constraint.getDescription());
        String columnsValues = row.getColumns()
            .entrySet()
            .stream()
            .map(entry -> entry.getKey() + "='" + entry.getValue()
                .getLabel() + "'")
            .collect(joining(", "));
        if (!columnsValues.isEmpty()) {
            description.append(" | ")
                .append(columnsValues);
        }
        issueBuilder.description(description.toString());
        issueBuilder.location(getLocation(result, row));
        return issueBuilder.build();
    }

    private Location getLocation(Result<? extends ExecutableRule> result, Row row) {
        return result.getPrimaryColumn()
            .map(primaryColumnName -> row.getColumns()
                .get(primaryColumnName))
            .flatMap(Column::getSourceLocation)
            .filter(location -> location instanceof FileLocation)
            .map(location -> (FileLocation) location)
            .filter(location -> location.getPath() != null)
            .map(this::getLocation)
            .orElse(DEFAULT_LOCATION);
    }

    private Location getLocation(FileLocation fileLocation) {
        return Location.builder()
            .path(fileLocation.getPath())
            .lines(Location.Lines.builder()
                .begin(fileLocation.getStartLine()
                    .orElse(1))
                .end(fileLocation.getEndLine()
                    .orElse(1))
                .build())
            .build();
    }
}
