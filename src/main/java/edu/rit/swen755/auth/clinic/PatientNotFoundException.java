package edu.rit.swen755.auth.clinic;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a request names a patient id that does not exist; the web layer answers 404.
 *
 * <p>Only staff can reach this: a patient asking for another id is refused with 403 by the
 * own-record check before the repository is consulted, so a 404 never leaks to a patient whether
 * an id exists.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class PatientNotFoundException extends RuntimeException {

    /**
     * @param patientId the id that was not found
     */
    public PatientNotFoundException(long patientId) {
        super("no patient with id " + patientId);
    }
}
