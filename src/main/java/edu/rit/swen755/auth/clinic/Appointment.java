package edu.rit.swen755.auth.clinic;

/**
 * A booked appointment.
 *
 * @param patientId the patient the appointment is for
 * @param when      the date and time, as text (for example {@code 2026-10-22T09:30})
 * @param with      who the appointment is with
 */
public record Appointment(long patientId, String when, String with) {
}
