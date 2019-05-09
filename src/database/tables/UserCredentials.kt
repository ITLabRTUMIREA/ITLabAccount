package database.tables

import com.sun.istack.NotNull
import javax.persistence.*

@Entity
@Table(name = "\"UserCredentials\"")
data class UserCredentials(

    @Id
    @Column(name = "username", updatable = false, nullable = false)
    val username: String? = null,

    @Basic
    @NotNull
    @Column(name = "password", insertable = true, nullable = false)
    val password: String? = null,

    @OneToOne
    @NotNull
    @JoinColumn(name = "user_id", nullable = false)
    val user: User? = null
)