package database.user

import database.user.User
import javax.persistence.*

@Entity
@Table(name = "\"UserCredentials\"")
data class UserCredentials(

    @Id
    @Column(name = "username")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val username: String,

    @Basic
    @Column(name = "password")
    val password: String,

    @OneToOne
    @JoinColumn(name = "userId")
    val user: User
)