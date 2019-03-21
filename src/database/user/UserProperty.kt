package database.user

import com.sun.istack.NotNull
import org.hibernate.annotations.Cascade
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
    val value: String = "",

    @ManyToOne
    @NotNull
    @JoinColumn(name = "userPropertyTypeId")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    val userPropertyType: UserPropertyType? = null,

    @ManyToOne
    @NotNull
    @JoinColumn(name = "userPropertyStatusId")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    val userPropertyStatus: UserPropertyStatus? = null
)