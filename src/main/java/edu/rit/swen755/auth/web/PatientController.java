package edu.rit.swen755.auth.web;

import edu.rit.swen755.auth.clinic.Appointment;
import edu.rit.swen755.auth.clinic.ClinicRepository;
import edu.rit.swen755.auth.clinic.MedicalRecord;
import edu.rit.swen755.auth.clinic.Patient;
import edu.rit.swen755.auth.clinic.Prescription;
import edu.rit.swen755.auth.clinic.Vitals;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The patient services.
 *
 * <p>Who may call each URL is decided by the URL rules (see
 * {@link edu.rit.swen755.auth.access.AccessRules}). The two read services a patient may call also
 * carry {@code @PreAuthorize("@ownRecord.allows(authentication, #id)")}: method security runs that
 * check before the body, so a patient is refused (403) another patient's data before the
 * repository is consulted. These annotations are part of the frozen contract; do not remove or
 * change them when filling the bodies.
 */
@RestController
public class PatientController {

    private final ClinicRepository clinic;

    /**
     * @param clinic the clinic data
     */
    public PatientController(ClinicRepository clinic) {
        this.clinic = clinic;
    }

    /**
     * {@code GET /api/patients} (slice 3: Chase): every patient, from
     * {@link ClinicRepository#patients()}.
     *
     * @return the patients
     */
    @GetMapping("/api/patients")
    public List<Patient> patients() {
        return clinic.patients();
    }

    /**
     * {@code GET /api/patients/{id}/record} (slice 3: Chase): the record from
     * {@link ClinicRepository#record(long)}; an unknown id gives 404.
     *
     * @param id the patient id
     * @return the record
     */
    @GetMapping("/api/patients/{id}/record")
    @PreAuthorize("@ownRecord.allows(authentication, #id)")
    public MedicalRecord record(@PathVariable long id) {
        return clinic.record(id);
    }

    /**
     * {@code GET /api/patients/{id}/appointments} (slice 3: Chase): the appointments from
     * {@link ClinicRepository#appointments(long)}; an unknown id gives 404.
     *
     * @param id the patient id
     * @return the appointments
     */
    @GetMapping("/api/patients/{id}/appointments")
    @PreAuthorize("@ownRecord.allows(authentication, #id)")
    public List<Appointment> appointments(@PathVariable long id) {
        return clinic.appointments(id);
    }

    /**
     * {@code POST /api/patients/{id}/vitals} (slice 3: Chase): stores the vitals through
     * {@link ClinicRepository#addVitals(long, Vitals)} and answers 201 with the stored item; an
     * unknown id gives 404.
     *
     * @param id     the patient id
     * @param vitals the JSON body
     * @return the stored vitals
     */
    @PostMapping("/api/patients/{id}/vitals")
    @ResponseStatus(HttpStatus.CREATED)
    public Vitals addVitals(@PathVariable long id, @RequestBody Vitals vitals) {
        return clinic.addVitals(id, vitals);
    }

    /**
     * {@code POST /api/patients/{id}/prescriptions} (slice 3: Chase): stores the prescription
     * through {@link ClinicRepository#addPrescription(long, Prescription)} and answers 201 with the
     * stored item; an unknown id gives 404.
     *
     * @param id           the patient id
     * @param prescription the JSON body
     * @return the stored prescription
     */
    @PostMapping("/api/patients/{id}/prescriptions")
    @ResponseStatus(HttpStatus.CREATED)
    public Prescription addPrescription(@PathVariable long id, @RequestBody Prescription prescription) {
        return clinic.addPrescription(id, prescription);
    }
}
