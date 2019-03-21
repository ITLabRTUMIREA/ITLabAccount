package database.user


import com.sun.istack.NotNull
import javax.persistence.*

@Entity
@Table(name = "\"User\"")
data class User(

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Basic
    @Column(name = "firstName")
    val firstName: String?=null,

    @Basic
    @Column(name = "lastName")
    val lastName: String?=null,

    @Basic
    @Column(name = "middleName")
    val middleName: String?=null,

    @OneToMany(cascade = [CascadeType.ALL],fetch = FetchType.EAGER)
    @JoinColumn(name = "userid")
    @NotNull
    val refreshTokens: Set<RefreshToken>?=null,

    @OneToMany(cascade = [CascadeType.ALL],fetch = FetchType.EAGER)
    @JoinColumn(name = "userid")
    @NotNull
    val userPropertys: Set<UserProperty>?=null,

    @OneToOne(cascade = [CascadeType.ALL])
    @NotNull
    @JoinColumn(name = "credentialsId")
    val credentials: UserCredentials?=null
)