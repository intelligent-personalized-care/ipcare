package pt.ipc

import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.multipart.commons.CommonsMultipartResolver
import pt.ipc.storage.repositories.jdbi.configure
import java.io.File

@Configuration
class AppConfig {

    private val maxVideoSize: Long = 300_000_000
    private val maxInMemory: Int = 300_000_000

    @Bean
    fun multipartResolver(): CommonsMultipartResolver {
        val resolver = CommonsMultipartResolver()
        resolver.setDefaultEncoding("UTF-8")
        resolver.setResolveLazily(true)
        resolver.setMaxUploadSizePerFile(maxVideoSize)
        resolver.setMaxUploadSize(maxVideoSize)
        resolver.setMaxInMemorySize(maxInMemory)
        resolver.setUploadTempDir(FileSystemResource(File(System.getProperty("java.io.tmpdir"))))
        return resolver
    }
}

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@EnableScheduling
@EnableCaching
class ApiApplication {

    @Bean
    fun jdbi(): Jdbi {
        val dbName = System.getenv("DB_NAME")
        val dbUser = System.getenv("DB_USER")
        val dbPass = System.getenv("DB_PASS")
        val instanceConnectionName =
            System.getenv("INSTANCE_CONNECTION_NAME")

        val jdbcUrl =
            "jdbc:postgresql://google/$dbName" +
                    "?socketFactory=com.google.cloud.sql.postgres.SocketFactory" +
                    "&cloudSqlInstance=$instanceConnectionName"

        return Jdbi.create(
            jdbcUrl,
            dbUser,
            dbPass
        ).configure()
    }
}

fun main(args: Array<String>) {
    runApplication<ApiApplication>(*args)
}
