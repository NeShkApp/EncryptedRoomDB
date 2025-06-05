package com.mun.bonecci.encryptedroomdb.domain.use_case

import kotlin.random.Random
import com.mun.bonecci.encryptedroomdb.commons.Result
import com.mun.bonecci.encryptedroomdb.data.models.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

/**
 * Use case for generating a random user.
 */
class GenerateRandomUserUseCase {
    /**
     * Invokes the use case to generate a random user.
     *
     * @return A flow emitting the result of generating a random user.
     */
    operator fun invoke(): Flow<Result<User>> = flow {
        try {
            val userRandomId = Random.nextInt(0, 7)
            val user = fakeUserList.getOrNull(userRandomId) ?: User(
                name = "Generic",
                email = "generic@email",
                age = 1
            )
            emit(Result.Success(user))
        } catch (e: Exception) {
            emit(Result.Error(message = e.message ?: GENERIC_ERROR))
        }
    }.catch { cause ->
        emit(
            Result.Error(
                message = cause.localizedMessage ?: GENERIC_ERROR
            )
        )
    }

    companion object {
        const val GENERIC_ERROR = "GENERIC_ERROR"
    }
}

/**
 * A list of fake users for generating random users.
 */
private val fakeUserList: List<User> = mutableListOf<User>().apply {
    add(User(name = "A", email = "@gmail.com", age = 1))
    add(User(name = "B", email = "@outlook.com", age = 2))
    add(User(name = "C", email = "@hotmail.com", age = 3))
    add(User(name = "D", email = "@outlook.com", age = 4))
    add(User(name = "E", email = "@gmail.com", age = 5))
    add(User(name = "F", email = "@hotmail.com", age = 6))
    add(User(name = "G", email = "@gmail.com", age = 7))
}