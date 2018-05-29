package org.stevenlowes.tools.strictorm.examples.a_mvp

import org.stevenlowes.tools.strictorm.dao.*
import org.stevenlowes.tools.strictorm.database.Transaction
import java.time.LocalDate

data class User(@StringLength(20) val name: String, val userInfo: UserInfo, override val id: Long = -1) : Dao {
    companion object : DaoCompanion<User>(User::class)
}

data class UserInfo(val birthDate: LocalDate, @StringLength(100) val email: String, override val id: Long = -1): Dao{
    companion object : DaoCompanion<UserInfo>(UserInfo::class)
}

fun main(args: Array<String>){
    Transaction.execute { conn ->
        conn.prepareStatement("DROP TABLE IF EXISTS user").execute()
        conn.prepareStatement("DROP TABLE IF EXISTS userinfo").execute()
    }

    DaoInitialiser.initialise(UserInfo::class, User::class) //TODO automatically reorder these
    DaoInitialiser.createTables()
    val userInfo = UserInfo(LocalDate.of(1995, 5, 12),"spam@stevenlowes.com").save()
    val user = User("Steven", userInfo).save()
}