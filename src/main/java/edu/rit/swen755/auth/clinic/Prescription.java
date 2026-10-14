package edu.rit.swen755.auth.clinic;

/**
 * A prescription written by a doctor.
 *
 * @param drug the drug name
 * @param dose the dose, as text (for example {@code 500mg})
 */
public record Prescription(String drug, String dose) {
}
