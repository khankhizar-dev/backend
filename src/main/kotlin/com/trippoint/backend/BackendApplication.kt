package com.trippoint.backend

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.time.ZoneId
import java.util.TimeZone
import javax.sql.DataSource

@SpringBootApplication
open class BackendApplication

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    runApplication<BackendApplication>(*args)
}

@Bean
fun testDatabase(dataSource: DataSource) = CommandLineRunner {
    dataSource.connection.use { connection ->
        println("Connected to: ${connection.metaData.url}")
        println("Database: ${connection.catalog}")
    }
}
