package org.jqassistant.plugin.codeclimate.report.impl;

import org.jqassistant.plugin.codeclimate.report.api.impl.model.Severity;
import org.mapstruct.Mapper;

@Mapper
public interface SeverityMapper {

    Severity toReport(com.buschmais.jqassistant.core.rule.api.model.Severity severity);

}
