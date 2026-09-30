package pt.ipc.storage.transaction

import org.springframework.stereotype.Component
import pt.ipc.storage.cloudStorageUtils.CloudStorageUtils
import pt.ipc.storage.repositories.AdminRepository
import pt.ipc.storage.repositories.PatientsRepository
import pt.ipc.storage.repositories.ExerciseRepository
import pt.ipc.storage.repositories.PhysiotherapistRepository
import pt.ipc.storage.repositories.PlansRepository
import pt.ipc.storage.repositories.UsersRepository

@Component
interface Transaction {

    val patientsRepository: PatientsRepository

    val physiotherapistRepository: PhysiotherapistRepository

    val plansRepository: PlansRepository

    val exerciseRepository: ExerciseRepository

    val cloudStorage: CloudStorageUtils

    val adminRepository: AdminRepository

    val usersRepository: UsersRepository
}
