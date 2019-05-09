package database.tables

import io.ktor.auth.Principal
import javax.persistence.*

@Entity
@Table(name = "\"User\"")
data class User (

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Basic
    @Column(name = "firstname", nullable = false)
    val firstName: String? = null,

    @Basic
    @Column(name = "lastname", nullable = false)
    val lastName: String? = null,

    @Basic
    @Column(name = "middlename", nullable = false)
    val middleName: String? = null

) : Principal