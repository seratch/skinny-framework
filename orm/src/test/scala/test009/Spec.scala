package test009

import java.util.concurrent.{Executors, TimeUnit}

import org.scalatest._
import scalikejdbc._
import scalikejdbc.scalatest.AutoRollback
import skinny.dbmigration.DBSeeds
import skinny.orm._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

trait Connection {
  Class.forName("org.h2.Driver")
  ConnectionPool.add('test009, "jdbc:h2:mem:test009;MODE=PostgreSQL", "sa", "sa")
}

trait CreateTables extends DBSeeds { self: Connection =>
  override val dbSeedsAutoSession = NamedAutoSession('test009)
  addSeedSQL(sql"""
    create table blog (
      id bigint auto_increment primary key not null,
      name varchar(100) not null
    )""")
  addSeedSQL(sql"""
   create table article (
     id bigint auto_increment primary key not null,
     blog_id bigint not null references blog(id),
     title varchar(1000) not null,
     body text not null
   )""")
  addSeedSQL(sql"""
   create table comment (
     id bigint auto_increment primary key not null,
     article_id bigint not null references article(id),
     poster_name varchar(100) not null,
     title varchar(100) not null,
     body text not null
   )""")
  runIfFailed(sql"select count(1) from blog")
}

/**
  * This test code aims to ensure explicitly closing IncludesQueryRepository works fine for memory efficiency in a short term.
  */
class Spec extends fixture.FunSpec with Matchers with Connection with CreateTables with AutoRollback {

  override def db(): DB = NamedDB('test009).toDB()

  case class Blog(id: Long, name: String, articles: Seq[Article] = Nil)

  case class Article(id: Long,
                     blogId: Long,
                     title: String,
                     body: String,
                     blog: Option[Blog] = None,
                     comments: Seq[Comment] = Nil)

  case class Comment(
      id: Long,
      articleId: Long,
      posterName: String,
      title: String,
      body: String
  )

  object Blog extends SkinnyCRUDMapper[Blog] {
    override val connectionPoolName                                  = 'test009
    override def defaultAlias                                        = createAlias("b")
    override def extract(rs: WrappedResultSet, rn: ResultName[Blog]) = autoConstruct(rs, rn, "articles")

    lazy val articlesRef = hasMany[Article](
      many = Article -> Article.defaultAlias,
      on = (b, a) => sqls.eq(b.id, a.blogId),
      merge = (b, as) => b.copy(articles = as)
    ).includes[Article](
      merge = { (blogs, articles) =>
        blogs.map { blog =>
          blog.copy(articles = articles.filter(_.blogId == blog.id))
        }
      }
    )
  }

  object Article extends SkinnyCRUDMapper[Article] {
    override val connectionPoolName                                     = 'test009
    override def defaultAlias                                           = createAlias("a")
    override def extract(rs: WrappedResultSet, rn: ResultName[Article]) = autoConstruct(rs, rn, "blog", "comments")

    lazy val blogRef = belongsTo[Blog](Blog, (a, b) => a.copy(blog = b))

    hasMany[Comment](
      many = Comment -> Comment.defaultAlias,
      on = (a, c) => sqls.eq(a.id, c.articleId),
      merge = (a, cs) => a.copy(comments = cs)
    ).includes[Comment](
        merge = { (articles, comments) =>
          articles.map { article =>
            article.copy(comments = comments.filter(_.articleId == article.id))
          }
        }
      )
      .byDefault
  }

  object Comment extends SkinnyCRUDMapper[Comment] {
    override val connectionPoolName                                     = 'test009
    override def defaultAlias                                           = createAlias("c")
    override def extract(rs: WrappedResultSet, rn: ResultName[Comment]) = autoConstruct(rs, rn)
  }

  describe("working with huge column in an entity resolved by #includes") {
    it("should consider memory efficiency") { implicit session =>
      val blogId = Blog.createWithAttributes('name -> "Apply in Tokyo")
      (1 to 9).foreach { _ =>
        Blog.createWithAttributes('name -> "Anonymous Blog")
      }

      val a = Article.column
      val articleId = Article.createWithNamedValues(
        a.title  -> s"Learning Scala: Day 1",
        a.body   -> "Hello World",
        a.blogId -> blogId
      )
      val c = Comment.column

      val largeBody: String = "a" * 500 * 1000
      largeBody.getBytes.length should equal(500 * 1000)

      Comment.createWithNamedValues(
        c.articleId  -> articleId,
        c.posterName -> "Anonymous",
        c.title      -> "Huge body",
        c.body       -> largeBody
      )
      (1 to 100).foreach { i =>
        Comment.createWithNamedValues(
          c.articleId  -> articleId,
          c.posterName -> "Anonymous",
          c.title      -> s"Hello x ${i}",
          c.body       -> "This post is very helpful for me. Thanks!"
        )
      }
      Comment.count() should equal(101)
      Article.findById(articleId).head.comments.size should equal(101)

      val articleId2 = Article.createWithNamedValues(
        a.title  -> s"Learning Scala: Day 2",
        a.body   -> "Learn how to use sbt",
        a.blogId -> blogId
      )
      (1 to 500).foreach { i =>
        Comment.createWithNamedValues(
          c.articleId  -> articleId2,
          c.posterName -> "Anonymous",
          c.title      -> s"Hello x ${i} for Day2!",
          c.body       -> "This post is very helpful for me. Thanks!"
        )
      }

      (3 to 500).foreach { day =>
        Article.createWithNamedValues(
          a.title  -> s"Learning Scala: Day ${day}",
          a.body   -> "Learn how to define class/trait/object",
          a.blogId -> blogId
        )
      }

      Runtime.getRuntime.gc()

      val beforeFreeMemory: Long = Runtime.getRuntime.freeMemory()

      var mostConsumed: Long = Long.MinValue
      val consumedSizeList   = new scala.collection.mutable.ListBuffer[Long]

      implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(20))

      val futures = (1 to 100).map { id =>
        Future {
          Blog.includes(Blog.articlesRef).findAll()
          Thread.sleep(100)

          //Runtime.getRuntime.gc()

          val afterFreeMemory: Long = Runtime.getRuntime.freeMemory()
          val consumedMemory: Long  = beforeFreeMemory - afterFreeMemory
          consumedSizeList.append(consumedMemory)
          if (mostConsumed < consumedMemory) {
            mostConsumed = consumedMemory
          }

//          println(
//            s"[Memory Usage]\tconsumed: ${consumedMemory} (${consumedMemory / 1000 / 1000}MB)" +
//              s"\tafter: ${afterFreeMemory}(${afterFreeMemory / 1000 / 1000}MB)" +
//              s"\tbefore: ${beforeFreeMemory}(${beforeFreeMemory / 1000 / 1000}MB)"
//          )
        }
      }
      Await.result(Future.sequence(futures), Duration.create(30, TimeUnit.SECONDS))

      val average: Long = consumedSizeList.sum / 100
      println(s"mostConsumed: ${mostConsumed}, average: ${average}")
      average should be <= 0L

      // ensuring the heap memory is eventually released
      Thread.sleep(500L)
      Runtime.getRuntime.gc()

      val afterMemory: Long = Runtime.getRuntime.freeMemory()
      val ratio             = (afterMemory.toDouble / beforeFreeMemory)
      ratio should be >= 0.9
    }
  }

}
