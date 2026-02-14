package org.jqassistant.plugin.codeclimate.report.it;

import java.io.File;
import java.util.Map;

import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.model.Constraint;
import com.buschmais.jqassistant.core.rule.api.model.RuleException;
import com.buschmais.jqassistant.plugin.java.test.AbstractJavaPluginIT;

import org.junit.jupiter.api.Test;

import static com.buschmais.jqassistant.core.report.api.model.Result.Status.FAILURE;
import static org.assertj.core.api.Assertions.assertThat;

public class CodeClimateReportIT extends AbstractJavaPluginIT {

    @Test
    void constraintWithIssues() throws RuleException {
        scanClassPathDirectory(getClassesDirectory(TypeWithIssues.class));
        Result<Constraint> result = validateConstraint("codeclimate-report-it:ConstraintWithIssues", Map.of("fqn", TypeWithIssues.class.getName()));

        assertThat(result.getStatus()).isEqualTo(FAILURE);

        File codeClimateReport = new File("target/jqassistant/report/codeclimate/jqassistant-codeclimate.json");
        assertThat(codeClimateReport).exists();
    }

}
