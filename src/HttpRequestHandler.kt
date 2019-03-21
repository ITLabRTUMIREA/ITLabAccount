@file:Suppress("IMPLICIT_CAST_TO_ANY")

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import io.ktor.features.ContentNegotiation
import io.ktor.application.Application
import io.ktor.gson.*
import io.ktor.application.install
import io.ktor.routing.*
import org.slf4j.LoggerFactory
import com.google.gson.JsonObject
import database.user.*
import io.ktor.application.*
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import java.io.InputStreamReader
import io.ktor.request.receiveStream
import kotlin.math.ln

@Suppress("requestHandler")
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = true) {

    val logger = LoggerFactory.getLogger("HttpRequestHandler")

    //val userSource: UserSource = UserSourceImpl()
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        }
    }

//    install(Authentication) {
//        jwt {
//            verifier(JwtConfig.verifier)
//            val config = Config().config!!
//
//            if(config.hasPath("ktor.jwt.realm")) {
//                realm = config.getString("ktor.jwt.realm")
//                logger.info("Realm loaded from config = $realm")
//            }else{
//                realm = "ru.rtuitlab.account"
//                logger.info("Realm not loaded from config, default realm = $realm")
//            }
//
////            validate {
////                it.payload.id.toInt().let (userSource::findUserById)
////            }
//        }
//    }

    //TODO: Refactor code and do more readable ?

    val hibernateUtil = HibernateUtil().setUpSession()
    install(Routing) {

        post("api/database/connect") {
            hibernateUtil.setUpSession()
        }

        //TODO: Change input json ?
        post("api/database/addRefreshToken") {
            val tmp: JsonObject? =
                Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
            if (tmp != null) {
                val param = HashMap<String, String>()
                tmp.entrySet().map { param[it.key] = it.value.asString }

                val token = token.Token()
                val tokenGen = token.getTokenGen()

                tokenGen.addParameters(param)

                //TODO: JUST FOR TESTING! ITs must be random
                tokenGen.createToken("123")

                val id = hibernateUtil.addRefreshToken(token.tokenStr)
                if (id != 0) {
                    logger.info("RefreshToken added to postgres database with id = $id")
                    call.response.status(HttpStatusCode.OK)
                } else {
                    logger.error("Can't add entity RefreshToken to postgres database")
                    call.response.status(HttpStatusCode.BadRequest)
                }
            } else {
                logger.error("Can't add entity RefreshToken to postgres database")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        get("api/database/refreshToken") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }

            if (id != null) {

                val refreshToken = when (id) {
                    0 -> hibernateUtil.getEntities(RefreshToken())
                    else -> hibernateUtil.getEntity(id, RefreshToken())
                }

                if (refreshToken != null) {
                    val jsonElement = JsonParser().parse(Gson().toJson(refreshToken)) as JsonElement
                    val jsonObject = JsonObject()
                    jsonObject.add("data", jsonElement)
                    logger.info("Entity RefreshToken with id = $id is gotten successfully")
                    call.respond(jsonObject)
                } else {
                    logger.error("Entity RefreshToken with id = $id is not gotten (NOT FOUND)")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        put("api/database/refreshToken") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                var refreshToken = hibernateUtil.getEntity(id, RefreshToken())
                if (refreshToken != null) {
                    val tmp: JsonObject? =
                        Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
                    val param = HashMap<String, String>()
                    if (tmp != null) {
                        tmp.entrySet().map { param[it.key] = it.value.asString }

                        val token = token.Token()
                        val tokenGen = token.getTokenGen()

                        tokenGen.addParameters(param)

                        //TODO: JUST FOR TESTING! ITs must be random
                        tokenGen.createToken("123")
                        refreshToken = refreshToken.copy(token = token.tokenStr)
                        if (hibernateUtil.updateEntity(refreshToken)) {
                            logger.info("Entity RefreshToken with id = $id is updated successfully")
                            call.response.status(HttpStatusCode.OK)
                        }
                    } else {
                        logger.info("Entity RefreshToken with id = $id is updated unsuccessfully")
                        call.response.status(HttpStatusCode.Conflict)
                    }
                } else {
                    logger.info("Entity RefreshToken with id = $id is not found")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        delete("api/database/refreshToken") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }

            if (id != null) {
                val resultOfDeleting = hibernateUtil.deleteEntities(id, RefreshToken())
                if (resultOfDeleting) {
                    logger.info("Entity RefreshToken with id = $id is deleted successfully")
                    call.response.status(HttpStatusCode.OK)
                } else {
                    logger.error("Entity RefreshToken with id = $id is not deleted")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        get("api/database/userPropertyStatus") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }

            if (id != null) {
                val userPropertyStatus = when (id) {
                    0 -> hibernateUtil.getEntities(UserPropertyStatus())
                    else -> hibernateUtil.getEntity(id, UserPropertyStatus())
                }
                if (userPropertyStatus != null) {
                    val jsonElement = JsonParser().parse(Gson().toJson(userPropertyStatus)) as JsonElement
                    val jsonObject = JsonObject()
                    jsonObject.add("data", jsonElement)
                    logger.info("Entity userPropertyStatus with id = $id is gotten successfully")
                    call.respond(jsonObject)
                } else {
                    logger.error("Entity userPropertyStatus with id = $id is not gotten")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        post("api/database/addUserPropertyStatus") {
            val tmp: JsonObject? =
                Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
            val value = tmp?.get("statusValue")
            if (value != null) {
                val result = JsonObject()
                val id = hibernateUtil.addUserPropertyStatus(value.asString)
                result.addProperty("id", id)
                logger.info("UserPropertyStatus added to postgres database with id = $id")
                call.respond(result)
            } else {
                logger.error("Can't add entity UserPropertyStatus to postgres database")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        put("api/database/userPropertyStatus") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                var propertyStatus = hibernateUtil.getEntity(id, UserPropertyStatus())
                if (propertyStatus != null) {
                    val tmp: JsonObject? =
                        Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
                    val value = tmp?.get("statusValue")
                    if (value != null) {
                        propertyStatus = propertyStatus.copy(value = value.asString)
                    }
                    if (hibernateUtil.updateEntity(propertyStatus)) {
                        logger.info("Entity userPropertyStatus with id = $id is updated successfully")
                        call.response.status(HttpStatusCode.OK)
                    } else {
                        logger.info("Entity userPropertyStatus with id = $id is updated unsuccessfully")
                        call.response.status(HttpStatusCode.Conflict)
                    }
                } else {
                    logger.info("Entity userPropertyStatus with id = $id is not found")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        delete("api/database/userPropertyStatus") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }
            if (id != null) {
                val resultOfDeleting = hibernateUtil.deleteEntities(id, UserPropertyStatus())
                if (resultOfDeleting) {
                    logger.info("Entity userPropertyStatus with id = $id is deleted successfully")
                    call.response.status(HttpStatusCode.OK)
                } else {
                    logger.error("Entity userPropertyStatus with id = $id is not deleted")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        get("/api/database/userPropertyType") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }

            if (id != null) {
                val userPropertyType = when (id) {
                    0 -> hibernateUtil.getEntities(UserPropertyType())
                    else -> hibernateUtil.getEntity(id, UserPropertyType())
                }
                if (userPropertyType != null) {
                    val jsonElement = JsonParser().parse(Gson().toJson(userPropertyType)) as JsonElement
                    val jsonObject = JsonObject()
                    jsonObject.add("data", jsonElement)
                    logger.info("Entity userPropertyType with id = $id is gotten successfully")
                    call.respond(jsonObject)
                } else {
                    logger.error("Entity userPropertyType with id = $id is not gotten")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        post("/api/database/addUserPropertyType") {
            val tmp: JsonObject? =
                Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
            val name = tmp?.get("name")
            val description = tmp?.get("description")
            if (name != null) {
                val result = JsonObject()
                val id = hibernateUtil.addUserPropertyType(name.asString, description?.asString)
                result.addProperty("id", id)
                logger.info("UserPropertyType added to postgres database with id = $id")
                call.respond(result)
            } else {
                logger.error("Can't add entity UserPropertyType to postgres database")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        put("api/database/userPropertyType") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                var propertyType = hibernateUtil.getEntity(id, UserPropertyType())
                if (propertyType != null) {
                    val tmp: JsonObject? =
                        Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
                    val name = tmp?.get("name")
                    val description = tmp?.get("description")
                    if (name != null) {
                        propertyType = propertyType.copy(name = name.asString)
                    }
                    if (description != null) {
                        propertyType = propertyType.copy(description = description.asString)
                    }
                    if (hibernateUtil.updateEntity(propertyType)) {
                        logger.info("Entity userPropertyType with id = $id is updated successfully")
                        call.response.status(HttpStatusCode.OK)
                    } else {
                        logger.info("Entity userPropertyType with id = $id is updated unsuccessfully")
                        call.response.status(HttpStatusCode.Conflict)
                    }
                } else {
                    logger.info("Entity userPropertyType with id = $id is not found")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        delete("api/database/userPropertyType") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }
            if (id != null) {
                val resultOfDeleting = hibernateUtil.deleteEntities(id, UserPropertyType())
                if (resultOfDeleting) {
                    logger.info("Entity UserPropertyType with id = $id is deleted successfully")
                    call.response.status(HttpStatusCode.OK)
                } else {
                    logger.error("Entity UserPropertyType with id = $id is not deleted")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        get("api/database/userProperty") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }
            if (id != null) {
                val userProperty = when (id) {
                    0 -> hibernateUtil.getEntities(UserProperty())
                    else -> hibernateUtil.getEntity(id, UserProperty())
                }

                if (userProperty != null) {
                    val jsonElement = JsonParser().parse(Gson().toJson(userProperty)) as JsonElement
                    val jsonObject = JsonObject()
                    jsonObject.add("data", jsonElement)
                    logger.info("Entity UserProperty with id = $id is gotten successfully")
                    call.respond(jsonObject)
                } else {
                    logger.error("Entity UserProperty with id = $id is not gotten")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        post("api/database/addFullUserProperty") {
            val tmp: JsonObject? =
                Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)

            val value = tmp?.get("value")
            val propertyType = Gson().fromJson(tmp?.get("UserPropertyType"), UserPropertyType()::class.java)
            val propertyStatus = Gson().fromJson(tmp?.get("UserPropertyStatus"), UserPropertyStatus()::class.java)
            if (value != null && propertyType != null && propertyStatus != null) {
                val id = hibernateUtil.addUserProperty(value.asString, propertyType, propertyStatus)
                val result = JsonObject()
                call.respond(result)
                logger.info("UserProperty added to postgres database with id = $id")
            } else {
                logger.error("Can't add entity UserProperty to postgres database")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        post("api/database/addUserProperty") {
            val tmp: JsonObject? =
                Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)

            val value = tmp?.get("value")
            val typeId = tmp?.get("typeId")
            val statusId = tmp?.get("statusId")

            if (value != null && typeId != null && statusId != null) {
                val userPropertyType = hibernateUtil.getEntity(typeId.asInt, UserPropertyType())
                val userPropertyStatus = hibernateUtil.getEntity(statusId.asInt, UserPropertyStatus())
                if (userPropertyType != null && userPropertyStatus != null) {
                    val result = JsonObject()

                    val id = hibernateUtil.addUserProperty(
                        value.asString, userPropertyType, userPropertyStatus
                    )

                    result.addProperty("id", id)
                    logger.info("UserProperty added to postgres database with id = $id")
                    call.respond(result)
                } else {
                    logger.error("id of userPropertyType or userPropertyStatus is invalid")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Can't add entity UserProperty to postgres database")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        put("api/database/userProperty") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                var userProperty = hibernateUtil.getEntity(id, UserProperty())
                if (userProperty != null) {
                    val tmp: JsonObject? =
                        Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
                    val value = tmp?.get("value")
                    val typeId = tmp?.get("typeId")
                    val statusId = tmp?.get("statusId")

                    if (value != null) {
                        userProperty = userProperty.copy(value = value.asString)
                    }

                    if (statusId != null) {
                        val propertyStatus = hibernateUtil.getEntity(statusId.asInt, UserPropertyStatus())
                        userProperty = userProperty.copy(userPropertyStatus = propertyStatus)
                    }

                    if (typeId != null) {
                        val propertyType = hibernateUtil.getEntity(typeId.asInt, UserPropertyType())
                        userProperty = userProperty.copy(userPropertyType = propertyType)
                    }

                    if (hibernateUtil.updateEntity(userProperty)) {
                        logger.info("Entity UserProperty with id = $id is updated successfully")
                        call.response.status(HttpStatusCode.OK)
                    } else {
                        logger.info("Entity UserProperty with id = $id is not updated")
                        call.response.status(HttpStatusCode.Conflict)
                    }

                } else {
                    logger.info("Entity UserProperty with id = $id is not found")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        delete("api/database/userProperty") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }
            if (id != null) {
                val resultOfDeleting = hibernateUtil.deleteEntities(id, UserProperty())
                if (resultOfDeleting) {
                    logger.info("Entity userPropertyStatus with id = $id is deleted successfully")
                    call.response.status(HttpStatusCode.OK)
                } else {
                    logger.error("Entity userPropertyStatus with id = $id is not deleted")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        get ("api/database/user") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }
            if (id != null) {
                val user = when (id) {
                    0 -> hibernateUtil.getEntities(User())
                    else -> hibernateUtil.getEntity(id, User())
                }

                if (user != null) {
                    val jsonElement = JsonParser().parse(Gson().toJson(user)) as JsonElement
                    val jsonObject = JsonObject()
                    jsonObject.add("data", jsonElement)
                    logger.info("Entity User with id = $id is gotten successfully")
                    call.respond(jsonObject)
                } else {
                    logger.error("Entity User with id = $id is not gotten")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        post("api/database/addUser") {
            val tmp: JsonObject? =
                Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
            val fName = tmp?.get("firstName")
            val lName = tmp?.get("lastName")
            val mName = tmp?.get("middleName")
            val refreshTokens = tmp?.get("refreshTokensId")?.asJsonArray
            val userPropertys = tmp?.get("userPropertysId")?.asJsonArray
            val userCredentials = tmp?.get("userCredentialsId")
            if (fName != null && lName != null && mName != null && refreshTokens != null && userPropertys != null && userCredentials != null) {
                val tokens = HashSet<RefreshToken>()
                refreshTokens.forEach {
                    val token = hibernateUtil.getEntity(
                        Gson().fromJson(it, JsonObject::class.java).get("id").asInt,
                        RefreshToken()
                    )
                    if (token != null) {
                        tokens.add(token)
                    }
                }
                val propertys = HashSet<UserProperty>()
                userPropertys.forEach {
                    val property = hibernateUtil.getEntity(
                        Gson().fromJson(it, JsonObject::class.java).get("id").asInt,
                        UserProperty()
                    )
                    if (property != null) {
                        propertys.add(property)
                    }
                }
                val credentials = hibernateUtil.getEntity(userCredentials.asInt, UserCredentials())
                if (tokens.size > 0 && propertys.size > 0 && credentials != null) {
                    val id = hibernateUtil.addUser(fName.asString, lName.asString, mName.asString, tokens, propertys,credentials)
                    val result = JsonObject()
                    result.addProperty("id", id)
                    logger.info("User added to postgres database with id = $id")
                    call.respond(result)
                } else {
                    logger.error("Can't add entity User to postgres database")
                    call.response.status(HttpStatusCode.BadRequest)
                }
            } else {
                logger.error("Can't add entity User to postgres database")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        put("api/database/user") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                var user = hibernateUtil.getEntity(id, User())
                if (user != null) {
                    val tmp: JsonObject? =
                        Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
                    val fName = tmp?.get("firstName")
                    val lName = tmp?.get("lastName")
                    val mName = tmp?.get("middleName")

                    if (fName != null) {
                        user = user.copy(firstName = fName.asString)
                    }

                    if (lName != null) {
                        user = user.copy(lastName = lName.asString)
                    }

                    if (mName != null) {
                        user = user.copy(middleName = mName.asString)
                    }

                    if (hibernateUtil.updateEntity(user)) {
                        logger.info("Entity User with id = $id is updated successfully")
                        call.response.status(HttpStatusCode.OK)
                    } else {
                        logger.info("Entity User with id = $id is not updated")
                        call.response.status(HttpStatusCode.Conflict)
                    }

                } else {
                    logger.info("Entity User with id = $id is not found")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        delete("api/database/user") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }
            if (id != null) {
                val resultOfDeleting = hibernateUtil.deleteEntities(id, User())
                if (resultOfDeleting) {
                    logger.info("Entity User with id = $id is deleted successfully")
                    call.response.status(HttpStatusCode.OK)
                } else {
                    logger.error("Entity User with id = $id is not deleted")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        get("api/database/userCredentials") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }
            if (id != null) {
                val user = when (id) {
                    0 -> hibernateUtil.getEntities(UserCredentials())
                    else -> hibernateUtil.getEntity(id, UserCredentials())
                }

                if (user != null) {
                    val jsonElement = JsonParser().parse(Gson().toJson(user)) as JsonElement
                    val jsonObject = JsonObject()
                    jsonObject.add("data", jsonElement)
                    logger.info("Entity userCredentials with id = $id is gotten successfully")
                    call.respond(jsonObject)
                } else {
                    logger.error("Entity userCredentials with id = $id is not gotten")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        post("api/database/addUserCredentials") {
            val tmp: JsonObject? =
                Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
            val username = tmp?.get("username")
            val password = tmp?.get("password")
            if (username != null && password != null) {
                val result = JsonObject()
                val id = hibernateUtil.addUserCredentials(username = username.asString, password = password.asString)
                result.addProperty("id", id)
                logger.info("UserPropertyType added to postgres database with id = $id")
                call.respond(result)
            } else {
                logger.error("Can't add entity UserPropertyType to postgres database")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        put("api/database/userCredentials") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                var userCredentials = hibernateUtil.getEntity(id, UserCredentials())
                if (userCredentials != null) {
                    val tmp: JsonObject? =
                        Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
                    val password = tmp?.get("password")
                    if (password != null) {
                        userCredentials = userCredentials.copy(password = password.asString)
                    }

                    if (hibernateUtil.updateEntity(userCredentials)) {
                        logger.info("Entity userCredentials with id = $id is updated successfully")
                        call.response.status(HttpStatusCode.OK)
                    } else {
                        logger.info("Entity userCredentials with id = $id is not updated")
                        call.response.status(HttpStatusCode.Conflict)
                    }

                } else {
                    logger.info("Entity UserProperty with id = $id is not found")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        delete("api/database/userCredentials") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }
            if (id != null) {
                val resultOfDeleting = hibernateUtil.deleteEntities(id, UserCredentials())
                if (resultOfDeleting) {
                    logger.info("Entity userCredentials with id = $id is deleted successfully")
                    call.response.status(HttpStatusCode.OK)
                } else {
                    logger.error("Entity userCredentials with id = $id is not deleted")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        post("api/registration") {
            val tmp: JsonObject? =
                Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
            val userCredentials = Gson().fromJson(tmp?.get("userCredentials"), JsonObject::class.java)
            val userInfo = Gson().fromJson(tmp?.get("user"), JsonObject::class.java)
            val userProperty = Gson().fromJson(tmp?.get("userProperty"), JsonObject::class.java)
            if (userCredentials != null && userInfo != null && userProperty != null) {
                val userC = UserCredentials(
                    username = userCredentials.get("username").asString,
                    password = userCredentials.get("password").asString
                )
                val userP = UserProperty(
                    value = userProperty.get("value").asString,
                    userPropertyStatus = hibernateUtil.getEntity(2, UserPropertyStatus()),
                    userPropertyType = hibernateUtil.getEntity(
                        userProperty.get("userPropertyTypeId").asInt,
                        UserPropertyType()
                    )

                )
                val userPropertys = HashSet<UserProperty>()
                //TODO: Generate token here?
                val userTokens = HashSet<RefreshToken>()
                userPropertys.add(userP)
                val id = hibernateUtil.addUser(
                    userInfo.get("firstName").asString,
                    userInfo.get("lastName").asString,
                    userInfo.get("middleName").asString,
                    userTokens,
                    userPropertys,
                    userC
                )
                val result = JsonObject()
                result.addProperty("id", id)
                logger.info("User added to postgres database with id = $id")
                call.respond(result)
            } else {
                logger.error("Can't add entity User to postgres database")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        post("api/authentication/login") {

            val hibernateUtil = HibernateUtil()
            hibernateUtil.setUpSession()
            //hibernateUtil.addRefreshToken()
            //logger.info("HERE")
            //            val response = JsonObject()
//            val credentials = call.receive< ru.rtuitlab.account.authorization.UserCredential>()
//
//            val user = userSource.findUserByCredentials(credentials)
//            val accessToken = JwtConfig.makeAccessToken(user)
//            val refreshToken = JwtConfig.makeRefreshToken(user)
//            response.addProperty("accessToken", accessToken)
//            response.addProperty("refreshToken", refreshToken)
//            response.addProperty("expiresIn", JWT.decode(accessToken).expiresAt.time)
//
//            call.respond(response)

        }



//        get("optional") {
//            val hibernateUtil = HibernateUtil()
//            hibernateUtil.setUpSession()
//            hibernateUtil.addRefreshToken()
//            //val user = call.user
//            //val response = if (user != null) "authenticated!" else "optional"
//            call.respond("OK")
//        }

        /**
         * All [Route]s in the authentication block are secured.
         */
//        authenticate {
//            route("secret") {
//                get {
//                    //val user = call.user!!
//                    //call.respond(user.countries)
//                }
//
//                put {
//                    println("PUT")
//                    TODO("All your secret routes can follow here")
//                }
//
//            }
//        }

//        /**
//         * Routes with optional authentication
//         */
//        authenticate(optional = true) {
//            get("optional") {
//                val hibernateUtil = HibernateUtil()
//                hibernateUtil.setUpSession()
//                hibernateUtil.addRefreshToken()
//                //val user = call.user
//                //val response = if (user != null) "authenticated!" else "optional"
//                call.respond("OK")
//            }
//        }
    }
}