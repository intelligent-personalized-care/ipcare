package pt.ipc_app.domain.user

/**
 * Represents the patient.
 *
 * @property [name] the patient's name
 */
class Patient(
    name: String,
    email: String,
    password: String,
    val birthDate: String,
    val weight: Int,
    val height: Int,
    val physicalCondition: String
): User(name, email, password) {

    companion object {
        const val WEIGHT_LENGTH_MAX = 3
        const val HEIGHT_LENGTH_MAX = 3
        val PHYSICAL_CONDITION_LENGTH_RANGE = 3..255

        /**
         * Returns an [Patient] instance with the received values or null, if those
         * values are invalid.
         */
        fun patientOrNull(
            name: String,
            email: String,
            password: String,
            birthDate: String,
            weight: Int,
            height: Int,
            physicalCondition: String
        ): Patient? =
            if (validatePatient(name, email, password, birthDate, weight, height, physicalCondition))
                Patient(name, email, password, birthDate, weight, height, physicalCondition)
            else null

        /**
         * Checks whether the received values are acceptable as [Patient]
         * instance fields.
         */
        private fun validatePatient(
            name: String,
            email: String,
            password: String,
            birthDate: String,
            weight: Int,
            height: Int,
            physicalCondition: String
        ) =
            validateUsername(name) && validateEmail(email) && validatePassword(password)
    }
}
