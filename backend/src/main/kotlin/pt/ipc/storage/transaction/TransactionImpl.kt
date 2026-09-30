package pt.ipc.storage.transaction

import org.jdbi.v3.core.Handle
import pt.ipc.storage.cloudStorageUtils.CloudStorageUtils
import pt.ipc.storage.cloudStorageUtils.CloudStorageUtilsImpl
import pt.ipc.storage.repositories.AdminRepository
import pt.ipc.storage.repositories.PatientsRepository
import pt.ipc.storage.repositories.ExerciseRepository
import pt.ipc.storage.repositories.PhysiotherapistRepository
import pt.ipc.storage.repositories.PlansRepository
import pt.ipc.storage.repositories.UsersRepository
import pt.ipc.storage.repositories.jdbi.JdbiAdminRepository
import pt.ipc.storage.repositories.jdbi.JdbiPatientsRepository
import pt.ipc.storage.repositories.jdbi.JdbiExercisesRepository
import pt.ipc.storage.repositories.jdbi.JdbiPhysiotherapistsRepository
import pt.ipc.storage.repositories.jdbi.JdbiPlansRepository
import pt.ipc.storage.repositories.jdbi.JdbiUsersRepository

class TransactionImpl(
    private val handle: Handle
) : Transaction {

    override val patientsRepository: PatientsRepository by lazy { JdbiPatientsRepository(handle) }

    override val physiotherapistRepository: PhysiotherapistRepository by lazy { JdbiPhysiotherapistsRepository(handle) }

    override val plansRepository: PlansRepository by lazy { JdbiPlansRepository(handle) }

    override val exerciseRepository: ExerciseRepository by lazy { JdbiExercisesRepository(handle) }

    override val cloudStorage: CloudStorageUtils by lazy { CloudStorageUtilsImpl() }

    override val adminRepository: AdminRepository by lazy { JdbiAdminRepository(handle) }

    override val usersRepository: UsersRepository by lazy { JdbiUsersRepository(handle) }
}
