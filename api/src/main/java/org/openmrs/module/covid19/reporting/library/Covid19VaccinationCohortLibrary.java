/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.covid19.reporting.library;

import org.openmrs.module.kenyacore.report.ReportUtils;
import org.openmrs.module.kenyaemr.reporting.library.ETLReports.RevisedDatim.DatimCohortLibrary;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CompositionCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Library of cohort definitions for Covid-19 vaccinations
 */
@Component
public class Covid19VaccinationCohortLibrary {
	
	public CohortDefinition currentInCare() {
		SqlCohortDefinition cd = new SqlCohortDefinition();
		String sqlQuery = "select e.patient_id\n" + "from (select fup.visit_date,\n" + "             fup.patient_id,\n"
		        + "             min(e.visit_date)                                               as enroll_date,\n"
		        + "             max(fup.visit_date)                                             as latest_vis_date,\n"
		        + "             mid(max(concat(fup.visit_date, fup.next_appointment_date)), 11) as latest_tca,\n"
		        + "             max(d.visit_date)                                               as date_discontinued,\n"
		        + "             d.patient_id                                                    as disc_patient\n"
		        + "      from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "             join kenyaemr_etl.etl_patient_demographics p on p.patient_id = fup.patient_id\n"
		        + "             join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id = e.patient_id\n"
		        + "             left outer JOIN (select patient_id, visit_date\n"
		        + "                              from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                              where date(visit_date) <= date(:endDate)\n"
		        + "                                and program_name = 'HIV'\n"
		        + "                              group by patient_id) d on d.patient_id = fup.patient_id\n"
		        + "      where fup.visit_date <= date(:endDate)\n" + "      group by patient_id\n"
		        + "      having ((date(latest_tca) > date(:endDate) and\n"
		        + "               (date(latest_tca) > date(date_discontinued) or disc_patient is null) and\n"
		        + "               (date(latest_vis_date) > date(date_discontinued) or disc_patient is null)) or\n"
		        + "              (((date(latest_tca) between date(:startDate) and date(:endDate)) and\n"
		        + "                (date(latest_vis_date) >= date(latest_tca)) or date(latest_tca) > curdate())) and\n"
		        + "              (date(latest_tca) > date(date_discontinued) or disc_patient is null))) e;";
		cd.setName("currentInCare");
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setQuery(sqlQuery);
		cd.setDescription("currentInCare");
		
		return cd;
	}
	
	public CohortDefinition fullyVaccinated() {
		SqlCohortDefinition cd = new SqlCohortDefinition();
		String sqlQuery = "select patient_id from kenyaemr_etl.etl_covid19_assessment\n"
		        + "group by patient_id\n"
		        + "having mid(max(concat(visit_date,final_vaccination_status)),11) = 5585 and mid(max(concat(visit_date,ever_vaccinated)),11) is not null\n"
		        + "and mid(max(concat(visit_date,date(second_dose_date))),11) between date(:startDate) and :endDate \n"
		        + "or (mid(max(concat(visit_date,first_vaccine_type)),11)= 166355 and mid(max(concat(visit_date,date(first_dose_date))),11) between date(:startDate) and :endDate);\n";
		cd.setName("fullyVaccinated");
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setQuery(sqlQuery);
		cd.setDescription("fullyVaccinated");
		
		return cd;
	}
	
	public CohortDefinition partiallyVaccinated() {
		SqlCohortDefinition cd = new SqlCohortDefinition();
		String sqlQuery = "select patient_id from kenyaemr_etl.etl_covid19_assessment\n"
		        + "group by patient_id\n"
		        + "having mid(max(concat(visit_date,final_vaccination_status)),11) = 166192 and mid(max(concat(visit_date,ever_vaccinated)),11) is not null\n"
		        + "and mid(max(concat(visit_date,date(first_dose_date))),11) between date(:startDate) and :endDate and mid(max(concat(visit_date,first_vaccine_type)),11) <> 166355;\n";
		cd.setName("partiallyVaccinated;");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("partiallyVaccinated");
		
		return cd;
	}
	
	public CohortDefinition notVaccinatedCovid19Sql() {
		SqlCohortDefinition cd = new SqlCohortDefinition();
		String sqlQuery = "select a.patient_id from kenyaemr_etl.etl_covid19_assessment a group by a.patient_id having\n"
		        + "            mid(max(concat(a.visit_date,a.ever_vaccinated)),11) is null  or mid(max(concat(a.visit_date,a.ever_vaccinated)),11)=1066;";
		cd.setName("notVaccinated;");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("notVaccinated");
		
		return cd;
	}
	
	public CohortDefinition assessedForCovid19Sql() {
		SqlCohortDefinition cd = new SqlCohortDefinition();
		String sqlQuery = "select a.patient_id from kenyaemr_etl.etl_covid19_assessment a";
		cd.setName("unknownCovid19VaccinationStatus;");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("unknownCovid19VaccinationStatus");
		
		return cd;
	}
	
	public CohortDefinition everInfected() {
		SqlCohortDefinition cd = new SqlCohortDefinition();
		String sqlQuery = "select patient_id from kenyaemr_etl.etl_covid19_assessment where ever_tested_covid_19_positive = 703 and ever_vaccinated is not null\n"
		        + "group by patient_id;";
		cd.setName("everInfected;");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("everInfected");
		
		return cd;
	}
	
	public CohortDefinition everHospitalised() {
		SqlCohortDefinition cd = new SqlCohortDefinition();
		String sqlQuery = "select patient_id from kenyaemr_etl.etl_covid19_assessment where hospital_admission = 1065 and ever_vaccinated is not null;\n";
		cd.setName("everHospitalised");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("everHospitalised");
		
		return cd;
	}
	
	public CohortDefinition diedDueToCovid() {
		SqlCohortDefinition cd = new SqlCohortDefinition();
		String sqlQuery = "select patient_id from kenyaemr_etl.etl_patient_program_discontinuation where discontinuation_reason =160034 and specific_death_cause=165609\n"
		        + "and coalesce(date(date_died),coalesce(date(effective_discontinuation_date),date(visit_date))) between date(:startDate) and date(:endDate);";
		cd.setName("diedDueToCovid");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("diedDueToCovid");
		
		return cd;
	}
	
	public CohortDefinition aged18AndAbove() {
		SqlCohortDefinition cd = new SqlCohortDefinition();
		String sqlQuery = "select patient_id from kenyaemr_etl.etl_patient_demographics where timestampdiff(YEAR ,dob,date(:endDate))>= 18;\n";
		cd.setName("aged18andAbove");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("aged18andAbove");
		
		return cd;
	}
	
	public CohortDefinition firstDoseVerifiedSQl() {
		SqlCohortDefinition cd = new SqlCohortDefinition();
		String sqlQuery = " select patient_id from kenyaemr_etl.etl_covid19_assessment where first_vaccination_verified = 164134 and ever_vaccinated is not null and\n"
		        + "        first_dose_date between date(:startDate) and :endDate;";
		cd.setName("firstDose");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("firstDose");
		
		return cd;
	}
	
	public CohortDefinition secondDoseVerifiedSQL() {
		SqlCohortDefinition cd = new SqlCohortDefinition();
		String sqlQuery = "select patient_id from kenyaemr_etl.etl_covid19_assessment where second_vaccination_verified = 164134 and ever_vaccinated is not null and\n"
		        + "        second_dose_date between date(:startDate) and :endDate;";
		cd.setName("secondDose");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("secondDose");
		
		return cd;
	}
	
	public CohortDefinition boosterDoseVerifiedSQL() {
		SqlCohortDefinition cd = new SqlCohortDefinition();
		String sqlQuery = " select patient_id from kenyaemr_etl.etl_covid19_assessment where booster_dose_verified = 164134 and ever_vaccinated is not null and\n"
		        + "        date_taken_booster_vaccine between date(:startDate) and :endDate;";
		cd.setName("boosterDose");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("boosterDose");
		
		return cd;
	}
	
	/**
	 * Patients OnArt and partially vaccinated
	 * 
	 * @return the cohort definition
	 */
	public CohortDefinition cicPartiallyVaccinated() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("cic", ReportUtils.map(currentInCare(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("partiallyVaccinated",
		    ReportUtils.map(partiallyVaccinated(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("cic AND partiallyVaccinated");
		return cd;
	}
	
	/**
	 * Patients On Art and not vaccinated
	 * 
	 * @return the cohort definition
	 */
	public CohortDefinition cicNotVaccinatedCovid19() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("cic", ReportUtils.map(currentInCare(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("notVaccinatedCovid19Sql",
		    ReportUtils.map(notVaccinatedCovid19Sql(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("cic AND notVaccinatedCovid19Sql");
		return cd;
	}
	
	/**
	 * Patients On Art and with unknown Covid-19 vaccination status
	 * 
	 * @return the cohort definition
	 */
	public CohortDefinition cicUnknownVaccinationStatus() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("cic", ReportUtils.map(currentInCare(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("assessedforCovid19",
		    ReportUtils.map(assessedForCovid19Sql(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("aged18andAbove", ReportUtils.map(aged18AndAbove(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("cic AND aged18andAbove AND NOT assessedforCovid19");
		return cd;
	}
	
	/**
	 * Patients cic and fully vaccinated
	 * 
	 * @return the cohort definition
	 */
	public CohortDefinition cicFullyVaccinated() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("cic", ReportUtils.map(currentInCare(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("fullyVaccinated", ReportUtils.map(fullyVaccinated(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("cic AND fullyVaccinated");
		return cd;
	}
	
	/**
	 * Patients cic and ever infected
	 * 
	 * @return the cohort definition
	 */
	public CohortDefinition cicEverInfected() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("cic", ReportUtils.map(currentInCare(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("everInfected", ReportUtils.map(everInfected(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("aged18andAbove", ReportUtils.map(aged18AndAbove(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("cic AND aged18andAbove AND everInfected");
		return cd;
	}
	
	/**
	 * Patients cic and ever admitted to hospital due to covid
	 * 
	 * @return the cohort definition
	 */
	public CohortDefinition cicEverHospitalised() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("cic", ReportUtils.map(currentInCare(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("everHospitalised", ReportUtils.map(everHospitalised(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("aged18andAbove", ReportUtils.map(aged18AndAbove(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("cic AND aged18andAbove AND everHospitalised");
		return cd;
	}
	
	/**
	 * Patients cic and 18 years and above
	 * 
	 * @return the cohort definition
	 */
	public CohortDefinition cicAged18AndAbove() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("cic", ReportUtils.map(currentInCare(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("aged18andAbove", ReportUtils.map(aged18AndAbove(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("cic AND aged18andAbove");
		return cd;
	}
	
	/**
	 * Patients with first dose verified
	 * 
	 * @return the cohort definition
	 */
	public CohortDefinition cicFirstDoseVerified() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("cic", ReportUtils.map(currentInCare(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("firstDoseVerified",
		    ReportUtils.map(firstDoseVerifiedSQl(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("cic AND firstDoseVerified");
		return cd;
	}
	
	/**
	 * Patients with second dose verified
	 * 
	 * @return the cohort definition
	 */
	public CohortDefinition cicSecondDoseVerified() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("cic", ReportUtils.map(currentInCare(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("secondDoseVerified",
		    ReportUtils.map(secondDoseVerifiedSQL(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("cic AND secondDoseVerified");
		return cd;
	}
	
	/**
	 * Patients with booster dose verified
	 * 
	 * @return the cohort definition
	 */
	public CohortDefinition cicBoosterDoseVerified() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("cic", ReportUtils.map(currentInCare(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("boosterDoseVerified",
		    ReportUtils.map(boosterDoseVerifiedSQL(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("cic AND boosterDoseVerified");
		return cd;
	}
}
