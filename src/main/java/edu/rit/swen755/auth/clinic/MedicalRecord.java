package edu.rit.swen755.auth.clinic;

import java.util.List;

/**
 * A patient's medical record: the protected data of the own-record rule.
 *
 * @param patientId the patient the record belongs to
 * @param notes     the clinical notes, oldest first
 */
public record MedicalRecord(long patientId, List<String> notes) {
}
