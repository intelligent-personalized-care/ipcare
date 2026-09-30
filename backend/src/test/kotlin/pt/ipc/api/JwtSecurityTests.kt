package pt.ipc.api

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import pt.ipc.domain.Role
import pt.ipc.domain.exceptions.Unauthenticated
import pt.ipc.domain.jwt.JwtConfiguration
import pt.ipc.domain.jwt.JwtUtils
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

/** Real signing/parsing, using a test-only key. No application credentials required. */
class JwtSecurityTests {
    private val secret = "test-only-signing-key-".repeat(4)
    private val jwt = JwtUtils(JwtConfiguration(secret))
    private val user = UUID.randomUUID()
    private val session = UUID.randomUUID()

    @Test fun `access token preserves identity role and session for every role`() {
        Role.values().forEach { role ->
            assertEquals(Triple(user, role, session), jwt.getUserInfo(jwt.createAccessToken(user, role, session)))
        }
    }

    @Test fun `refresh token identifies session but cannot authenticate as an access token`() {
        val refresh = jwt.createRefreshToken(session)
        assertEquals(session, jwt.getSessionID(refresh))
        assertThrows(Unauthenticated::class.java) { jwt.getUserInfo(refresh) }
    }

    @Test fun `access and refresh tokens have finite distinct lifetimes`() {
        val before = Instant.now()
        val parser = Jwts.parserBuilder().setSigningKey(SecretKeySpec(secret.toByteArray(), "HmacSHA512")).build()
        val access = parser.parseClaimsJws(jwt.createAccessToken(user, Role.PATIENT, session)).body.expiration.toInstant()
        val refresh = parser.parseClaimsJws(jwt.createRefreshToken(session)).body.expiration.toInstant()
        assertTrue(access.isAfter(before.plusSeconds(3590)))
        assertTrue(access.isBefore(Instant.now().plusSeconds(3601)))
        assertTrue(refresh.isAfter(before.plusSeconds(86390)))
        assertTrue(refresh.isBefore(Instant.now().plusSeconds(86401)))
    }

    @Test fun `token signed by another key is rejected`() {
        val foreign = JwtUtils(JwtConfiguration("different-test-key-".repeat(4)))
        assertThrows(JwtException::class.java) { jwt.getUserInfo(foreign.createAccessToken(user, Role.PATIENT, session)) }
    }

    @Test fun `expired correctly signed token is rejected`() {
        val expired = Jwts.builder().claim("userID", user).claim("role", Role.PATIENT)
            .claim("sessionID", session).setExpiration(Date.from(Instant.now().minusSeconds(60)))
            .signWith(SecretKeySpec(secret.toByteArray(), "HmacSHA512")).compact()
        assertThrows(ExpiredJwtException::class.java) { jwt.getUserInfo(expired) }
    }
}
