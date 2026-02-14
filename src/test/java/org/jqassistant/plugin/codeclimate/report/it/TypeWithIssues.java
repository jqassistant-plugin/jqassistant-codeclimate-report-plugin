package org.jqassistant.plugin.codeclimate.report.it;

import com.buschmais.jqassistant.plugin.java.annotation.jQASuppress;

public class TypeWithIssues {

    private static String issueField;

    public void issueMethod() {
        System.out.println("issueMethod");
    }

    @jQASuppress("codeclimate-report-it:ConstraintWithIssue")
    public void suppressedIssueMethod() {
        System.out.println("suppressedIssueMethod");

    }

    public void nonIssueMethod() {
        System.out.println("nonIssueMethod");
    }
}
