package database.user

import org.hibernate.annotations.Cascade
import javax.persistence.*

@Entity
@Table(name = "\"UserProperty\"")
data class UserProperty(

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Basic
    @Column(name = "value", nullable = false)
    val value: String? = null,

    @OneToOne
    @JoinColumn(name = "userpropertytype_id", nullable = false)
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    val userPropertyType: UserPropertyType? = null,

    @OneToOne
    @JoinColumn(name = "userpropertystatus_id", nullable = true)
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    val userPropertyStatus: UserPropertyStatus? = null,

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    val user: User? = null

)