package database.user

import database.user.User
import javax.persistence.*

@Entity
@Table(name = "\"UserCredentials\"")
data class UserCredentials(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id:Int=0,

    @Basic
    @Column(name = "username",updatable = false)
    val username: String="",

    @Basic
    @Column(name = "password",insertable = true)
    val password: String="123"
)