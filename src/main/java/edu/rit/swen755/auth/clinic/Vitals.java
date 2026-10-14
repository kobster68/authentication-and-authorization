package edu.rit.swen755.auth.clinic;

/**
 * A set of vital signs recorded by staff.
 *
 * @param systolic  systolic blood pressure (mmHg)
 * @param diastolic diastolic blood pressure (mmHg)
 * @param pulse     pulse (beats per minute)
 */
public record Vitals(int systolic, int diastolic, int pulse) {
}
