package edu.rit.swen755.auth.clinic;

/**
 * A patient of the clinic.
 *
 * @param id   the patient id used in URLs such as {@code /api/patients/1/record}
 * @param name the patient's name
 */
public record Patient(long id, String name) {
}
