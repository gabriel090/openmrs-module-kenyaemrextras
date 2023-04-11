/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemrextras.reporting.data.definition.evaluator.mortalityAuditTool;

import org.openmrs.annotation.Handler;
import org.openmrs.module.kenyaemrextras.reporting.data.definition.mortalityAuditTool.BaselineCD4DoneDataDefinition;
import org.openmrs.module.kenyaemrextras.reporting.data.definition.mortalityAuditTool.BaselineWHOStageDataDefinition;
import org.openmrs.module.reporting.data.person.EvaluatedPersonData;
import org.openmrs.module.reporting.data.person.definition.PersonDataDefinition;
import org.openmrs.module.reporting.data.person.evaluator.PersonDataEvaluator;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.querybuilder.SqlQueryBuilder;
import org.openmrs.module.reporting.evaluation.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Evaluates Baseline CD4 Done Data Definition
 */
@Handler(supports = BaselineCD4DoneDataDefinition.class, order = 50)
public class BaselineCD4DoneDataEvaluator implements PersonDataEvaluator {
	
	@Autowired
	private EvaluationService evaluationService;
	
	public EvaluatedPersonData evaluate(PersonDataDefinition definition, EvaluationContext context)
	        throws EvaluationException {
		EvaluatedPersonData c = new EvaluatedPersonData(definition, context);
		
		String qry = "select e.patient_id, if(l.patient_id is not null,'Yes','No') from kenyaemr_etl.etl_hiv_enrollment e left join\n"
		        + "(select patient_id,\n"
		        + "       mid(min(concat(coalesce(date(date_test_requested),date(visit_date)),\n"
		        + "                      if(lab_test = 5497, test_result, if(lab_test = 167718 and test_result = 1254, '>200', if(lab_test = 167718 and test_result = 167717,'<=200',if(lab_test = 730,concat(test_result,'%'),'')))), '')),\n"
		        + "           11) as baseline_cd4,\n"
		        + "        left(min(concat(coalesce(date(date_test_requested),date(visit_date)),\n"
		        + "                        if(lab_test = 5497, test_result, if(lab_test = 167718 and test_result = 1254, '>200', if(lab_test = 167718 and test_result = 167717,'<=200',if(lab_test = 730,concat(test_result,'%'),'')))), '')),\n"
		        + "             10)  as baseline_cd4_date\n"
		        + "from kenyaemr_etl.etl_laboratory_extract\n"
		        + "where lab_test in (167718,5497,730)\n" + "GROUP BY patient_id)l on e.patient_id = l.patient_id;";
		
		SqlQueryBuilder queryBuilder = new SqlQueryBuilder();
		queryBuilder.append(qry);
		Map<Integer, Object> data = evaluationService.evaluateToMap(queryBuilder, Integer.class, Object.class, context);
		c.setData(data);
		return c;
	}
}
