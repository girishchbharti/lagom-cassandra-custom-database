package com.knoldus.student.customdb


import akka.Done
import com.datastax.driver.core._
import com.datastax.driver.core.querybuilder.{QueryBuilder => QB}
import com.knoldus.student.customdb.utils._
import com.knoldus.student.impl.Student

import scala.collection.JavaConverters._
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

trait CustomCassandaSession{

  val cluster = Cluster.builder()
      .addContactPoint("127.0.0.1")
      // .withCredentials("username", "password")
      .build()

  val session = cluster.connect("student_data")
}

/**
  * Created by girish on 30/8/17.
  */
class StudentDao extends CustomCassandaSession {

  private val table = "student"
  private val id = "id"
  private val name = "name"

  createTable

  def createTable: Future[Done] = {
    val query = s"create table if not exists $table ($id text primary key, $name text )"
    session.executeAsync(query).map(_ => Done)
  }

  def dropTable: Future[Done] = {
    val query = s"drop table if exists $table"
    session.executeAsync(query).map(_ => Done)
  }

  def insert(student: Student): Future[Done] = {
    val query = {
      QB.insertInto(table)
        .value(id, student.id)
        .value(name, student.name)
    }
    session.executeAsync(query).map(_ => Done)
  }

  def select(stdId: String): Future[Seq[Student]] = {
    val query = {
      QB.select(id, name)
        .from(table)
        .where(QB.eq(id, stdId))
    }

    for {
      resultSet <- session.executeAsync(query)
    } yield {
      resultSet
        .asScala
        .map(row => Student(row.getString(id), row.getString(name)))
        .toSeq
    }
  }

  def delete(idToDelete: Long): Future[Done] = {
    val query = {
      QB.delete().all()
        .from(table)
        .where(QB.eq(id, idToDelete))
    }
    session.executeAsync(query).map(_ => Done)
  }
}


object StudentDao extends StudentDao
