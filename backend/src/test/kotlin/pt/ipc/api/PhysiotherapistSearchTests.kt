package pt.ipc.api

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pt.ipc.storage.repositories.jdbi.configure
import pt.ipc.storage.repositories.jdbi.JdbiPhysiotherapistsRepository
import java.io.File
import java.util.UUID

class PhysiotherapistSearchTests {
    private lateinit var db: Jdbi
    private val patient = UUID.randomUUID()

    @BeforeEach fun prepare() {
        val url = System.getenv("IPC_TEST_DB_URL")
        Assumptions.assumeTrue(url != null, "Requires the isolated ipc_integration_test database")
        require(url!!.contains("/ipc_integration_test"))
        db = Jdbi.create(url).configure()
        db.useHandle<Exception> { h ->
            require(h.createQuery("select current_database()").mapTo(String::class.java).one() == "ipc_integration_test")
            h.execute("drop schema if exists dbo cascade")
            h.execute(File("postgresql/createTable.sql").readText())
            h.createUpdate("insert into dbo.\"user\" values (:id,'Patient','patient@search.test','test')").bind("id", patient).execute()
            h.createUpdate("insert into dbo.patient(id) values (:id)").bind("id", patient).execute()
        }
    }

    private fun add(name: String, state: String? = "valid"): UUID {
        val id = UUID.randomUUID()
        db.useHandle<Exception> { h ->
            h.createUpdate("insert into dbo.\"user\" values (:id,:name,:email,'test')")
                .bind("id", id).bind("name", name).bind("email", "$id@search.test").execute()
            h.createUpdate("insert into dbo.physiotherapist(id) values (:id)").bind("id", id).execute()
            if (state != null) h.createUpdate("insert into dbo.docs_authenticity values (:id,:state,CURRENT_DATE)")
                .bind("id", id).bind("state", state).execute()
        }
        return id
    }

    private fun search(name: String? = null, skip: Int = 0, limit: Int = 10) = db.withHandle<List<pt.ipc.domain.physiotherapist.PhysiotherapistAvailable>, Exception> {
        JdbiPhysiotherapistsRepository(it).searchPhysiotherapistsAvailable(name, skip, limit, patient)
    }

    @Test fun `empty database returns empty list with absent blank and named searches`() {
        listOf(null, "", "   ", "Ana").forEach { assertTrue(search(it).isEmpty()) }
    }

    @Test fun `only validated physiotherapists are returned and unrated accounts are supported`() {
        val valid = add("Ana Silva")
        add("Waiting", "waiting"); add("Invalid", "invalid"); add("No document", null)
        val result = search()
        assertEquals(listOf(valid), result.map { it.id })
        assertEquals(0, result.single().rating!!.nrOfReviews)
        assertFalse(result.single().requested)
        db.useHandle<Exception> { h ->
            assertEquals(valid, JdbiPhysiotherapistsRepository(h).getPhysiotherapist(valid)!!.id)
        }
    }

    @Test fun `search ignores case and surrounding spaces but treats wildcard characters literally`() {
        val ana = add("Ana Silva")
        assertEquals(listOf(ana), search("  aNA  ").map { it.id })
        listOf("Missing", "%", "_", "' OR 1=1 --").forEach { assertTrue(search(it).isEmpty()) }
    }

    @Test fun `pagination is stable and request and rating belong to each physiotherapist`() {
        val z = add("Zoe")
        val a = add("Ana")
        db.useHandle<Exception> { h ->
            h.createUpdate("insert into dbo.physiotherapist_requests values (:request,:id,:patient,null)")
                .bind("request", UUID.randomUUID()).bind("id", a).bind("patient", patient).execute()
            h.createUpdate("insert into dbo.physiotherapist_rating values (:id,:patient,4)")
                .bind("id", a).bind("patient", patient).execute()
        }
        val first = search(limit = 1).single()
        assertEquals(a, first.id); assertTrue(first.requested)
        assertEquals(4f, first.rating!!.averageStarts)
        assertEquals(1, first.rating!!.nrOfReviews)
        assertEquals(z, search(skip = 1, limit = 1).single().id)
        assertTrue(search(skip = 2).isEmpty())
    }
}
