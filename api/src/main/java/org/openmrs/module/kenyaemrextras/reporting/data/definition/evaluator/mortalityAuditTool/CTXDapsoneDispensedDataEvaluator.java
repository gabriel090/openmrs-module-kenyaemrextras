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
import org.openmrs.module.kenyaemrextras.reporting.data.definition.mortalityAuditTool.CTXDapsoneDispensedDataDefinition;
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
 * Evaluates CTXDapsoneDispensedDataDefinition
 */
@Handler(supports = CTXDapsoneDispensedDataDefinition.class, order = 50)
public class CTXDapsoneDispensedDataEvaluator implements PersonDataEvaluator {
	
	@Autowired
	private EvaluationService evaluationService;
	
	public EvaluatedPersonData evaluate(PersonDataDefinition definition, EvaluationContext context)
	        throws EvaluationException {
		EvaluatedPersonData c = new EvaluatedPersonData(definition, context);
		
		String qry = "select a.patient_id,\n"
		        + "       case a.ctx_dap_dispensed\n"
		        + "           when 1065 then 'Yes'\n"
		        + "           when 105281 then 'Yes'\n"
		        + "           when 1066 then 'No'\n"
		        + "           when 1075 then 'NA' END as ctx_dap_dispensed\n"
		        + "from (select f.patient_id                                                                              as patient_id,\n"
		        + "             min(f.visit_date)                                                                         as fup_date,\n"
		        + "             mid(min(concat(date(f.visit_date), coalesce(f.ctx_dispensed, f.dapsone_dispensed))),\n"
		        + "                 11)                                                                                   as ctx_dap_dispensed,\n"
		        + "             left(min(concat(date(f.visit_date), coalesce(f.ctx_dispensed, f.dapsone_dispensed))), 10) as ctx_dap_date,\n"
		        + "             l.baseline_cd4_results_date,\n"
		        + "             l.baseline_cd4\n"
		        + "      from kenyaemr_etl.etl_patient_hiv_followup f\n"
		        + "               left join\n"
		        + "           (select patient_id,\n"
		        + "                   left(min(concat(coalesce(date(date_test_result_received), date(visit_date)),\n"
		        + "                                   if(lab_test = 5497, test_result, if(lab_test = 167718 and test_result = 1254, '>200',\n"
		        + "                                                                       if(lab_test = 167718 and test_result = 167717, '<=200', ''))),\n"
		        + "                                   '')),\n"
		        + "                        10) as baseline_cd4_results_date,\n"
		        + "                   mid(min(concat(coalesce(date(date_test_requested), date(visit_date)),\n"
		        + "                                  if(lab_test = 5497, test_result, if(lab_test = 167718 and test_result = 1254, '>200',\n"
		        + "                                                                      if(lab_test = 167718 and test_result = 167717,\n"
		        + "                                                                         '<=200',\n"
		        + "                                                                         if(lab_test = 730, concat(test_result, '%'), '')))),\n"
		        + "                                  '')),\n" + "                       11)  as baseline_cd4\n"
		        + "            from kenyaemr_etl.etl_laboratory_extract\n"
		        + "            where lab_test in (167718, 5497)\n"
		        + "            GROUP BY patient_id) l on f.patient_id = l.patient_id\n" + "      group by f.patient_id) a\n"
		        + "where a.fup_date >= a.baseline_cd4_results_date\n" + "    and a.baseline_cd4 = '<=200'\n"
		        + "   or a.baseline_cd4 <= 200;";
		
		SqlQueryBuilder queryBuilder = new SqlQueryBuilder();
		queryBuilder.append(qry);
		Map<Integer, Object> data = evaluationService.evaluateToMap(queryBuilder, Integer.class, Object.class, context);
		c.setData(data);
		return c;
	}
}
