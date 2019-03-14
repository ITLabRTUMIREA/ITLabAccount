package database.user


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
    val firstName: String,

    @Basic
    @Column(name = "lastName")
    val lastName: String,

    @Basic
    @Column(name = "middleName")
    val middleName: String,

    @OneToMany
    @JoinColumn(name = "refreshTokenId")
    val refreshTokens: Set<RefreshToken>,

    @OneToMany
    @JoinColumn(name = "userPropertyId")
    val userProperties: Set<UserProperty>
)