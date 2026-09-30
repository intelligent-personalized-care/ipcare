package pt.ipc.storage.repositories.jdbi

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import pt.ipc.domain.Role
import pt.ipc.domain.User
import pt.ipc.storage.repositories.UsersRepository
import java.util.*

class JdbiUsersRepository(
    private val handle: Handle
) : UsersRepository {

    override fun getUserBySession(sessionID: String): UUID? =
        handle.createQuery("select user_id from dbo.session s where s.session = :sessionID")
            .bind("sessionID", sessionID)
            .mapTo<UUID>()
            .singleOrNull()

    override fun updateSession(userID: UUID, sessionID: String) {
        handle.createUpdate(
            "insert into dbo.session (user_id, session) values (:userID,:sessionID)" +
                "on conflict(user_id) do update set session = :sessionID "
        )
            .bind("userID", userID)
            .bind("sessionID", sessionID)
            .execute()
    }

    override fun getUserByIDAndSession(id: UUID, sessionID: String): User? =
        handle.createQuery(
            "select u.id,u.name,u.email,u.password_hash from dbo.\"user\" u " +
                "inner join dbo.session s on s.user_id = u.id " +
                "where s.user_id = :id and s.session = :sessionID"
        )
            .bind("id", id)
            .bind("sessionID", sessionID)
            .mapTo<User>()
            .singleOrNull()

    override fun getUsersIDs(): List<UUID> =
        handle.createQuery("select id from dbo.\"user\"")
            .mapTo<UUID>()
            .toList()

    override fun login(email: String, passwordHash: String): UUID? =
        handle.createQuery("select id from dbo.\"user\" where email = :email and password_hash = :passwordHash")
            .bind("email", email)
            .bind("passwordHash", passwordHash)
            .mapTo<UUID>()
            .singleOrNull()

    override fun getUserByID(userID: UUID): User? =
        handle.createQuery("select id, name, email, password_hash from dbo.\"user\" where id = :userID")
            .bind("userID", userID)
            .mapTo<User>()
            .singleOrNull()

    override fun getRoleByID(userID: UUID): Role =
        handle.createQuery("select 'PATIENT' from dbo.patient where id = :userID")
            .bind("userID", userID)
            .mapTo<Role>()
            .singleOrNull()
            ?: handle.createQuery("select 'PHYSIOTHERAPIST' from dbo.physiotherapist where id = :userID")
                .bind("userID", userID)
                .mapTo<Role>()
                .singleOrNull() ?: Role.ADMIN
}
