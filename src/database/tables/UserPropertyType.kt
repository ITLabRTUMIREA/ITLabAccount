package database.tables

import javax.persistence.*

@Entity
@Table(name = "\"UserPropertyType\"")
data class UserPropertyType(

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Basic
    @Column(name = "name", nullable = false)
    val name: String? = null

)