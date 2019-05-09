package database.tables

import javax.persistence.*

@Entity
@Table(name = "\"UserPropertyStatus\"")
data class UserPropertyStatus(
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Basic
    @Column(name = "value", nullable = false)
    val value: String? = null
)