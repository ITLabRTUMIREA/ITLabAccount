package database.user

import database.user.UserPropertyStatus
import database.user.UserPropertyType
import org.hibernate.annotations.Cascade
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
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
    val value: String?="",

    @ManyToOne
    @JoinColumn(name = "userPropertyTypeId")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
     val userPropertyType: UserPropertyType?=null,

    @ManyToOne
    @JoinColumn(name = "userPropertyStatusId")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
     val userPropertyStatus: UserPropertyStatus?=null
)