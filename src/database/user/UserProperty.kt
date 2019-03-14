package database.user

import database.user.UserPropertyStatus
import database.user.UserPropertyType
import javax.persistence.*

@Entity
@Table(name = "\"UserProperty\"")
data class UserProperty(

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Basic
    @Column(name = "value")
    val value: String,

    @OneToOne
    @JoinColumn(name = "userPropertyTypeId")
    val userPropertyType: UserPropertyType,

    @OneToOne
    @JoinColumn(name = "userPropertyStatusId")
    val userPropertyStatus: UserPropertyStatus
)