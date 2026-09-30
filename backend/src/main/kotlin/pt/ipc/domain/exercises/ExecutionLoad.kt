package pt.ipc.domain.exercises

import pt.ipc.domain.exceptions.BadRequest

class InvalidExecutionLoad : BadRequest("For an exercise with load, provide a positive finite loadValue in kg; otherwise omit loadValue and loadUnit")

fun validateExecutionLoad(withLoad: Boolean?, loadValue: Float?, loadUnit: String?) {
    if (withLoad == true) {
        if (loadValue == null || !loadValue.isFinite() || loadValue <= 0f || loadUnit != "kg") throw InvalidExecutionLoad()
    } else if (loadValue != null || loadUnit != null) throw InvalidExecutionLoad()
}
