package pt.ipc.http.utils

object Uris {

    const val USERS_SUBSCRIBE = "/users/subscribe"
    const val USERS_UNSUBSCRIBE = "/users/unsubscribe"

    const val USERS_LOGIN = "/users/login"
    const val USER_PHOTO = "/users/{userID}/photo"
    const val REFRESH_TOKEN = "/users/refresh"

    const val PATIENT_REGISTER = "/users/patients"
    const val PATIENT_PROFILE = "/users/patients/{patientID}/profile"
    const val PATIENT_PHOTO = "/users/patients/{patientID}/profile/photo"
    const val PATIENT_PHYSIOTHERAPIST = "/users/patients/{patientID}/physiotherapist"

    const val PHYSIOTHERAPIST_BY_ID = "/users/physiotherapists/{physiotherapistID}"
    const val PHYSIOTHERAPIST_REQUESTS = "/users/physiotherapists/{physiotherapistID}/requests"
    const val PHYSIOTHERAPIST_DECIDE_REQUEST = "/users/physiotherapists/{physiotherapistID}/requests/{requestID}"
    const val PHYSIOTHERAPIST_RATE = "/users/physiotherapists/{physiotherapistID}/rate"
    const val PHYSIOTHERAPIST_CREDENTIAL = "/users/physiotherapists/{physiotherapistID}/credential"
    const val PHYSIOTHERAPISTS = "/users/physiotherapists"
    const val PHYSIOTHERAPIST_PHOTO = "/users/physiotherapists/{physiotherapistID}/profile/photo"
    const val PATIENTS_OF_PHYSIOTHERAPIST = "/users/physiotherapists/{physiotherapistID}/patients"
    const val PATIENT_OF_PHYSIOTHERAPIST = "/users/physiotherapists/{physiotherapistID}/patients/{patientID}"
    const val PHYSIOTHERAPIST_PATIENT_EXERCISES = "/users/physiotherapists/{physiotherapistID}/patients/exercises"
    const val PHYSIOTHERAPIST_PROFILE = "/users/physiotherapists/{physiotherapistID}/profile"

    const val EXERCISES = "/exercises"
    const val EXERCISES_INFO = "/exercises/{exerciseID}"
    const val EXERCISES_INFO_VIDEO = "/exercises/{exerciseID}/video"

    const val PHYSIOTHERAPIST_PATIENT_PLANS = "/users/physiotherapists/{physiotherapistID}/patients/{patientID}/plans"
    const val PLAN_CURRENT = "/users/patients/{patientID}/plans"
    const val EXERCISES_OF_PATIENT = "/users/patients/{patientID}/exercises"
    const val PLANS_OF_PHYSIOTHERAPIST = "/users/physiotherapists/{physiotherapistID}/plans"
    const val PHYSIOTHERAPIST_PLAN_BY_ID = "/users/physiotherapists/{physiotherapistID}/plans/{planID}"

    const val VIDEO_OF_EXERCISE = "/users/patients/{patientID}/plans/{planID}/daily_lists/{dailyListID}/exercises/{exerciseID}"
    const val EXERCISE_FEEDBACK = "/users/patients/{patientID}/plans/{planID}/daily_lists/{dailyListID}/exercises/{exerciseID}/feedback"

    const val ADMIN_CREATION = "/admin"
    const val UNVERIFIED_PHYSIOTHERAPISTS = "/admin/unverified_physiotherapists"
    const val UNVERIFIED_PHYSIOTHERAPIST = "/admin/unverified_physiotherapists/{physiotherapistID}"
    const val ADD_VIDEO_PREVIEW = "/admin/video_preview"
}
