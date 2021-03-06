package org.stevenlowes.tools.strictorm.examples.a_mvp

import org.stevenlowes.tools.strictorm.dao.*
import org.stevenlowes.tools.strictorm.dao.annotations.StringLength
import org.stevenlowes.tools.strictorm.database.Transaction
import java.time.LocalDate

data class User(
        @StringLength(20) val name: String,
        val userInfo: UserInfo,
        val post: Post,
        override val id: Int = -1) : Dao {
    companion object : DaoCompanion<User>(User::class)
}

data class UserInfo(
        val birthDate: LocalDate,
        @StringLength(100) val email: String,
        override val id: Int = -1) : Dao {
    companion object : DaoCompanion<UserInfo>(UserInfo::class)
}

data class Post(
        @StringLength(1000) val text: String,
        val comment1: Comment,
        val comment2: Comment?,
        override val id: Int = -1) : Dao {
    companion object : DaoCompanion<Post>(Post::class)
}

data class Comment(
        @StringLength(100) val text: String,
        override val id: Int = -1) : Dao {
    companion object : DaoCompanion<Comment>(Comment::class)
}

fun main(args: Array<String>) {
    Transaction.execute { conn ->
        conn.prepareStatement("DROP TABLE IF EXISTS user").execute()
        conn.prepareStatement("DROP TABLE IF EXISTS userinfo").execute()
        conn.prepareStatement("DROP TABLE IF EXISTS post").execute()
        conn.prepareStatement("DROP TABLE IF EXISTS comment").execute()
    }

    DaoInitialiser.initialise(UserInfo::class, User::class, Post::class, Comment::class)
    DaoInitialiser.createTables()
    val comment1 = Comment("This is a comment 1").save()
    val post = Post("This is a post", comment1, null).save()
    val userInfo = UserInfo(LocalDate.of(1995, 5, 12), "spam@stevenlowes.com").save()
    val user = User("Steven", userInfo, post).save()
    val readUser = User.read(user.id)
    println(readUser)
    val comments = Comment.list()
    println(comments)
}

//TODO documentation
//TODO Unit testing
//TODO dependency injection

//TODO Break dependency loops
//TODO force lazy computation
//TODO need to support MTM
//TODO test with multiple server backends
//TODO read from config file