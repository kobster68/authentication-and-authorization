package edu.rit.swen755.auth.clinic;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

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

    private final Map<Long, PatientData> patients = Map.of(
        1L, new PatientData(new Patient(1, "Alice Nguyen"),
            new MedicalRecord(1, List.of("Seasonal allergies; symptoms controlled.")),
            new Appointment(1, "2026-10-22T09:30", "Dr. Chen")),
        2L, new PatientData(new Patient(2, "Bob Okafor"),
            new MedicalRecord(2, List.of("Follow-up for elevated blood pressure.")),
            new Appointment(2, "2026-10-23T11:00", "Nurse Patel")),
        3L, new PatientData(new Patient(3, "Carol Diaz"),
            new MedicalRecord(3, List.of("Routine annual check-up.")),
            new Appointment(3, "2026-10-24T14:15", "Dr. Chen")));

    /**
     * Every patient, in id order.
     *
     * @return the three seeded patients
     */
    public List<Patient> patients() {
        return patients.values().stream()
            .map(data -> data.patient)
            .sorted((left, right) -> Long.compare(left.id(), right.id()))
            .toList();
    }

    /**
     * A patient's medical record.
     *
     * @param id the patient id
     * @return the record, whose {@code patientId} is {@code id}
     * @throws PatientNotFoundException if the id is unknown
     */
    public MedicalRecord record(long id) {
        return patient(id).record;
    }

    /**
     * A patient's appointments.
     *
     * @param id the patient id
     * @return the appointments (at least one for each seeded patient)
     * @throws PatientNotFoundException if the id is unknown
     */
    public List<Appointment> appointments(long id) {
        return List.of(patient(id).appointment);
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
        PatientData data = patient(id);
        data.vitals.add(vitals);
        return vitals;
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
        PatientData data = patient(id);
        data.prescriptions.add(prescription);
        return prescription;
    }

    private PatientData patient(long id) {
        PatientData data = patients.get(id);
        if (data == null) {
            throw new PatientNotFoundException(id);
        }
        return data;
    }

    private static final class PatientData {
        private final Patient patient;
        private final MedicalRecord record;
        private final Appointment appointment;
        private final List<Vitals> vitals = new CopyOnWriteArrayList<>();
        private final List<Prescription> prescriptions = new CopyOnWriteArrayList<>();

        private PatientData(Patient patient, MedicalRecord record, Appointment appointment) {
            this.patient = patient;
            this.record = record;
            this.appointment = appointment;
        }
    }
}
