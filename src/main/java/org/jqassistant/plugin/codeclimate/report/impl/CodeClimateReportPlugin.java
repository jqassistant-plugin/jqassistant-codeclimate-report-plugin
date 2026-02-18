package org.jqassistant.plugin.codeclimate.report.impl;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.buschmais.jqassistant.core.report.api.ReportContext;
import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.ReportPlugin;
import com.buschmais.jqassistant.core.report.api.ReportPlugin.Default;
import com.buschmais.jqassistant.core.report.api.model.Column;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.report.api.model.Row;
import com.buschmais.jqassistant.core.report.api.model.source.FileLocation;
import com.buschmais.jqassistant.core.report.api.model.source.SourceLocation;
import com.buschmais.jqassistant.core.rule.api.model.Constraint;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jqassistant.plugin.codeclimate.report.api.impl.model.Issue;
import org.jqassistant.plugin.codeclimate.report.api.impl.model.Location;
import org.mapstruct.factory.Mappers;

import static com.buschmais.jqassistant.core.report.api.model.Result.Status.FAILURE;
import static com.buschmais.jqassistant.core.report.api.model.Result.Status.WARNING;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.joining;

@Default
@Slf4j
public class CodeClimateReportPlugin implements ReportPlugin {

    public static final String REPORT_DIRECTORY = "codeclimate";

    public static final String REPORT_FILE = "jqassistant-codeclimate-report.json";

    private static final SeverityMapper SEVERITY_MAPPER = Mappers.getMapper(SeverityMapper.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(INDENT_OUTPUT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

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
            OBJECT_MAPPER.writeValue(file, issues);
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
        getLocation(result, row).ifPresent(issueBuilder::location);
        return issueBuilder.build();
    }

    private Optional<Location> getLocation(Result<? extends ExecutableRule> result, Row row) {
        Optional<String> primaryColumnName = result.getPrimaryColumn();
        if (primaryColumnName.isPresent()) {
            Column<?> column = row.getColumns()
                    .get(primaryColumnName.get());
            Optional<SourceLocation<?>> optionalSourceLocation = column.getSourceLocation();
            if (optionalSourceLocation.isPresent()) {
                SourceLocation<?> sourceLocation = optionalSourceLocation.get();
                Location.LocationBuilder locationBuilder = Location.builder()
                        .path(sourceLocation.getFileName());
                if (sourceLocation instanceof FileLocation) {
                    FileLocation fileLocation = (FileLocation) sourceLocation;
                    fileLocation.getStartLine()
                            .ifPresent(startLine -> {
                                Location.Lines.LinesBuilder linesBuilder = Location.Lines.builder()
                                        .begin(startLine);
                                fileLocation.getEndLine()
                                        .ifPresent(linesBuilder::end);
                                locationBuilder.lines(linesBuilder.build());
                            });
                }
                return Optional.of(locationBuilder.build());
            }
        }
        return empty();
    }

    /**
     * TODO: Move to AnalyzerContext#toRow
     * <p>
     * Determine the primary column for a rule, i.e. the colum used by tools like
     * SonarQube to attach issues.
     *
     * @param rule
     *         The {@link ExecutableRule}.
     * @param columnNames
     *         The column names returned by the executed rule.
     * @return The name of the primary column.
     */
    private String getPrimaryColumn(ExecutableRule<?> rule, List<String> columnNames) {
        if (columnNames == null || columnNames.isEmpty()) {
            return null;
        }
        String primaryColumn = rule.getReport()
                .getPrimaryColumn();
        String firstColumn = columnNames.get(0);
        if (primaryColumn == null) {
            // primary column not explicitly specifed by the rule, so take the first column by default.
            return firstColumn;
        }
        if (!columnNames.contains(primaryColumn)) {
            log.warn("Rule '{}' defines primary column '{}' which is not provided by the result (available columns: {}). Falling back to '{}'.", rule,
                    primaryColumn, columnNames, firstColumn);
            primaryColumn = firstColumn;
        }
        return primaryColumn;
    }

}
