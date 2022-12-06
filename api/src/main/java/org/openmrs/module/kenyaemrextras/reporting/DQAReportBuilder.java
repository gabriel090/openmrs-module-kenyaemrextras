/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemrextras.reporting;

import org.openmrs.PatientIdentifierType;
import org.openmrs.module.kenyacore.report.HybridReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportUtils;
import org.openmrs.module.kenyacore.report.builder.AbstractHybridReportBuilder;
import org.openmrs.module.kenyacore.report.builder.Builds;
import org.openmrs.module.kenyacore.report.data.patient.definition.CalculationDataDefinition;
import org.openmrs.module.kenyaemr.calculation.library.hiv.DateConfirmedHivPositiveCalculation;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.art.ETLArtStartDateDataDefinition;
import org.openmrs.module.kenyaemrextras.reporting.cohort.definition.DQAActiveCohortDefinition;
import org.openmrs.module.kenyaemrextras.reporting.cohort.definition.DQADuplicateActiveCohortDefinition;
import org.openmrs.module.kenyaemrextras.reporting.data.definition.*;
import org.openmrs.module.kenyaemrextras.reporting.data.definition.converter.DQADefaultDataCompletenessDataConverter;
import org.openmrs.module.kenyaemrextras.reporting.data.definition.converter.DQADefaultYesDataConverter;
import org.openmrs.module.kenyaemrextras.reporting.data.definition.converter.DQAIdentifierCompletenessDataConverter;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.common.SortCriteria;
import org.openmrs.module.reporting.data.DataDefinition;
import org.openmrs.module.reporting.data.converter.BirthdateConverter;
import org.openmrs.module.reporting.data.converter.DataConverter;
import org.openmrs.module.reporting.data.converter.DateConverter;
import org.openmrs.module.reporting.data.converter.ObjectFormatter;
import org.openmrs.module.reporting.data.patient.definition.ConvertedPatientDataDefinition;
import org.openmrs.module.reporting.data.patient.definition.PatientIdentifierDataDefinition;
import org.openmrs.module.reporting.data.person.definition.BirthdateDataDefinition;
import org.openmrs.module.reporting.data.person.definition.ConvertedPersonDataDefinition;
import org.openmrs.module.reporting.data.person.definition.GenderDataDefinition;
import org.openmrs.module.reporting.data.person.definition.PersonIdDataDefinition;
import org.openmrs.module.reporting.data.person.definition.PreferredNameDataDefinition;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.PatientDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@Builds({ "kenyaemr.extras.report.dqaReport" })
public class DQAReportBuilder extends AbstractHybridReportBuilder {
	
	public static final String DATE_FORMAT = "dd/MM/yyyy";
	
	/**
	 * @see org.openmrs.module.kenyacore.report.builder.AbstractCohortReportBuilder#addColumns(org.openmrs.module.kenyacore.report.CohortReportDescriptor,
	 *      PatientDataSetDefinition)
	 */
	@Override
	protected void addColumns(HybridReportDescriptor report, PatientDataSetDefinition dsd) {
		
	}
	
	@Override
	protected List<Parameter> getParameters(ReportDescriptor reportDescriptor) {
		return Arrays.asList(new Parameter("startDate", "Start Date", Date.class), new Parameter("endDate", "End Date",
		        Date.class), new Parameter("dateBasedReporting", "", String.class));
	}
	
	@Override
	protected Mapped<CohortDefinition> buildCohort(HybridReportDescriptor descriptor, PatientDataSetDefinition dsd) {
		return null;
	}
	
	protected Mapped<CohortDefinition> activePatientsCohort() {
		CohortDefinition cd = new DQAActiveCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setName("DQA Active Patients");
		return ReportUtils.map(cd, "startDate=${startDate},endDate=${endDate}");
	}
	
	protected Mapped<CohortDefinition> activeDuplicatePatientsCohort() {
		CohortDefinition cd = new DQADuplicateActiveCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setName("DQA Active Patients");
		return ReportUtils.map(cd, "startDate=${startDate},endDate=${endDate}");
	}
	
	@Override
	protected List<Mapped<DataSetDefinition>> buildDataSets(ReportDescriptor descriptor, ReportDefinition report) {
		
		PatientDataSetDefinition activePatients = rdqaActiveDataSetDefinition("activePatients");
		activePatients.addRowFilter(activePatientsCohort());
		DataSetDefinition activePatientsDSD = activePatients;
		
		PatientDataSetDefinition dqaPatients = dqaActiveDataSetVariablesDefinition("activePatientsDqa");
		dqaPatients.addRowFilter(activeDuplicatePatientsCohort());
		DataSetDefinition dqaPatientsDSD = dqaPatients;
		
		return Arrays.asList(ReportUtils.map(activePatientsDSD, "startDate=${startDate},endDate=${endDate}"),
		    ReportUtils.map(dqaPatientsDSD, "startDate=${startDate},endDate=${endDate}"));
		
	}
	
	protected PatientDataSetDefinition rdqaActiveDataSetDefinition(String datasetName) {
		
		PatientDataSetDefinition dsd = new PatientDataSetDefinition(datasetName);
		String indParams = "startDate=${startDate},endDate=${endDate}";
		
		dsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		dsd.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		PatientIdentifierType upn = MetadataUtils.existing(PatientIdentifierType.class,
		    HivMetadata._PatientIdentifierType.UNIQUE_PATIENT_NUMBER);
		DataConverter identifierFormatter = new ObjectFormatter("{identifier}");
		DataDefinition identifierDef = new ConvertedPatientDataDefinition("identifier", new PatientIdentifierDataDefinition(
		        upn.getName(), upn), identifierFormatter);
		
		DataConverter nameFormatter = new ObjectFormatter("{familyName}, {givenName}");
		DataDefinition nameDef = new ConvertedPersonDataDefinition("name", new PreferredNameDataDefinition(), nameFormatter);
		dsd.addColumn("id", new PersonIdDataDefinition(), "");
		dsd.addColumn("Name", nameDef, "");
		dsd.addColumn("CCC No", identifierDef, "");
		dsd.addColumn("CCC No Format", identifierDef, "");
		dsd.addColumn("Sex", new GenderDataDefinition(), "", null);
		dsd.addColumn("Date of Birth", new BirthdateDataDefinition(), "", new BirthdateConverter(DATE_FORMAT));
		dsd.addSortCriteria("Category", SortCriteria.SortDirection.ASC);
		
		DQAWeightDataDefinition weightDataDefinition = new DQAWeightDataDefinition();
		weightDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		weightDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("Weight", weightDataDefinition, indParams, null);
		
		DQAHeightDataDefinition heightDataDefinition = new DQAHeightDataDefinition();
		heightDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		heightDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("Height", heightDataDefinition, indParams, null);
		
		DQAARTInitiationDateDataDefinition artInitiationDataDefinition = new DQAARTInitiationDateDataDefinition();
		artInitiationDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		artInitiationDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("ART Initiation date", artInitiationDataDefinition, indParams, new DateConverter(DATE_FORMAT));
		
		DQACurrentRegimenDataDefinition currentRegimenDataDefinition = new DQACurrentRegimenDataDefinition();
		currentRegimenDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		currentRegimenDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("Current ART Regimen", currentRegimenDataDefinition, indParams, null);
		
		LastAppointmentPeriodDataDefinition lastAppointmentPeriodDataDefinition = new LastAppointmentPeriodDataDefinition();
		lastAppointmentPeriodDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		lastAppointmentPeriodDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("Drug dosage given (Duration)", lastAppointmentPeriodDataDefinition, indParams, null);
		
		DQATBScreeningStatusLastVisitDataDefinition tbScreeningStatusDataDefinition = new DQATBScreeningStatusLastVisitDataDefinition();
		tbScreeningStatusDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		tbScreeningStatusDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("TB Screening status", tbScreeningStatusDataDefinition, indParams, null);
		
		EverOnIPTDataDefinition everOnIPTDataDefinition = new EverOnIPTDataDefinition();
		everOnIPTDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		everOnIPTDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("TPT/IPT initiated", everOnIPTDataDefinition, indParams, null);
		
		LastNutritionAssessmentDataDefinition lastNutritionAssessmentDataDefinition = new LastNutritionAssessmentDataDefinition();
		lastNutritionAssessmentDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		lastNutritionAssessmentDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("Nutrition Assessment done", lastNutritionAssessmentDataDefinition, indParams, null);
		
		LastDSDModelDataDefinition lastDSDModelDataDefinition = new LastDSDModelDataDefinition();
		lastDSDModelDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		lastDSDModelDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("DSD model", lastDSDModelDataDefinition, indParams, null);
		
		DQALastVLDateDataDefinition lastVLDateDataDefinition = new DQALastVLDateDataDefinition();
		lastVLDateDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		lastVLDateDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("Latest VL result documented", lastVLDateDataDefinition, indParams, null);
		
		DQALastVisitDataDefinition lastVisitDateDataDefinition = new DQALastVisitDataDefinition();
		lastVisitDateDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		lastVisitDateDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("Date of last appointment", lastVisitDateDataDefinition, indParams, null);
		
		DQACohortCategoryDataDefinition cohortCategoryDataDefinition = new DQACohortCategoryDataDefinition();
		cohortCategoryDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cohortCategoryDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		dsd.addColumn("Category", cohortCategoryDataDefinition, indParams, null);
		
		DQANupiDataDefinition nupiDataDefinition = new DQANupiDataDefinition();
		nupiDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		nupiDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("NUPI", nupiDataDefinition, indParams);
		dsd.addColumn("Date confirmed positive", new CalculationDataDefinition("Date confirmed positive",
		        new DateConfirmedHivPositiveCalculation()), "");
		
		DQABaselineCD4DataDefinition baselineCD4DataDefinition = new DQABaselineCD4DataDefinition();
		baselineCD4DataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		baselineCD4DataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		dsd.addColumn("Baseline CD4", baselineCD4DataDefinition, indParams);
		return dsd;
	}
	
	protected PatientDataSetDefinition dqaActiveDataSetVariablesDefinition(String datasetName) {
		
		PatientDataSetDefinition dsd = new PatientDataSetDefinition(datasetName);
		String indParams = "startDate=${startDate},endDate=${endDate}";
		
		dsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		dsd.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		PatientIdentifierType upn = MetadataUtils.existing(PatientIdentifierType.class,
		    HivMetadata._PatientIdentifierType.UNIQUE_PATIENT_NUMBER);
		DataConverter identifierFormatter = new ObjectFormatter("{identifier}");
		DataDefinition identifierDef = new ConvertedPatientDataDefinition("identifier", new PatientIdentifierDataDefinition(
		        upn.getName(), upn), new DQAIdentifierCompletenessDataConverter());
		
		DataConverter nameFormatter = new ObjectFormatter("{familyName}, {givenName}");
		DataDefinition nameDef = new ConvertedPersonDataDefinition("name", new PreferredNameDataDefinition(), nameFormatter);
		dsd.addColumn("id", new PersonIdDataDefinition(), "");
		dsd.addColumn("Name", nameDef, "");
		dsd.addColumn("CCC No", identifierDef, "");
		dsd.addColumn("CCC No Format", identifierDef, "");
		dsd.addColumn("Sex", new GenderDataDefinition(), "", new DQADefaultYesDataConverter());
		dsd.addColumn("Date of Birth", new BirthdateDataDefinition(), "", new DQADefaultYesDataConverter());
		dsd.addSortCriteria("Category", SortCriteria.SortDirection.ASC);
		
		DQAWeightDataDefinition weightDataDefinition = new DQAWeightDataDefinition();
		weightDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		weightDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("Weight", weightDataDefinition, indParams, new DQADefaultDataCompletenessDataConverter());
		
		DQAHeightDataDefinition heightDataDefinition = new DQAHeightDataDefinition();
		heightDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		heightDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("Height", heightDataDefinition, indParams, new DQADefaultDataCompletenessDataConverter());
		
		DQAARTInitiationDateDataDefinition artInitiationDateDataDefinition = new DQAARTInitiationDateDataDefinition();
		artInitiationDateDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		artInitiationDateDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("ART Initiation date", artInitiationDateDataDefinition, indParams,
		    new DQADefaultDataCompletenessDataConverter());
		
		DQACurrentRegimenDataDefinition currentRegimenDataDefinition = new DQACurrentRegimenDataDefinition();
		currentRegimenDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		currentRegimenDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("Current ART Regimen", currentRegimenDataDefinition, indParams,
		    new DQADefaultDataCompletenessDataConverter());
		
		LastAppointmentPeriodDataDefinition lastAppointmentPeriodDataDefinition = new LastAppointmentPeriodDataDefinition();
		lastAppointmentPeriodDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		lastAppointmentPeriodDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("Drug dosage given (Duration)", lastAppointmentPeriodDataDefinition, indParams,
		    new DQADefaultDataCompletenessDataConverter());
		
		DQATBScreeningLastVisitDataDefinition tbScreeningDataDefinition = new DQATBScreeningLastVisitDataDefinition();
		tbScreeningDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		tbScreeningDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("TB Screening", tbScreeningDataDefinition, indParams, null);
		
		EverOnIPTDataDefinition everOnIPTDataDefinition = new EverOnIPTDataDefinition();
		everOnIPTDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		everOnIPTDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("TPT/IPT initiated", everOnIPTDataDefinition, indParams, new DQADefaultDataCompletenessDataConverter());
		
		LastNutritionAssessmentDataDefinition lastNutritionAssessmentDataDefinition = new LastNutritionAssessmentDataDefinition();
		lastNutritionAssessmentDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		lastNutritionAssessmentDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("Nutrition Assessment done", lastNutritionAssessmentDataDefinition, indParams,
		    new DQADefaultDataCompletenessDataConverter());
		
		LastDSDModelDataDefinition lastDSDModelDataDefinition = new LastDSDModelDataDefinition();
		lastDSDModelDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		lastDSDModelDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("DSD model", lastDSDModelDataDefinition, indParams, new DQADefaultDataCompletenessDataConverter());
		
		DQALastVLDateDataDefinition lastVLDateDataDefinition = new DQALastVLDateDataDefinition();
		lastVLDateDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		lastVLDateDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("Latest VL result documented", lastVLDateDataDefinition, indParams,
		    new DQADefaultDataCompletenessDataConverter());
		
		DQALastVisitDataDefinition lastVisitDateDataDefinition = new DQALastVisitDataDefinition();
		lastVisitDateDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		lastVisitDateDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("Date of last appointment", lastVisitDateDataDefinition, indParams, new DQADefaultYesDataConverter());
		
		DQACohortCategoryDataDefinition cohortCategoryDataDefinition = new DQACohortCategoryDataDefinition();
		cohortCategoryDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cohortCategoryDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		dsd.addColumn("Category", cohortCategoryDataDefinition, indParams, null);
		
		DQANupiDataDefinition nupiDataDefinition = new DQANupiDataDefinition();
		nupiDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		nupiDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		dsd.addColumn("NUPI", nupiDataDefinition, indParams, new DQADefaultDataCompletenessDataConverter());
		dsd.addColumn("Date confirmed positive", new CalculationDataDefinition("Date confirmed positive",
		        new DateConfirmedHivPositiveCalculation()), "", new DQADefaultDataCompletenessDataConverter());
		
		DQABaselineCD4DataDefinition baselineCD4DataDefinition = new DQABaselineCD4DataDefinition();
		baselineCD4DataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		baselineCD4DataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		dsd.addColumn("Baseline CD4", baselineCD4DataDefinition, indParams, new DQADefaultDataCompletenessDataConverter());
		
		return dsd;
	}
	
}
