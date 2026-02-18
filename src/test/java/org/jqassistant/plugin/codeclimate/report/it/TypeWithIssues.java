package org.jqassistant.plugin.codeclimate.report.it;

import com.buschmais.jqassistant.plugin.java.annotation.jQASuppress;

public class TypeWithIssues {

    private static String issueField;

    public void issueMethod() {
        System.out.println("issueMethod");
    }

    @jQASuppress(value = { "codeclimate-report-it:ConstraintWithFailures", "codeclimate-report-it:ConstraintWithWarnings" })
    public void suppressedIssueMethod() {
        System.out.println("suppressedIssueMethod");

    }

    public void nonIssueMethod() {
        System.out.println("nonIssueMethod");
    }
}
