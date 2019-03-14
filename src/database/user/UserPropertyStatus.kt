package database.user

import javax.persistence.*

@Entity
@Table(name = "\"UserPropertyStatus\"")
data class UserPropertyStatus(
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Basic
    @Column(name = "value")
    val value: String = ""
)