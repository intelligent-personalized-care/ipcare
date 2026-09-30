package pt.ipc_app.domain.user

/**
 * Represents the physiotherapist.
 *
 * @property [name] the physiotherapist's name
 */
class Physiotherapist(
    name: String,
    email: String,
    password: String
): User(name, email, password) {

    companion object {

        /**
         * Returns an [Physiotherapist] instance with the received values or null, if those
         * values are invalid.
         */
        fun physiotherapistOrNull(
            name: String,
            email: String,
            password: String,
        ): Physiotherapist? =
            if (validatePhysiotherapist(name, email, password))
                Physiotherapist(name, email, password)
            else null

        /**
         * Checks whether the received values are acceptable as [Physiotherapist]
         * instance fields.
         */
        fun validatePhysiotherapist(
            name: String,
            email: String,
            password: String
        ) =
            validateUsername(name) && validateEmail(email) && validatePassword(password)
    }
}
