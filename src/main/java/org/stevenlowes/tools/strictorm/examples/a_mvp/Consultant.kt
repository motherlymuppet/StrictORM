package org.stevenlowes.tools.strictorm.examples.a_mvp

import org.stevenlowes.tools.strictorm.dao.*
import java.time.LocalDate

data class User(
        @StringLength(20) val name: String,
        val userInfo: UserInfo,
        val post: Post,
        override val id: Long = -1) : Dao {
    companion object : DaoCompanion<User>(User::class)
}

data class UserInfo(
        val birthDate: LocalDate,
        @StringLength(100) val email: String,
        override val id: Long = -1) : Dao {
    companion object : DaoCompanion<UserInfo>(UserInfo::class)
}

data class Post(
        @StringLength(1000) val text: String,
        val comment1: Comment,
        val comment2: Comment,
        override val id: Long = -1) : Dao {
    companion object : DaoCompanion<Post>(Post::class)
}

data class Comment(
        @StringLength(100) val text: String,
        override val id: Long = -1) : Dao {
    companion object : DaoCompanion<Comment>(Comment::class)
}

fun main(args: Array<String>) {
    /*
    Transaction.execute { conn ->
        conn.prepareStatement("DROP TABLE IF EXISTS user").execute()
        conn.prepareStatement("DROP TABLE IF EXISTS userinfo").execute()
        conn.prepareStatement("DROP TABLE IF EXISTS post").execute()
        conn.prepareStatement("DROP TABLE IF EXISTS comment").execute()
    }
    */

    DaoInitialiser.initialise(UserInfo::class, User::class, Post::class, Comment::class)
    DaoInitialiser.createTables()
    val comment1 = Comment("This is a comment 1").save()
    val comment2 = Comment("This is a comment 2").save()
    val post = Post("This is a post", comment1, comment2).save()
    val userInfo = UserInfo(LocalDate.of(1995, 5, 12), "spam@stevenlowes.com").save()
    val user = User("Steven", userInfo, post).save()
    val readUser = User.read(user.id)
    println(readUser)
    //Prints: User(name=Steven, userInfo=UserInfo(birthDate=1995-05-12, email=spam@stevenlowes.com, id=1), post=Post(text=This is a post, comment1=Comment(text=This is a comment 1, id=1), comment2=Comment(text=This is a comment 2, id=2), id=1), id=1)
}

//TODO need to support MTM
//TODO need to support cyclical foreign keys
//TODO support listing instead of just reading