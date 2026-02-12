package org.jqassistant.plugin.codeclimate.report.impl;

import com.buschmais.jqassistant.core.report.api.ReportPlugin;
import com.buschmais.jqassistant.core.report.api.ReportPlugin.Default;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.model.Constraint;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jqassistant.plugin.codeclimate.report.api.impl.model.CodeClimateReport;
import org.mapstruct.factory.Mappers;

import static com.buschmais.jqassistant.core.report.api.model.Result.Status.FAILURE;

@Default
public class CodeClimateReportPlugin implements ReportPlugin {

    private static final SeverityMapper SEVERITY_MAPPER = Mappers.getMapper(SeverityMapper.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void setResult(Result<? extends ExecutableRule> result) {
        if (FAILURE.equals(result.getStatus())) {
            ExecutableRule executableRule = result.getRule();
            if (executableRule instanceof Constraint) {
                Constraint constraint = (Constraint) executableRule;
                CodeClimateReport.CodeClimateReportBuilder reportBuilder = CodeClimateReport.builder()
                        .checkName("[jQAssistant] " + constraint.getId())
                        .description(constraint.getDescription());
            }
        }
    }
}
