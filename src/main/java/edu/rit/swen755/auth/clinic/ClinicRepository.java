package edu.rit.swen755.auth.clinic;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * The clinic's data, held in memory.
 *
 * <p>Required seed (slice 3: Chase): patients 1 Alice Nguyen, 2 Bob Okafor and 3 Carol Diaz (who
 * has no login), each with one record note and one appointment. Every method that takes a patient
 * id throws {@link PatientNotFoundException} when the id is unknown. The repository must be safe
 * for concurrent requests.
 */
@Repository
public class ClinicRepository {

    /**
     * Every patient, in id order.
     *
     * @return the three seeded patients
     */
    public List<Patient> patients() {
        throw new UnsupportedOperationException("TODO(slice 3: Chase)");
    }

    /**
     * A patient's medical record.
     *
     * @param id the patient id
     * @return the record, whose {@code patientId} is {@code id}
     * @throws PatientNotFoundException if the id is unknown
     */
    public MedicalRecord record(long id) {
        throw new UnsupportedOperationException("TODO(slice 3: Chase)");
    }

    /**
     * A patient's appointments.
     *
     * @param id the patient id
     * @return the appointments (at least one for each seeded patient)
     * @throws PatientNotFoundException if the id is unknown
     */
    public List<Appointment> appointments(long id) {
        throw new UnsupportedOperationException("TODO(slice 3: Chase)");
    }

    /**
     * Records a new set of vitals for a patient.
     *
     * @param id     the patient id
     * @param vitals the vitals to append
     * @return the stored vitals (equal to the argument)
     * @throws PatientNotFoundException if the id is unknown
     */
    public Vitals addVitals(long id, Vitals vitals) {
        throw new UnsupportedOperationException("TODO(slice 3: Chase)");
    }

    /**
     * Records a new prescription for a patient.
     *
     * @param id           the patient id
     * @param prescription the prescription to append
     * @return the stored prescription (equal to the argument)
     * @throws PatientNotFoundException if the id is unknown
     */
    public Prescription addPrescription(long id, Prescription prescription) {
        throw new UnsupportedOperationException("TODO(slice 3: Chase)");
    }
}
