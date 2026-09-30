package pt.ipc_app.ui.components

import pt.ipc_app.service.utils.*

data class UserFacingError(val title: String, val message: String)

fun ResponseError.userFacing(): UserFacingError = when {
    this is NoInternetConnection -> UserFacingError("Sem ligação", "Verifica a ligação à Internet e tenta novamente.")
    this is ProblemJson && unauthenticatedResponse() -> UserFacingError("Sessão terminada", "Inicia sessão novamente para continuar.")
    this is ProblemJson && status == 401 -> UserFacingError("Não foi possível entrar", "Verifica o email e a palavra-passe e tenta novamente.")
    title == Errors.emailAlreadyExists -> UserFacingError("Email já registado", "Utiliza outro email ou inicia sessão na tua conta.")
    this is ProblemJson && status == 403 -> UserFacingError("Acesso indisponível", "A tua conta não tem acesso a esta ação.")
    this is ProblemJson && status == 404 -> UserFacingError("Informação indisponível", "Este registo já não está disponível. Volta atrás e atualiza a lista.")
    this is ProblemJson && status == 409 -> UserFacingError("Não foi possível concluir", "Os dados foram alterados ou já existem. Atualiza a informação e tenta novamente.")
    this is ProblemJson && status in listOf(400, 422) -> UserFacingError("Revê os dados", "Verifica os campos preenchidos e tenta novamente.")
    this is ProblemJson && status >= 500 -> UserFacingError("Serviço indisponível", "Não conseguimos contactar o serviço neste momento. Tenta novamente mais tarde.")
    else -> UserFacingError("Não foi possível concluir", "Tenta novamente. Se o problema continuar, volta a abrir este ecrã.")
}
