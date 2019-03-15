package database.user

import javax.persistence.*

@Entity
@Table(name = "\"RefreshToken\"")
data class RefreshToken(

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Basic
    @Column(name = "token")
    val token: String=""
)