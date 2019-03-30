package database.user

import com.sun.istack.NotNull
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
    @JoinColumn(name = "userpropertytype_id", nullable = true)
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    val userPropertyType: UserPropertyType? = null,

    @OneToOne
    @JoinColumn(name = "userpropertystatus_id", nullable = true)
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    val userPropertyStatus: UserPropertyStatus? = null,

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "user_id", nullable = false)
    val userId: User? = null

)