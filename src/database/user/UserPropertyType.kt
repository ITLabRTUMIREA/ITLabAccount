package database.user

import javax.persistence.*

@Entity
@Table(name = "\"UserPropertyType\"")
data class UserPropertyType(

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Basic
    @Column(name = "name")
    val name: String="",

    @Basic
    @Column(name = "description")
    val description: String = ""
)